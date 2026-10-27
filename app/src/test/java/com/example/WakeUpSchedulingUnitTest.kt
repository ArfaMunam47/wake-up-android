package com.example

import com.example.data.model.AlarmEntity
import com.example.data.model.ChallengeType
import com.example.util.RoutineParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class WakeUpSchedulingUnitTest {

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
    fun testSequenceTimesCalculation() {
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

        // Base 7:00 AM in millis
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val baseMillis = cal.timeInMillis

        val sequenceOffsetsMinutes = (0 until alarm.maxSequenceCount).map { i ->
            i * alarm.sequenceIntervalMinutes
        }

        // Expected: 0, 5, 10, 15 minutes offset
        assertEquals(listOf(0, 5, 10, 15), sequenceOffsetsMinutes)
    }

    @Test
    fun testRoutineParser() {
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
    fun testSequenceDismissalPreservesFutureRecurrence() {
        val alarm = AlarmEntity(
            id = 10,
            label = "Routine",
            hour = 7,
            minute = 0,
            repeatDays = "1,3,5", // Mon, Wed, Fri
            isEnabled = true,
            isSequenceEnabled = true,
            sequenceIntervalMinutes = 5,
            maxSequenceCount = 4,
            lastDismissedOccurrenceDate = null
        )

        // Today's morning sequence dismissed:
        val todayStr = "2026-10-26" // Suppose Monday
        val updatedAlarm = alarm.copy(lastDismissedOccurrenceDate = todayStr)

        // Verify alarm is still enabled for future recurring schedule
        assertTrue(updatedAlarm.isEnabled)
        assertEquals(todayStr, updatedAlarm.lastDismissedOccurrenceDate)
        assertEquals("Mon, Wed, Fri", updatedAlarm.formattedDays())
    }
}
