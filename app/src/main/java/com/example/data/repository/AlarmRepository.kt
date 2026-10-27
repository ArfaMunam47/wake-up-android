package com.example.data.repository

import android.content.Context
import com.example.data.database.AlarmDao
import com.example.data.database.WakeUpDatabase
import com.example.data.model.AlarmEntity
import com.example.scheduler.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val alarmScheduler: AlarmScheduler
) {
    val allAlarms: Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()

    suspend fun getAlarmById(id: Long): AlarmEntity? = withContext(Dispatchers.IO) {
        alarmDao.getAlarmById(id)
    }

    fun getAlarmByIdFlow(id: Long): Flow<AlarmEntity?> = alarmDao.getAlarmByIdFlow(id)

    suspend fun saveAlarm(alarm: AlarmEntity): Long = withContext(Dispatchers.IO) {
        val id = if (alarm.id == 0L) {
            alarmDao.insert(alarm)
        } else {
            alarmDao.update(alarm)
            alarm.id
        }
        val saved = alarmDao.getAlarmById(id)
        if (saved != null && saved.isEnabled) {
            alarmScheduler.scheduleAlarm(saved)
        } else if (saved != null && !saved.isEnabled) {
            alarmScheduler.cancelAlarm(saved.id)
        }
        id
    }

    suspend fun toggleAlarm(alarm: AlarmEntity, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        val updated = alarm.copy(isEnabled = isEnabled)
        alarmDao.update(updated)
        if (isEnabled) {
            alarmScheduler.scheduleAlarm(updated)
        } else {
            alarmScheduler.cancelAlarm(updated.id)
        }
    }

    suspend fun deleteAlarm(alarm: AlarmEntity) = withContext(Dispatchers.IO) {
        alarmScheduler.cancelAlarm(alarm.id)
        alarmDao.delete(alarm)
    }

    suspend fun deleteAlarmById(id: Long) = withContext(Dispatchers.IO) {
        alarmScheduler.cancelAlarm(id)
        alarmDao.deleteById(id)
    }

    /**
     * Marks the current morning's sequence as dismissed.
     * Cancels any remaining alarms for today's sequence,
     * updates the database with today's date so today is marked completed,
     * and schedules the NEXT upcoming day in the recurring routine.
     */
    suspend fun dismissSequenceForToday(
        alarmId: Long,
        currentSequenceIndex: Int,
        maxSequence: Int
    ) = withContext(Dispatchers.IO) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(System.currentTimeMillis())

        // 1. Cancel remaining sequence intents for today (from index + 1 to max)
        alarmScheduler.cancelRemainingSequenceForToday(alarmId, currentSequenceIndex + 1, maxSequence)

        // 2. Persist completion date for this alarm
        alarmDao.updateDismissedOccurrence(alarmId, todayStr)

        // 3. Fetch updated alarm and schedule its next recurring day
        val alarm = alarmDao.getAlarmById(alarmId)
        if (alarm != null && alarm.isEnabled) {
            alarmScheduler.scheduleAlarm(alarm)
        }
    }

    /**
     * Finds the next upcoming scheduled alarm from a list of alarms.
     */
    fun findNextScheduledAlarm(alarms: List<AlarmEntity>): Pair<AlarmEntity, Calendar>? {
        val enabledAlarms = alarms.filter { it.isEnabled }
        if (enabledAlarms.isEmpty()) return null

        var earliestTime: Long = Long.MAX_VALUE
        var nextPair: Pair<AlarmEntity, Calendar>? = null

        val now = System.currentTimeMillis()
        for (alarm in enabledAlarms) {
            val nextCal = alarmScheduler.calculateNextOccurrence(alarm, now) ?: continue
            val time = nextCal.timeInMillis
            if (time in (now + 1)..earliestTime) {
                earliestTime = time
                nextPair = Pair(alarm, nextCal)
            }
        }

        return nextPair
    }

    suspend fun rescheduleAllActiveAlarms() = withContext(Dispatchers.IO) {
        val enabled = alarmDao.getAllEnabledAlarms()
        for (alarm in enabled) {
            alarmScheduler.scheduleAlarm(alarm)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AlarmRepository? = null

        fun getRepository(context: Context): AlarmRepository {
            return INSTANCE ?: synchronized(this) {
                val db = WakeUpDatabase.getDatabase(context)
                val scheduler = AlarmScheduler(context)
                val repo = AlarmRepository(db.alarmDao(), scheduler)
                INSTANCE = repo
                repo
            }
        }
    }
}
