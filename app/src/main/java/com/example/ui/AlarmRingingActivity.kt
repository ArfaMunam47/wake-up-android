package com.example.ui

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.repository.AlarmRepository
import com.example.scheduler.AlarmScheduler
import com.example.service.AlarmRingManager
import com.example.ui.screens.AlarmRingingScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.WakeUpBackground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Turn screen on and show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            km?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val sequenceIndex = intent.getIntExtra(AlarmScheduler.EXTRA_SEQUENCE_INDEX, 0)
        val maxSequence = intent.getIntExtra(AlarmScheduler.EXTRA_MAX_SEQUENCE, 1)
        val occurrenceDate = intent.getStringExtra(AlarmScheduler.EXTRA_OCCURRENCE_DATE) ?: ""
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: "Wake Up"
        val challengeType = intent.getStringExtra(AlarmScheduler.EXTRA_CHALLENGE_TYPE) ?: "MATHS"

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = WakeUpBackground
                ) {
                    AlarmRingingScreen(
                        alarmId = alarmId,
                        sequenceIndex = sequenceIndex,
                        maxSequence = maxSequence,
                        occurrenceDate = occurrenceDate,
                        label = label,
                        challengeType = challengeType,
                        onDismissComplete = {
                            // Stop sound and vibration
                            AlarmRingManager.stopRinging()

                            // Cancel remaining sequence alarms for today and persist completion
                            if (alarmId > 0) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    val repo = AlarmRepository.getRepository(applicationContext)
                                    repo.dismissSequenceForToday(alarmId, sequenceIndex, maxSequence)
                                }
                            }

                            finish()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AlarmRingManager.stopRinging()
    }
}
