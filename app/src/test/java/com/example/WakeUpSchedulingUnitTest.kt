package com.example

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.WakeUpDatabase
import com.example.data.model.AlarmEntity
import com.example.data.model.ChallengeType
import com.example.data.repository.AlarmRepository
import com.example.scheduler.AlarmScheduler
import com.example.util.RoutineParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WakeUpSchedulingUnitTest {

    private lateinit var context: Context
    private lateinit var scheduler: AlarmScheduler
    private lateinit var repository: AlarmRepository
    private lateinit var shadowAlarmManager: org.robolectric.shadows.ShadowAlarmManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        scheduler = AlarmScheduler(context)
        val db = WakeUpDatabase.getDatabase(context)
        repository = AlarmRepository(db.alarmDao(), scheduler)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
    }

    @Test
    fun testAlarmModelFormatting() {
        val alarm = AlarmEntity(
            id = 1,
            label = "College",
            hour = 7,
            minute = 0,
            repeatDays = "1,3,5",
            isEnabled = true,
            isSequenceEnabled = true,
            sequenceIntervalMinutes = 5,
            maxSequenceCount = 4,
            challengeType = ChallengeType.MATHS.name
        )

        assertEquals("7:00 AM", alarm.formattedTime())
        assertEquals("Mon, Wed, Fri", alarm.formattedDays())
        assertTrue(alarm.hasDay(1))
        assertTrue(alarm.hasDay(3))
        assertTrue(alarm.hasDay(5))
        assertFalse(alarm.hasDay(2))
    }

    @Test
    fun testRequestCodeUniqueness() {
        // Ensure request codes never collide across different alarms or different sequence indices
        val codes = mutableSetOf<Int>()
        for (alarmId in 1L..50L) {
            for (seqIndex in 0..10) {
                val code = AlarmScheduler.getRequestCode(alarmId, seqIndex)
                assertFalse("Collision detected for alarm $alarmId seq $seqIndex: $code", codes.contains(code))
                codes.add(code)
            }
        }
    }

    @Test
    fun testSequenceCalculationOffsets() {
        val alarm = AlarmEntity(
            id = 2,
            label = "Morning",
            hour = 7,
            minute = 0,
            repeatDays = "1,2,3,4,5",
            isEnabled = true,
            isSequenceEnabled = true,
            sequenceIntervalMinutes = 5,
            maxSequenceCount = 4
        )

        val offsets = (0 until alarm.maxSequenceCount).map { i ->
            i * alarm.sequenceIntervalMinutes
        }
        assertEquals(listOf(0, 5, 10, 15), offsets)
    }

    @Test
    fun testNextOccurrenceSkipsDismissedToday() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(System.currentTimeMillis())
        val now = Calendar.getInstance()

        // Recurring alarm every day
        val alarm = AlarmEntity(
            id = 5,
            label = "Daily",
            hour = 8,
            minute = 30,
            repeatDays = "1,2,3,4,5,6,7",
            isEnabled = true,
            isSequenceEnabled = true,
            sequenceIntervalMinutes = 5,
            maxSequenceCount = 4,
            lastDismissedOccurrenceDate = todayStr // Today already dismissed
        )

        val nextOccurrence = scheduler.calculateNextOccurrence(alarm, now.timeInMillis)
        assertNotNull(nextOccurrence)

        val nextDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(nextOccurrence!!.time)
        // Must NOT be today!
        assertNotEquals(todayStr, nextDateStr)
    }

    @Test
    fun testRoutineParserDeterministic() {
        val input = "I have college on Monday, Wednesday, and Friday. I need to wake up at 7 AM. Repeat alarms every 5 minutes, with a maximum of four alarms."
        val parsed = RoutineParser.parse(input)

        assertEquals("College", parsed.label)
        assertEquals(7, parsed.hour)
        assertEquals(0, parsed.minute)
        assertEquals(listOf(1, 3, 5), parsed.days)
        assertEquals(5, parsed.intervalMinutes)
        assertEquals(4, parsed.maxSequenceCount)
    }

    @Test
    fun testRealAlarmSchedulingInAlarmManager() = runBlocking {
        val alarm = AlarmEntity(
            id = 42,
            label = "Robolectric Test Alarm",
            hour = 9,
            minute = 0,
            repeatDays = "1,2,3,4,5,6,7",
            isEnabled = true,
            isSequenceEnabled = true,
            sequenceIntervalMinutes = 5,
            maxSequenceCount = 3
        )

        scheduler.scheduleAlarm(alarm)

        // Robolectric shadowAlarmManager should contain the scheduled alarms
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue("Expected at least one real Android alarm scheduled in AlarmManager", scheduledAlarms.isNotEmpty())
    }

    @Test
    fun testCancellingAlarmClearsFromAlarmManager() = runBlocking {
        val alarm = AlarmEntity(
            id = 99,
            label = "Test Cancel",
            hour = 11,
            minute = 0,
            repeatDays = "",
            isEnabled = true,
            isSequenceEnabled = false
        )

        scheduler.scheduleAlarm(alarm)
        scheduler.cancelAlarm(alarm.id)

        // After cancelling, pending intent is cancelled
        val requestCode = AlarmScheduler.getRequestCode(alarm.id, 0)
        val intent = android.content.Intent(context, com.example.receiver.AlarmReceiver::class.java).apply {
            action = com.example.receiver.AlarmReceiver.ACTION_ALARM_TRIGGER
        }
        val checkPending = android.app.PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        // Cancelled pending intent returns null or is cancelled
        assertTrue(checkPending == null)
    }

    @Test
    fun testDismissRemainingSequenceCancelsOnlyToday() = runBlocking {
        val alarmId = 15L
        val maxSequence = 4

        // Simulate 4 sequence items scheduled
        for (i in 0 until maxSequence) {
            val req = AlarmScheduler.getRequestCode(alarmId, i)
            val intent = android.content.Intent(context, com.example.receiver.AlarmReceiver::class.java).apply {
                action = com.example.receiver.AlarmReceiver.ACTION_ALARM_TRIGGER
            }
            android.app.PendingIntent.getBroadcast(
                context,
                req,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Dismiss at step 1: cancels steps 2 and 3
        scheduler.cancelRemainingSequenceForToday(alarmId, fromIndex = 2, maxSequence = maxSequence)

        // Step 2 should be cancelled
        val step2Req = AlarmScheduler.getRequestCode(alarmId, 2)
        val intent = android.content.Intent(context, com.example.receiver.AlarmReceiver::class.java).apply {
            action = com.example.receiver.AlarmReceiver.ACTION_ALARM_TRIGGER
        }
        val step2Pending = android.app.PendingIntent.getBroadcast(
            context,
            step2Req,
            intent,
            android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        assertTrue(step2Pending == null)
    }
}
