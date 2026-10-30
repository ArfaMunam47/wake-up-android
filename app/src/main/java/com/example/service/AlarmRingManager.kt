package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.R
import com.example.data.model.AlarmSound

object AlarmRingManager {
    private const val TAG = "AlarmRingManager"
    private var mediaPlayer: MediaPlayer? = null
    private var fallbackRingtone: Ringtone? = null
    private var previewPlayer: MediaPlayer? = null
    private var previewSoundPlaying: AlarmSound? = null

    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isRinging = false

    /**
     * Starts active alarm ringing with the selected sound, loop playback,
     * AudioAttributes.USAGE_ALARM audio routing, vibration pattern, and partial WakeLock.
     */
    @Synchronized
    fun startRinging(context: Context, soundName: String? = null) {
        if (isRinging) return
        isRinging = true

        // Stop any sound preview that was playing
        stopPreview()

        // 1. Acquire partial wake lock with 5-minute safety timeout to prevent CPU sleep
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null) {
                wakeLock?.release()
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "WakeUp:AlarmRingingWakeLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire(5 * 60 * 1000L) // 5 minutes max
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }

        // 2. Play the selected alarm sound (Gentle, Classic, Urgent) via MediaPlayer
        val soundEnum = AlarmSound.fromName(soundName)
        val rawResId = getRawResId(context, soundEnum)

        try {
            if (rawResId != 0) {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    val afd = context.resources.openRawResourceFd(rawResId)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    isLooping = true
                    prepare()
                    start()
                }
                Log.d(TAG, "Playing alarm sound: ${soundEnum.displayName}")
            } else {
                startFallbackRingtone(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing custom alarm sound, falling back to system ringtone", e)
            startFallbackRingtone(context)
        }

        // 3. Trigger vibration pattern
        try {
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator = vib

            val pattern = longArrayOf(0, 500, 300, 500, 300, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vib?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start vibration", e)
        }
    }

    private fun startFallbackRingtone(context: Context) {
        try {
            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            fallbackRingtone = RingtoneManager.getRingtone(context.applicationContext, alarmUri)?.apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    isLooping = true
                }
                play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed fallback ringtone", e)
        }
    }

    /**
     * Stops alarm ringing, releases MediaPlayer, cancels vibration, and releases WakeLock.
     */
    @Synchronized
    fun stopRinging() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        }

        try {
            fallbackRingtone?.stop()
            fallbackRingtone = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping fallback ringtone", e)
        }

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vibration", e)
        }

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        }

        isRinging = false
    }

    /**
     * Preview sound in the Sound Selector screen.
     * Guaranteed single audio session: stops any other playing preview or restarts if tapped again.
     */
    @Synchronized
    fun togglePreview(context: Context, sound: AlarmSound, onPlaybackChanged: (Boolean) -> Unit) {
        if (previewSoundPlaying == sound) {
            stopPreview()
            onPlaybackChanged(false)
            return
        }

        stopPreview()

        val rawResId = getRawResId(context, sound)
        if (rawResId == 0) {
            onPlaybackChanged(false)
            return
        }

        try {
            previewPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                val afd = context.resources.openRawResourceFd(rawResId)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = false
                setOnCompletionListener {
                    previewSoundPlaying = null
                    onPlaybackChanged(false)
                }
                prepare()
                start()
            }
            previewSoundPlaying = sound
            onPlaybackChanged(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to preview sound", e)
            onPlaybackChanged(false)
        }
    }

    @Synchronized
    fun isPreviewing(sound: AlarmSound): Boolean {
        return previewSoundPlaying == sound && previewPlayer?.isPlaying == true
    }

    @Synchronized
    fun stopPreview() {
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
            previewPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping preview", e)
        }
        previewSoundPlaying = null
    }

    private fun getRawResId(context: Context, sound: AlarmSound): Int {
        return context.resources.getIdentifier(sound.rawResName, "raw", context.packageName)
    }
}
