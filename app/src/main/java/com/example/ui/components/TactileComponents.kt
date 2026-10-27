package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.ui.theme.WakeUpBackground
import com.example.ui.theme.WakeUpButtonShadow
import com.example.ui.theme.WakeUpCardShadow
import com.example.ui.theme.WakeUpCrimson
import com.example.ui.theme.WakeUpCrimsonBorder
import com.example.ui.theme.WakeUpCrimsonContainer
import com.example.ui.theme.WakeUpCrimsonPressed
import com.example.ui.theme.WakeUpDisabled
import com.example.ui.theme.WakeUpDivider
import com.example.ui.theme.WakeUpSurface
import com.example.ui.theme.WakeUpSurfaceSecondary
import com.example.ui.theme.WakeUpTextPrimary
import com.example.ui.theme.WakeUpTextSecondary

/**
 * TactileCard provides a soft 3D elevated surface with realistic subtle shadows
 * and clean rounded corners that give a tangible, physical aesthetic.
 */
@Composable
fun TactileCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = WakeUpSurface,
    borderColor: Color = WakeUpDivider.copy(alpha = 0.7f),
    elevation: Dp = 2.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color(0x0A000000),
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
                ambientColor = Color(0x0A000000),
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

/**
 * TactileSwitch customized with crimson thumb and soft tactile track
 */
@Composable
fun TactileSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "alarm_switch"
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.testTag(testTag),
        colors = SwitchDefaults.colors(
            checkedThumbColor = WakeUpCrimson,
            checkedTrackColor = WakeUpCrimsonContainer,
            checkedBorderColor = WakeUpCrimsonBorder,
            uncheckedThumbColor = WakeUpTextSecondary,
            uncheckedTrackColor = WakeUpSurfaceSecondary,
            uncheckedBorderColor = WakeUpDivider
        )
    )
}

/**
 * Raised Circular crimson "+" button
 */
@Composable
fun TactileAddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "add_alarm_button"
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                ambientColor = Color(0x1F000000),
                spotColor = WakeUpButtonShadow
            )
            .clip(CircleShape)
            .background(WakeUpCrimson)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)), CircleShape)
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
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * Tactile Crimson Primary Button with soft depth
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
            defaultElevation = 3.dp,
            pressedElevation = 1.dp,
            disabledElevation = 0.dp
        ),
        modifier = modifier
            .height(52.dp)
            .testTag(testTag)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
        )
    }
}

/**
 * Weekday selection chip with tactile feedback
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
        targetValue = if (isSelected) Color.White else WakeUpTextSecondary,
        label = "chip_text"
    )
    val borderColor = if (isSelected) WakeUpCrimsonPressed else WakeUpDivider

    Box(
        modifier = modifier
            .size(42.dp)
            .shadow(
                elevation = if (isSelected) 2.dp else 1.dp,
                shape = CircleShape,
                ambientColor = Color(0x08000000),
                spotColor = if (isSelected) WakeUpButtonShadow else WakeUpCardShadow
            )
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
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
    }
}
