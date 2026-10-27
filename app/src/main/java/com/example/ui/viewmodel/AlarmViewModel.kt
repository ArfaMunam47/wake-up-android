package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AlarmEntity
import com.example.data.model.ChallengeType
import com.example.data.repository.AlarmRepository
import com.example.scheduler.AlarmScheduler
import com.example.util.ParsedRoutine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AlarmRepository.getRepository(application)
    private val scheduler = AlarmScheduler(application)

    val alarms: StateFlow<List<AlarmEntity>> = repository.allAlarms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val nextAlarmPair: StateFlow<Pair<AlarmEntity, Calendar>?> = alarms
        .map { list -> repository.findNextScheduledAlarm(list) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun saveAlarm(alarm: AlarmEntity, onSaved: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveAlarm(alarm)
            onSaved?.invoke()
        }
    }

    fun toggleAlarm(alarm: AlarmEntity, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAlarm(alarm, isEnabled)
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
        }
    }

    fun saveParsedRoutine(parsed: ParsedRoutine) {
        viewModelScope.launch {
            val entity = AlarmEntity(
                label = parsed.label,
                hour = parsed.hour,
                minute = parsed.minute,
                repeatDays = parsed.days.sorted().joinToString(","),
                isEnabled = true,
                isSequenceEnabled = true,
                sequenceIntervalMinutes = parsed.intervalMinutes,
                maxSequenceCount = parsed.maxSequenceCount,
                challengeType = ChallengeType.MATHS.name
            )
            repository.saveAlarm(entity)
        }
    }

    fun scheduleTestAlarmInSeconds(seconds: Int = 5) {
        viewModelScope.launch {
            val testCal = Calendar.getInstance().apply {
                add(Calendar.SECOND, seconds)
            }
            val testAlarm = AlarmEntity(
                label = "Quick Verification Alarm",
                hour = testCal.get(Calendar.HOUR_OF_DAY),
                minute = testCal.get(Calendar.MINUTE),
                repeatDays = "", // One time
                isEnabled = true,
                isSequenceEnabled = true,
                sequenceIntervalMinutes = 1,
                maxSequenceCount = 2,
                challengeType = ChallengeType.MATHS.name
            )
            val savedId = repository.saveAlarm(testAlarm)
        }
    }
}
