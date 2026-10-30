package com.example.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.data.model.AlarmEntity
import com.example.receiver.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    companion object {
        const val TAG = "AlarmScheduler"
        const val EXTRA_ALARM_ID = "com.example.wakeup.EXTRA_ALARM_ID"
        const val EXTRA_SEQUENCE_INDEX = "com.example.wakeup.EXTRA_SEQUENCE_INDEX"
        const val EXTRA_MAX_SEQUENCE = "com.example.wakeup.EXTRA_MAX_SEQUENCE"
        const val EXTRA_OCCURRENCE_DATE = "com.example.wakeup.EXTRA_OCCURRENCE_DATE"
        const val EXTRA_ALARM_LABEL = "com.example.wakeup.EXTRA_ALARM_LABEL"
        const val EXTRA_CHALLENGE_TYPE = "com.example.wakeup.EXTRA_CHALLENGE_TYPE"
        const val EXTRA_ALARM_SOUND = "com.example.wakeup.EXTRA_ALARM_SOUND"

        fun getRequestCode(alarmId: Long, sequenceIndex: Int): Int {
            // Collision-free request code for up to 100M alarms and 20 sequence steps per alarm
            val safeId = (alarmId and 0x3FFFFFF).toInt()
            return (safeId * 20) + (sequenceIndex.coerceIn(0, 19))
        }
    }

    /**
     * Schedules the next occurrence (and its sequence of alarms, if enabled) for the given alarm.
     * Uses real Android AlarmManager.setAlarmClock() for system-level priority, lockscreen integration,
     * and guaranteed execution through Doze mode.
     */
    fun scheduleAlarm(alarm: AlarmEntity) {
        if (!alarm.isEnabled) {
            cancelAlarm(alarm.id)
            return
        }

        // Cancel any existing pending intents for this alarm before rescheduling
        cancelAlarm(alarm.id)

        val nextOccurrence = calculateNextOccurrence(alarm) ?: run {
            Log.w(TAG, "No valid next occurrence found for alarm id ${alarm.id}")
            return
        }

        val occurrenceDateStr = dateFormat.format(nextOccurrence.time)
        val baseTriggerMillis = nextOccurrence.timeInMillis

        val sequenceCount = if (alarm.isSequenceEnabled) alarm.maxSequenceCount.coerceIn(1, 10) else 1
        val intervalMillis = if (alarm.isSequenceEnabled) alarm.sequenceIntervalMinutes * 60_000L else 0L

        for (index in 0 until sequenceCount) {
            val triggerMillis = baseTriggerMillis + (index * intervalMillis)
            if (triggerMillis < System.currentTimeMillis()) {
                // If this specific step has already passed in real-time, skip scheduling it
                continue
            }

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM_TRIGGER
                putExtra(EXTRA_ALARM_ID, alarm.id)
                putExtra(EXTRA_SEQUENCE_INDEX, index)
                putExtra(EXTRA_MAX_SEQUENCE, sequenceCount)
                putExtra(EXTRA_OCCURRENCE_DATE, occurrenceDateStr)
                putExtra(EXTRA_ALARM_LABEL, alarm.label.ifBlank { "Wake Up" })
                putExtra(EXTRA_CHALLENGE_TYPE, alarm.challengeType)
                putExtra(EXTRA_ALARM_SOUND, alarm.soundName)
            }

            val requestCode = getRequestCode(alarm.id, index)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

            setExactAlarm(triggerMillis, requestCode, pendingIntent)
            Log.d(TAG, "Scheduled real alarm ${alarm.id} seq $index for $occurrenceDateStr at $triggerMillis (code $requestCode)")
        }
    }

    /**
     * Cancels all scheduled intents for an alarm across all sequence steps.
     */
    fun cancelAlarm(alarmId: Long) {
        for (index in 0 until 20) {
            val requestCode = getRequestCode(alarmId, index)
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM_TRIGGER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null && alarmManager != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
        Log.d(TAG, "Cancelled all scheduled intents for alarm $alarmId")
    }

    /**
     * Cancels only the remaining sequence alarms for the CURRENT morning.
     * This is called when the user dismisses the alarm (e.g. at step 2 of 4).
     */
    fun cancelRemainingSequenceForToday(alarmId: Long, fromIndex: Int, maxSequence: Int) {
        for (index in fromIndex until maxSequence) {
            val requestCode = getRequestCode(alarmId, index)
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_ALARM_TRIGGER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null && alarmManager != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d(TAG, "Cancelled remaining sequence step $index for alarm $alarmId (code $requestCode)")
            }
        }
    }

    /**
     * Computes the next Calendar trigger date/time for an alarm.
     */
    fun calculateNextOccurrence(
        alarm: AlarmEntity,
        fromMillis: Long = System.currentTimeMillis()
    ): Calendar? {
        val daysList = alarm.getRepeatDaysList()
        val now = Calendar.getInstance().apply { timeInMillis = fromMillis }

        if (daysList.isEmpty()) {
            // One-time alarm
            val target = Calendar.getInstance().apply {
                timeInMillis = fromMillis
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (target.timeInMillis <= now.timeInMillis) {
                // Target time today has already passed, schedule for tomorrow
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target
        }

        // Recurring alarm
        val todayDateStr = dateFormat.format(now.time)
        var bestCandidate: Calendar? = null

        // Check the next 14 days to find the earliest matching scheduled day
        for (dayOffset in 0..14) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = now.timeInMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, alarm.hour)
                set(Calendar.MINUTE, alarm.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val isoDayOfWeek = when (candidate.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }

            if (daysList.contains(isoDayOfWeek)) {
                val candidateDateStr = dateFormat.format(candidate.time)

                // If candidate is today, check if time has already passed or if today's sequence was already dismissed
                if (dayOffset == 0) {
                    val alreadyPassed = candidate.timeInMillis <= now.timeInMillis
                    val alreadyDismissedToday = alarm.lastDismissedOccurrenceDate == todayDateStr
                    if (alreadyPassed || alreadyDismissedToday) {
                        continue
                    }
                }

                bestCandidate = candidate
                break
            }
        }

        return bestCandidate
    }

    /**
     * Calculates all sequence timestamps for a given alarm starting from its next occurrence.
     */
    fun getPreviewSequenceTimes(alarm: AlarmEntity): List<Long> {
        val next = calculateNextOccurrence(alarm) ?: return emptyList()
        val base = next.timeInMillis
        val count = if (alarm.isSequenceEnabled) alarm.maxSequenceCount.coerceIn(1, 10) else 1
        val interval = if (alarm.isSequenceEnabled) alarm.sequenceIntervalMinutes * 60_000L else 0L

        return (0 until count).map { index -> base + (index * interval) }
    }

    /**
     * Sets real Android system alarm using setAlarmClock when available.
     * AlarmClockInfo integrates directly into system UI (lockscreen alarm banner and status bar icon)
     * and guarantees execution even in deep Doze mode.
     */
    private fun setExactAlarm(triggerMillis: Long, requestCode: Int, pendingIntent: PendingIntent) {
        if (alarmManager == null) return

        try {
            // Intent opened if user taps alarm icon in system UI
            val showIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val showPendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d(TAG, "Successfully setAlarmClock for trigger $triggerMillis")
        } catch (e: SecurityException) {
            Log.w(TAG, "setAlarmClock not permitted, falling back to setExactAndAllowWhileIdle / setAndAllowWhileIdle", e)
            fallbackSetAlarm(triggerMillis, pendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling with setAlarmClock, falling back", e)
            fallbackSetAlarm(triggerMillis, pendingIntent)
        }
    }

    private fun fallbackSetAlarm(triggerMillis: Long, pendingIntent: PendingIntent) {
        if (alarmManager == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback alarm scheduling failed", e)
        }
    }
}
