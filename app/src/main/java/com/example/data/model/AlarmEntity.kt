package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

enum class ChallengeType {
    STANDARD,
    MATHS,
    QR_CODE
}

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String = "",
    val hour: Int = 7,
    val minute: Int = 0,
    // Comma-separated list of day numbers: 1 = Mon, 2 = Tue, 3 = Wed, 4 = Thu, 5 = Fri, 6 = Sat, 7 = Sun
    // Empty string means non-recurring (once)
    val repeatDays: String = "1,3,5",
    val isEnabled: Boolean = true,
    val isSequenceEnabled: Boolean = true,
    val sequenceIntervalMinutes: Int = 5,
    val maxSequenceCount: Int = 4,
    val challengeType: String = ChallengeType.MATHS.name,
    // Stores YYYY-MM-DD of the sequence completed for that specific morning
    val lastDismissedOccurrenceDate: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getRepeatDaysList(): List<Int> {
        if (repeatDays.isBlank()) return emptyList()
        return repeatDays.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .sorted()
    }

    fun hasDay(day: Int): Boolean = getRepeatDaysList().contains(day)

    fun formattedTime(): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, amPm)
    }

    fun formattedDays(): String {
        val days = getRepeatDaysList()
        if (days.isEmpty()) return "Once"
        if (days.size == 7) return "Every day"
        if (days == listOf(1, 2, 3, 4, 5)) return "Weekdays"
        if (days == listOf(6, 7)) return "Weekends"

        val dayNames = mapOf(
            1 to "Mon",
            2 to "Tue",
            3 to "Wed",
            4 to "Thu",
            5 to "Fri",
            6 to "Sat",
            7 to "Sun"
        )
        return days.mapNotNull { dayNames[it] }.joinToString(", ")
    }

    fun getChallengeTypeEnum(): ChallengeType {
        return try {
            ChallengeType.valueOf(challengeType)
        } catch (_: Exception) {
            ChallengeType.STANDARD
        }
    }
}
