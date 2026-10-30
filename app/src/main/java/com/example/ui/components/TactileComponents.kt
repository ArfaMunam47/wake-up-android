package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.WakeUpCardShadow
import com.example.ui.theme.WakeUpCrimson
import com.example.ui.theme.WakeUpCrimsonGlow
import com.example.ui.theme.WakeUpCrimsonPressed
import com.example.ui.theme.WakeUpDisabled
import com.example.ui.theme.WakeUpDivider
import com.example.ui.theme.WakeUpSurface
import com.example.ui.theme.WakeUpSurfaceSecondary
import com.example.ui.theme.WakeUpTextPrimary
import com.example.ui.theme.WakeUpTextSecondary

/**
 * Tactile physical switch matching the reference image:
 * A soft grey pill track with a sliding knob that turns crimson red when active.
 */
@Composable
fun TactileSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "alarm_switch"
) {
    val trackWidth = 46.dp
    val trackHeight = 26.dp
    val knobSize = 20.dp
    val padding = 3.dp

    val maxOffset = trackWidth - knobSize - (padding * 2)
    val knobOffset by animateDpAsState(
        targetValue = if (checked) maxOffset else 0.dp,
        animationSpec = tween(durationMillis = 180),
        label = "switch_knob_offset"
    )

    val knobColor by animateColorAsState(
        targetValue = if (checked) WakeUpCrimson else Color(0xFF8E8E93),
        animationSpec = tween(durationMillis = 180),
        label = "switch_knob_color"
    )

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(RoundedCornerShape(13.dp))
            .background(Color(0xFFE5E5EA))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch
            ) { onCheckedChange(!checked) }
            .testTag(testTag)
            .padding(padding),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(knobSize)
                .shadow(elevation = 2.dp, shape = CircleShape, spotColor = Color(0x25000000))
                .clip(CircleShape)
                .background(knobColor)
        )
    }
}

/**
 * Raised Circular crimson "+" FAB with glowing tinted shadow matching reference image
 */
@Composable
fun TactileAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "add_alarm_button"
) {
    Box(
        modifier = modifier
            .size(54.dp)
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                ambientColor = WakeUpCrimsonGlow,
                spotColor = WakeUpCrimsonGlow
            )
            .clip(CircleShape)
            .background(WakeUpCrimson)
            .clickable(
                onClick = onClick,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Alarm",
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}

/**
 * Tactile Crimson Primary Button matching the "Save" button in the reference image
 */
@Composable
fun TactileCrimsonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = "primary_button"
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WakeUpCrimson,
            contentColor = Color.White,
            disabledContainerColor = WakeUpDisabled,
            disabledContentColor = WakeUpTextSecondary
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag(testTag)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.1.sp,
                color = Color.White
            )
        )
    }
}

/**
 * Weekday selection chip matching the circular chips in the reference image: S M T W T F S
 */
@Composable
fun WeekdayChip(
    dayNumber: Int,
    label: String,
    isSelected: Boolean,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) WakeUpCrimson else WakeUpSurface,
        label = "chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF8E8E93),
        label = "chip_text"
    )
    val borderColor = if (isSelected) WakeUpCrimson else Color(0xFFE5E5EA)

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(BorderStroke(1.dp, borderColor), CircleShape)
            .clickable(
                onClick = { onToggle(dayNumber) },
                role = Role.Checkbox,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * TactileCard for clean grouped settings or summary cards
 */
@Composable
fun TactileCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = WakeUpSurface,
    borderColor: Color = WakeUpDivider,
    elevation: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color(0x06000000),
                spotColor = WakeUpCardShadow
            )
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), shape)
            .clickable(onClick = onClick)
    } else {
        modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color(0x06000000),
                spotColor = WakeUpCardShadow
            )
            .clip(shape)
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), shape)
    }

    Box(modifier = cardModifier) {
        content()
    }
}
