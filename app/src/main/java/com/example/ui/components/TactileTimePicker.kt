package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.WakeUpButtonShadow
import com.example.ui.theme.WakeUpCardShadow
import com.example.ui.theme.WakeUpCrimson
import com.example.ui.theme.WakeUpCrimsonContainer
import com.example.ui.theme.WakeUpDivider
import com.example.ui.theme.WakeUpSurface
import com.example.ui.theme.WakeUpSurfaceSecondary
import com.example.ui.theme.WakeUpTextPrimary
import com.example.ui.theme.WakeUpTextSecondary
import java.util.Locale

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

    TactileCard(
        modifier = modifier.fillMaxWidth(),
        elevation = 3.dp,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ALARM TIME",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = WakeUpTextSecondary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Hour Column
                TimeUnitColumn(
                    value = displayHour,
                    onIncrement = {
                        val newDisplay = if (displayHour == 12) 1 else displayHour + 1
                        val new24 = if (isPm) {
                            if (newDisplay == 12) 12 else newDisplay + 12
                        } else {
                            if (newDisplay == 12) 0 else newDisplay
                        }
                        onTimeChanged(new24, minute)
                    },
                    onDecrement = {
                        val newDisplay = if (displayHour == 1) 12 else displayHour - 1
                        val new24 = if (isPm) {
                            if (newDisplay == 12) 12 else newDisplay + 12
                        } else {
                            if (newDisplay == 12) 0 else newDisplay
                        }
                        onTimeChanged(new24, minute)
                    }
                )

                Text(
                    text = ":",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Light,
                        color = WakeUpTextSecondary
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Minute Column
                TimeUnitColumn(
                    value = minute,
                    formatString = "%02d",
                    onIncrement = {
                        val newMin = (minute + 5) % 60
                        onTimeChanged(hour24, newMin)
                    },
                    onDecrement = {
                        val newMin = if (minute - 5 < 0) 55 else minute - 5
                        onTimeChanged(hour24, newMin)
                    }
                )

                Spacer(modifier = Modifier.width(16.dp))

                // AM / PM Selector
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AmPmTogglePill(
                        label = "AM",
                        isSelected = !isPm,
                        onClick = {
                            if (isPm) {
                                val new24 = if (hour24 == 12) 0 else hour24 - 12
                                onTimeChanged(new24, minute)
                            }
                        }
                    )
                    AmPmTogglePill(
                        label = "PM",
                        isSelected = isPm,
                        onClick = {
                            if (!isPm) {
                                val new24 = if (hour24 == 0) 12 else hour24 + 12
                                onTimeChanged(new24, minute)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick minute adjustment chips (+5m, +15m, +30m)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickTimeChip(text = "+5m") {
                    val newTotal = (minute + 5)
                    val newMin = newTotal % 60
                    val addHours = newTotal / 60
                    val newH = (hour24 + addHours) % 24
                    onTimeChanged(newH, newMin)
                }
                QuickTimeChip(text = "+15m") {
                    val newTotal = (minute + 15)
                    val newMin = newTotal % 60
                    val addHours = newTotal / 60
                    val newH = (hour24 + addHours) % 24
                    onTimeChanged(newH, newMin)
                }
                QuickTimeChip(text = ":00") {
                    onTimeChanged(hour24, 0)
                }
                QuickTimeChip(text = ":30") {
                    onTimeChanged(hour24, 30)
                }
            }
        }
    }
}

@Composable
private fun TimeUnitColumn(
    value: Int,
    formatString: String = "%d",
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = onIncrement,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Increment",
                tint = WakeUpTextSecondary
            )
        }

        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 76.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = WakeUpCardShadow
                )
                .clip(RoundedCornerShape(16.dp))
                .background(WakeUpSurfaceSecondary)
                .border(1.dp, WakeUpDivider, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format(Locale.getDefault(), formatString, value),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = WakeUpTextPrimary
                ),
                textAlign = TextAlign.Center
            )
        }

        IconButton(
            onClick = onDecrement,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Decrement",
                tint = WakeUpTextSecondary
            )
        }
    }
}

@Composable
private fun AmPmTogglePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) WakeUpCrimson else WakeUpSurfaceSecondary
    val textColor = if (isSelected) Color.White else WakeUpTextSecondary
    val borderColor = if (isSelected) WakeUpCrimson else WakeUpDivider

    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 34.dp)
            .shadow(
                elevation = if (isSelected) 2.dp else 1.dp,
                shape = RoundedCornerShape(10.dp),
                spotColor = if (isSelected) WakeUpButtonShadow else WakeUpCardShadow
            )
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(
                onClick = onClick,
                role = Role.RadioButton,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        )
    }
}

@Composable
private fun QuickTimeChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(WakeUpSurfaceSecondary)
            .border(1.dp, WakeUpDivider, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = WakeUpTextSecondary
            )
        )
    }
}
