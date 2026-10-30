package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.WakeUpDivider
import com.example.ui.theme.WakeUpTextPrimary
import com.example.ui.theme.WakeUpTextTertiary
import java.util.Locale

/**
 * Wheel-style time picker matching the reference image's three-column layout:
 * Hour, Minute, AM/PM with previous and next values above and below the selected value.
 */
@Composable
fun TactileTimePicker(
    hour24: Int,
    minute: Int,
    onTimeChanged: (hour24: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isPm = hour24 >= 12
    val displayHour = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }

    val prevHour = if (displayHour == 1) 12 else displayHour - 1
    val nextHour = if (displayHour == 12) 1 else displayHour + 1

    val prevMinute = if (minute == 0) 59 else minute - 1
    val nextMinute = if (minute == 59) 0 else minute + 1

    val otherAmPm = if (isPm) "AM" else "PM"
    val currentAmPm = if (isPm) "PM" else "AM"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Center row highlight lines (very subtle)
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(42.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(WakeUpDivider.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.height(48.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(WakeUpDivider.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.height(42.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hour Column
            ScrollableTimeColumn(
                prevValue = prevHour.toString(),
                currentValue = displayHour.toString(),
                nextValue = nextHour.toString(),
                onPrevious = {
                    val newH = if (displayHour == 1) 12 else displayHour - 1
                    val new24 = if (isPm) {
                        if (newH == 12) 12 else newH + 12
                    } else {
                        if (newH == 12) 0 else newH
                    }
                    onTimeChanged(new24, minute)
                },
                onNext = {
                    val newH = if (displayHour == 12) 1 else displayHour + 1
                    val new24 = if (isPm) {
                        if (newH == 12) 12 else newH + 12
                    } else {
                        if (newH == 12) 0 else newH
                    }
                    onTimeChanged(new24, minute)
                }
            )

            // Minute Column
            ScrollableTimeColumn(
                prevValue = String.format(Locale.getDefault(), "%02d", prevMinute),
                currentValue = String.format(Locale.getDefault(), "%02d", minute),
                nextValue = String.format(Locale.getDefault(), "%02d", nextMinute),
                onPrevious = {
                    val newM = if (minute == 0) 59 else minute - 1
                    onTimeChanged(hour24, newM)
                },
                onNext = {
                    val newM = if (minute == 59) 0 else minute + 1
                    onTimeChanged(hour24, newM)
                }
            )

            // AM / PM Column
            ScrollableTimeColumn(
                prevValue = otherAmPm,
                currentValue = currentAmPm,
                nextValue = otherAmPm,
                onPrevious = {
                    val new24 = if (isPm) hour24 - 12 else hour24 + 12
                    onTimeChanged(new24, minute)
                },
                onNext = {
                    val new24 = if (isPm) hour24 - 12 else hour24 + 12
                    onTimeChanged(new24, minute)
                }
            )
        }
    }
}

@Composable
private fun ScrollableTimeColumn(
    prevValue: String,
    currentValue: String,
    nextValue: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .width(76.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount
                        if (dragAccumulator > 30f) {
                            onPrevious()
                            dragAccumulator = 0f
                        } else if (dragAccumulator < -30f) {
                            onNext()
                            dragAccumulator = 0f
                        }
                    },
                    onDragEnd = { dragAccumulator = 0f },
                    onDragCancel = { dragAccumulator = 0f }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Previous value (top row, dimmed)
        Text(
            text = prevValue,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                color = WakeUpTextTertiary,
                fontWeight = FontWeight.Normal
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPrevious
                )
                .padding(vertical = 10.dp)
        )

        // Selected value (center row, prominent bold black)
        Text(
            text = currentValue,
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 28.sp,
                color = WakeUpTextPrimary,
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        )

        // Next value (bottom row, dimmed)
        Text(
            text = nextValue,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                color = WakeUpTextTertiary,
                fontWeight = FontWeight.Normal
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onNext
                )
                .padding(vertical = 10.dp)
        )
    }
}
