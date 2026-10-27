package com.example.util

import java.util.Locale
import java.util.regex.Pattern

data class ParsedRoutine(
    val label: String,
    val hour: Int,
    val minute: Int,
    val days: List<Int>, // 1 = Mon .. 7 = Sun
    val intervalMinutes: Int,
    val maxSequenceCount: Int,
    val isConfident: Boolean
) {
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
        if (days.isEmpty()) return "Once"
        if (days.size == 7) return "Every day"
        if (days == listOf(1, 2, 3, 4, 5)) return "Weekdays"
        if (days == listOf(6, 7)) return "Weekends"
        val names = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
        return days.mapNotNull { names[it] }.joinToString(", ")
    }
}

object RoutineParser {

    fun parse(text: String): ParsedRoutine {
        val lower = text.lowercase(Locale.ROOT)

        // 1. Parse Days
        val days = mutableSetOf<Int>()
        if (lower.contains("everyday") || lower.contains("every day") || lower.contains("daily")) {
            days.addAll(1..7)
        } else if (lower.contains("weekday") || lower.contains("weekdays")) {
            days.addAll(1..5)
        } else if (lower.contains("weekend") || lower.contains("weekends")) {
            days.addAll(listOf(6, 7))
        }

        if (lower.contains("monday") || lower.contains(" mon ") || lower.startsWith("mon ") || lower.contains("mon,")) days.add(1)
        if (lower.contains("tuesday") || lower.contains(" tue ") || lower.startsWith("tue ") || lower.contains("tue,")) days.add(2)
        if (lower.contains("wednesday") || lower.contains(" wed ") || lower.startsWith("wed ") || lower.contains("wed,")) days.add(3)
        if (lower.contains("thursday") || lower.contains(" thu ") || lower.startsWith("thu ") || lower.contains("thu,")) days.add(4)
        if (lower.contains("friday") || lower.contains(" fri ") || lower.startsWith("fri ") || lower.contains("fri,")) days.add(5)
        if (lower.contains("saturday") || lower.contains(" sat ") || lower.startsWith("sat ") || lower.contains("sat,")) days.add(6)
        if (lower.contains("sunday") || lower.contains(" sun ") || lower.startsWith("sun ") || lower.contains("sun,")) days.add(7)

        val finalDays = if (days.isEmpty()) listOf(1, 2, 3, 4, 5) else days.sorted()

        // 2. Parse Time
        var hour = 7
        var minute = 0
        var foundTime = false

        // Match "7:30 AM", "7:30am", "7 AM", "07:00", "7am"
        val timeRegex = Pattern.compile("(\\b\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b", Pattern.CASE_INSENSITIVE)
        val matcher = timeRegex.matcher(lower)
        while (matcher.find()) {
            val hStr = matcher.group(1) ?: continue
            val mStr = matcher.group(2)
            val amPm = matcher.group(3)

            val rawH = hStr.toIntOrNull() ?: continue
            val rawM = mStr?.toIntOrNull() ?: 0

            if (amPm != null || mStr != null || rawH in 1..12) {
                if (amPm != null) {
                    hour = when {
                        amPm.equals("pm", ignoreCase = true) && rawH < 12 -> rawH + 12
                        amPm.equals("am", ignoreCase = true) && rawH == 12 -> 0
                        else -> rawH
                    }
                } else if (rawH in 0..23) {
                    hour = rawH
                }
                minute = rawM.coerceIn(0, 59)
                foundTime = true
                break
            }
        }

        // 3. Parse Interval
        var interval = 5
        val intervalRegex = Pattern.compile("(\\d+)\\s*(?:min|minute|minutes|m)\\b")
        val intervalMatcher = intervalRegex.matcher(lower)
        if (intervalMatcher.find()) {
            val num = intervalMatcher.group(1)?.toIntOrNull()
            if (num != null && num in 1..60) {
                interval = num
            }
        }

        // 4. Parse Max Sequence Count
        var maxCount = 4
        val wordToNum = mapOf("two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6)
        for ((word, num) in wordToNum) {
            if (lower.contains("$word alarm") || lower.contains("maximum of $word")) {
                maxCount = num
                break
            }
        }
        val countRegex = Pattern.compile("(\\d+)\\s*(?:alarm|alarms|times|repeats)\\b")
        val countMatcher = countRegex.matcher(lower)
        if (countMatcher.find()) {
            val num = countMatcher.group(1)?.toIntOrNull()
            if (num != null && num in 1..10) {
                maxCount = num
            }
        }

        // 5. Parse Label
        val knownLabels = listOf("college", "university", "school", "work", "gym", "study", "morning routine", "class")
        var label = "Morning Routine"
        for (kw in knownLabels) {
            if (lower.contains(kw)) {
                label = kw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                break
            }
        }

        return ParsedRoutine(
            label = label,
            hour = hour,
            minute = minute,
            days = finalDays,
            intervalMinutes = interval,
            maxSequenceCount = maxCount,
            isConfident = foundTime
        )
    }
}
