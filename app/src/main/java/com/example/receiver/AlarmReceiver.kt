package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.repository.AlarmRepository
import com.example.scheduler.AlarmScheduler
import com.example.service.AlarmRingManager
import com.example.ui.AlarmRingingActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "AlarmReceiver"
        const val ACTION_ALARM_TRIGGER = "com.example.wakeup.ACTION_ALARM_TRIGGER"
        const val ACTION_DISMISS_NOTIFICATION = "com.example.wakeup.ACTION_DISMISS_NOTIFICATION"
        const val CHANNEL_ID = "wake_up_alarm_channel"
        const val CHANNEL_NAME = "Wake Up Alarms"

        fun getNotificationId(alarmId: Long): Int {
            return (1000 + (alarmId and 0xFFFF)).toInt()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action == ACTION_DISMISS_NOTIFICATION) {
            val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
            val sequenceIndex = intent.getIntExtra(AlarmScheduler.EXTRA_SEQUENCE_INDEX, 0)
            val maxSequence = intent.getIntExtra(AlarmScheduler.EXTRA_MAX_SEQUENCE, 1)

            AlarmRingManager.stopRinging()
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (alarmId > 0) {
                nm?.cancel(getNotificationId(alarmId))
            }

            if (alarmId > 0) {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repository = AlarmRepository.getRepository(context)
                        repository.dismissSequenceForToday(alarmId, sequenceIndex, maxSequence)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error dismissing from notification", e)
                    } finally {
                        pending.finish()
                    }
                }
            }
            return
        }

        if (action != ACTION_ALARM_TRIGGER) return

        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val sequenceIndex = intent.getIntExtra(AlarmScheduler.EXTRA_SEQUENCE_INDEX, 0)
        val maxSequence = intent.getIntExtra(AlarmScheduler.EXTRA_MAX_SEQUENCE, 1)
        val occurrenceDate = intent.getStringExtra(AlarmScheduler.EXTRA_OCCURRENCE_DATE) ?: ""
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: "Wake Up"
        val challengeType = intent.getStringExtra(AlarmScheduler.EXTRA_CHALLENGE_TYPE) ?: "STANDARD"
        val soundName = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_SOUND) ?: "GENTLE"

        Log.d(TAG, "Real Android alarm triggered! id: $alarmId, seq: $sequenceIndex/$maxSequence, date: $occurrenceDate, sound: $soundName")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = AlarmRepository.getRepository(context)
                val alarm = repository.getAlarmById(alarmId)

                if (alarm == null || !alarm.isEnabled) {
                    Log.d(TAG, "Alarm $alarmId is disabled or deleted, suppressing trigger")
                    pendingResult.finish()
                    return@launch
                }

                // Check if this occurrence was already dismissed (e.g. earlier sequence was dismissed)
                if (alarm.lastDismissedOccurrenceDate != null &&
                    alarm.lastDismissedOccurrenceDate == occurrenceDate
                ) {
                    Log.d(TAG, "Sequence for $occurrenceDate was already dismissed. Suppressing step $sequenceIndex")
                    pendingResult.finish()
                    return@launch
                }

                val activeSound = alarm.soundName.ifBlank { soundName }

                // Start ringing audio with selected sound and vibration (held by wake lock)
                AlarmRingManager.startRinging(context, activeSound)

                // Show heads-up notification with FullScreenIntent
                showAlarmNotification(
                    context = context,
                    alarmId = alarmId,
                    label = label,
                    sequenceIndex = sequenceIndex,
                    maxSequence = maxSequence,
                    occurrenceDate = occurrenceDate,
                    challengeType = challengeType,
                    soundName = activeSound
                )

                // Directly launch the full-screen ringing activity
                val activityIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                    putExtra(AlarmScheduler.EXTRA_SEQUENCE_INDEX, sequenceIndex)
                    putExtra(AlarmScheduler.EXTRA_MAX_SEQUENCE, maxSequence)
                    putExtra(AlarmScheduler.EXTRA_OCCURRENCE_DATE, occurrenceDate)
                    putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, label)
                    putExtra(AlarmScheduler.EXTRA_CHALLENGE_TYPE, challengeType)
                    putExtra(AlarmScheduler.EXTRA_ALARM_SOUND, activeSound)
                }
                context.startActivity(activityIntent)

                // If this is the final sequence alarm for today and it's recurring,
                // proactively schedule the next day's recurrence so future mornings are not missed!
                if (sequenceIndex >= maxSequence - 1 && alarm.getRepeatDaysList().isNotEmpty()) {
                    repository.scheduleNextRecurringOccurrence(alarmId, occurrenceDate)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error handling alarm trigger", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showAlarmNotification(
        context: Context,
        alarmId: Long,
        label: String,
        sequenceIndex: Int,
        maxSequence: Int,
        occurrenceDate: String,
        challengeType: String,
        soundName: String
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel with ALARM usage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundEnum = com.example.data.model.AlarmSound.fromName(soundName)
            val resId = context.resources.getIdentifier(soundEnum.rawResName, "raw", context.packageName)
            val alarmSound: Uri = if (resId != 0) {
                Uri.parse("android.resource://" + context.packageName + "/" + resId)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarms and wake-up challenges"
                setSound(alarmSound, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 300, 500)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val fullScreenIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_SEQUENCE_INDEX, sequenceIndex)
            putExtra(AlarmScheduler.EXTRA_MAX_SEQUENCE, maxSequence)
            putExtra(AlarmScheduler.EXTRA_OCCURRENCE_DATE, occurrenceDate)
            putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, label)
            putExtra(AlarmScheduler.EXTRA_CHALLENGE_TYPE, challengeType)
            putExtra(AlarmScheduler.EXTRA_ALARM_SOUND, soundName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (alarmId * 1000 + sequenceIndex).toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action intent to dismiss directly if emergency
        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_NOTIFICATION
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_SEQUENCE_INDEX, sequenceIndex)
            putExtra(AlarmScheduler.EXTRA_MAX_SEQUENCE, maxSequence)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId * 1000 + 99).toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val seqText = if (maxSequence > 1) {
            "Alarm ${sequenceIndex + 1} of $maxSequence • Tap to solve challenge"
        } else {
            "Tap to solve wake-up challenge"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(label.ifBlank { "Wake Up" })
            .setContentText(seqText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .addAction(0, "Open Challenge", pendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(getNotificationId(alarmId), notification)
    }
}
