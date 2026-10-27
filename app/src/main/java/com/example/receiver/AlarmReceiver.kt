package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
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
        const val CHANNEL_ID = "wake_up_alarm_channel"
        const val CHANNEL_NAME = "Wake Up Alarms"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALARM_TRIGGER) return

        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val sequenceIndex = intent.getIntExtra(AlarmScheduler.EXTRA_SEQUENCE_INDEX, 0)
        val maxSequence = intent.getIntExtra(AlarmScheduler.EXTRA_MAX_SEQUENCE, 1)
        val occurrenceDate = intent.getStringExtra(AlarmScheduler.EXTRA_OCCURRENCE_DATE) ?: ""
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: "Wake Up"
        val challengeType = intent.getStringExtra(AlarmScheduler.EXTRA_CHALLENGE_TYPE) ?: "STANDARD"

        Log.d(TAG, "Alarm triggered! id: $alarmId, seq: $sequenceIndex/$maxSequence, date: $occurrenceDate")

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
                    Log.d(TAG, "Sequence for $occurrenceDate was already dismissed. Skipping step $sequenceIndex")
                    pendingResult.finish()
                    return@launch
                }

                // Start ringing audio and vibration
                AlarmRingManager.startRinging(context)

                // Show notification with full-screen intent
                showAlarmNotification(
                    context = context,
                    alarmId = alarmId,
                    label = label,
                    sequenceIndex = sequenceIndex,
                    maxSequence = maxSequence,
                    occurrenceDate = occurrenceDate,
                    challengeType = challengeType
                )

                // Also directly launch the full screen ringing activity
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
                }
                context.startActivity(activityIntent)

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
        challengeType: String
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel with ALARM usage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
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
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (alarmId * 1000 + sequenceIndex).toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val seqText = if (maxSequence > 1) {
            "Alarm ${sequenceIndex + 1} of $maxSequence • Tap to dismiss"
        } else {
            "Tap to solve wake-up challenge"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(label.ifBlank { "Wake Up" })
            .setContentText(seqText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify((1000 + alarmId).toInt(), notification)
    }
}
