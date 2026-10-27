package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlarmEntity
import com.example.data.model.ChallengeType
import com.example.ui.components.TactileCard
import com.example.ui.components.TactileCrimsonButton
import com.example.ui.components.TactileSwitch
import com.example.ui.components.TactileTimePicker
import com.example.ui.components.WeekdayChip
import com.example.ui.theme.WakeUpBackground
import com.example.ui.theme.WakeUpButtonShadow
import com.example.ui.theme.WakeUpCardShadow
import com.example.ui.theme.WakeUpCrimson
import com.example.ui.theme.WakeUpCrimsonBorder
import com.example.ui.theme.WakeUpCrimsonContainer
import com.example.ui.theme.WakeUpCrimsonPressed
import com.example.ui.theme.WakeUpDivider
import com.example.ui.theme.WakeUpSurface
import com.example.ui.theme.WakeUpSurfaceSecondary
import com.example.ui.theme.WakeUpTextPrimary
import com.example.ui.theme.WakeUpTextSecondary
import com.example.ui.theme.WakeUpTextTertiary
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlarmScreen(
    initialAlarm: AlarmEntity?,
    onSave: (AlarmEntity) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onCancel() }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var hour24 by remember { mutableIntStateOf(initialAlarm?.hour ?: 7) }
    var minute by remember { mutableIntStateOf(initialAlarm?.minute ?: 0) }
    var label by remember { mutableStateOf(initialAlarm?.label ?: "") }

    // Repeat Days: Set of 1..7 (1=Mon, 7=Sun)
    var selectedDays by remember {
        mutableStateOf(
            initialAlarm?.getRepeatDaysList()?.toSet() ?: setOf(1, 3, 5) // Default: Mon, Wed, Fri
        )
    }

    // Sequence Settings
    var isSequenceEnabled by remember { mutableStateOf(initialAlarm?.isSequenceEnabled ?: true) }
    var sequenceIntervalMinutes by remember { mutableIntStateOf(initialAlarm?.sequenceIntervalMinutes ?: 5) }
    var maxSequenceCount by remember { mutableIntStateOf(initialAlarm?.maxSequenceCount ?: 4) }

    // Challenge
    var challengeType by remember {
        mutableStateOf(initialAlarm?.challengeType ?: ChallengeType.MATHS.name)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(WakeUpBackground),
        containerColor = WakeUpBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WakeUpBackground)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                TactileCrimsonButton(
                    text = if (initialAlarm == null) "Save Routine Alarm" else "Update Routine Alarm",
                    onClick = {
                        val repeatDaysString = selectedDays.sorted().joinToString(",")
                        val entity = (initialAlarm ?: AlarmEntity()).copy(
                            hour = hour24,
                            minute = minute,
                            label = label.trim(),
                            repeatDays = repeatDaysString,
                            isEnabled = true,
                            isSequenceEnabled = isSequenceEnabled,
                            sequenceIntervalMinutes = sequenceIntervalMinutes,
                            maxSequenceCount = maxSequenceCount,
                            challengeType = challengeType
                        )
                        onSave(entity)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "save_alarm_button"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(WakeUpSurface)
                        .border(1.dp, WakeUpDivider, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = WakeUpTextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = if (initialAlarm == null) "New Alarm" else "Edit Alarm",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = WakeUpTextPrimary
                    )
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Time Picker
                item {
                    TactileTimePicker(
                        hour24 = hour24,
                        minute = minute,
                        onTimeChanged = { h, m ->
                            hour24 = h
                            minute = m
                        }
                    )
                }

                // 2. Alarm Label Card
                item {
                    TactileCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "LABEL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = WakeUpTextSecondary
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = label,
                                onValueChange = { label = it },
                                placeholder = {
                                    Text("e.g. College, Morning Routine, Workout", color = WakeUpTextTertiary)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = WakeUpCrimson,
                                    unfocusedBorderColor = WakeUpDivider,
                                    focusedContainerColor = WakeUpSurfaceSecondary,
                                    unfocusedContainerColor = WakeUpSurfaceSecondary
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Label suggestions
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("College", "Work", "Morning class", "Weekend").forEach { suggestion ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(WakeUpSurfaceSecondary)
                                            .border(1.dp, WakeUpDivider, RoundedCornerShape(8.dp))
                                            .clickable { label = suggestion }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = suggestion,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = WakeUpTextSecondary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Repeat Days Schedule
                item {
                    TactileCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "REPEAT SCHEDULE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = WakeUpTextSecondary
                                    )
                                )

                                Text(
                                    text = formatSelectedDaysSummary(selectedDays),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = WakeUpCrimson,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Day chips: M T W T F S S
                            val daysInfo = listOf(
                                1 to "M",
                                2 to "T",
                                3 to "W",
                                4 to "T",
                                5 to "F",
                                6 to "S",
                                7 to "S"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                daysInfo.forEach { (dayNum, chipLabel) ->
                                    WeekdayChip(
                                        dayNumber = dayNum,
                                        label = chipLabel,
                                        isSelected = selectedDays.contains(dayNum),
                                        onToggle = { toggledDay ->
                                            selectedDays = if (selectedDays.contains(toggledDay)) {
                                                selectedDays - toggledDay
                                            } else {
                                                selectedDays + toggledDay
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Preset toggles: Weekdays, Weekends, Every day
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DayPresetButton(
                                    label = "Weekdays",
                                    isActive = selectedDays == setOf(1, 2, 3, 4, 5),
                                    onClick = { selectedDays = setOf(1, 2, 3, 4, 5) }
                                )
                                DayPresetButton(
                                    label = "Weekends",
                                    isActive = selectedDays == setOf(6, 7),
                                    onClick = { selectedDays = setOf(6, 7) }
                                )
                                DayPresetButton(
                                    label = "Every day",
                                    isActive = selectedDays.size == 7,
                                    onClick = { selectedDays = (1..7).toSet() }
                                )
                                DayPresetButton(
                                    label = "Once",
                                    isActive = selectedDays.isEmpty(),
                                    onClick = { selectedDays = emptySet() }
                                )
                            }
                        }
                    }
                }

                // 4. Repeated-Alarm Sequence
                item {
                    TactileCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "WAKE-UP SEQUENCE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            color = WakeUpTextSecondary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "A chain of escalating alarms that cancels once you solve the challenge",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = WakeUpTextSecondary
                                        )
                                    )
                                }

                                TactileSwitch(
                                    checked = isSequenceEnabled,
                                    onCheckedChange = { isSequenceEnabled = it },
                                    testTag = "sequence_toggle"
                                )
                            }

                            AnimatedVisibility(visible = isSequenceEnabled) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Interval selector
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Interval between alarms:",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = WakeUpTextPrimary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(3, 5, 10, 15).forEach { mins ->
                                                ConfigOptionPill(
                                                    text = "${mins}m",
                                                    isSelected = sequenceIntervalMinutes == mins,
                                                    onClick = { sequenceIntervalMinutes = mins }
                                                )
                                            }
                                        }
                                    }

                                    // Maximum alarms count selector
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Max alarms in sequence:",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = WakeUpTextPrimary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf(2, 3, 4, 5).forEach { count ->
                                                ConfigOptionPill(
                                                    text = "$count",
                                                    isSelected = maxSequenceCount == count,
                                                    onClick = { maxSequenceCount = count }
                                                )
                                            }
                                        }
                                    }

                                    // Sequence Preview Roadmap
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(WakeUpSurfaceSecondary)
                                            .border(1.dp, WakeUpDivider, RoundedCornerShape(12.dp))
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            text = "SEQUENCE ROADMAP",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = WakeUpTextSecondary,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        val sequenceSteps = (0 until maxSequenceCount).map { i ->
                                            val totalMinutes = hour24 * 60 + minute + (i * sequenceIntervalMinutes)
                                            val h = (totalMinutes / 60) % 24
                                            val m = totalMinutes % 60
                                            val amPm = if (h < 12) "AM" else "PM"
                                            val displayH = if (h == 0) 12 else if (h > 12) h - 12 else h
                                            val isFinal = i == maxSequenceCount - 1
                                            String.format(Locale.getDefault(), "%d:%02d %s", displayH, m, amPm) to isFinal
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            sequenceSteps.forEachIndexed { idx, (timeStr, isFinal) ->
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isFinal) WakeUpCrimsonContainer else WakeUpSurface)
                                                        .border(
                                                            1.dp,
                                                            if (isFinal) WakeUpCrimsonBorder else WakeUpDivider,
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .padding(vertical = 6.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = if (isFinal) "Final" else "#${idx + 1}",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 10.sp,
                                                            color = if (isFinal) WakeUpCrimson else WakeUpTextSecondary,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                    Text(
                                                        text = timeStr,
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = if (isFinal) WakeUpCrimson else WakeUpTextPrimary
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Dismissing any alarm in the sequence completes the routine for that morning and cancels subsequent alarms, preserving next scheduled days.",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = WakeUpTextSecondary,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Wake-Up Challenge
                item {
                    TactileCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "WAKE-UP CHALLENGE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = WakeUpTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ensure your mind is awake before the sequence is dismissed",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = WakeUpTextSecondary
                                )
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            ChallengeChoiceItem(
                                title = "Maths Challenge",
                                subtitle = "Solve simple arithmetic (e.g. 18 + 27) with a dedicated tactile keypad",
                                icon = Icons.Default.Calculate,
                                isSelected = challengeType == ChallengeType.MATHS.name,
                                onClick = { challengeType = ChallengeType.MATHS.name }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            ChallengeChoiceItem(
                                title = "Standard Dismissal",
                                subtitle = "Single deliberate tap to dismiss without puzzles",
                                icon = Icons.Default.NotificationsActive,
                                isSelected = challengeType == ChallengeType.STANDARD.name,
                                onClick = { challengeType = ChallengeType.STANDARD.name }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            ChallengeChoiceItem(
                                title = "QR-Code Challenge",
                                subtitle = "Scan a predefined physical barcode/QR code (Prototype preview)",
                                icon = Icons.Default.QrCode,
                                isSelected = challengeType == ChallengeType.QR_CODE.name,
                                isPrototype = true,
                                onClick = {
                                    challengeType = ChallengeType.QR_CODE.name
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("QR Challenge selected (Prototype mode)")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayPresetButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) WakeUpCrimsonContainer else WakeUpSurfaceSecondary
    val textColor = if (isActive) WakeUpCrimson else WakeUpTextSecondary
    val borderColor = if (isActive) WakeUpCrimsonBorder else WakeUpDivider

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        )
    }
}

@Composable
private fun ConfigOptionPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) WakeUpCrimson else WakeUpSurfaceSecondary
    val textColor = if (isSelected) Color.White else WakeUpTextPrimary
    val borderColor = if (isSelected) WakeUpCrimsonPressed else WakeUpDivider

    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 32.dp)
            .shadow(
                elevation = if (isSelected) 2.dp else 0.dp,
                shape = RoundedCornerShape(8.dp),
                spotColor = WakeUpButtonShadow
            )
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(
                onClick = onClick,
                role = Role.RadioButton
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        )
    }
}

@Composable
private fun ChallengeChoiceItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    isPrototype: Boolean = false,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) WakeUpCrimson else WakeUpDivider
    val bgColor = if (isSelected) WakeUpCrimsonContainer.copy(alpha = 0.5f) else WakeUpSurfaceSecondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(BorderStroke(1.5.dp, borderColor), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) WakeUpCrimson else WakeUpSurface)
                    .border(1.dp, if (isSelected) WakeUpCrimsonPressed else WakeUpDivider, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else WakeUpTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = WakeUpTextPrimary
                        )
                    )
                    if (isPrototype) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(WakeUpDivider)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Preview",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WakeUpTextSecondary
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = WakeUpTextSecondary
                    )
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = WakeUpCrimson,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatSelectedDaysSummary(days: Set<Int>): String {
    if (days.isEmpty()) return "Once"
    if (days.size == 7) return "Every day"
    if (days == setOf(1, 2, 3, 4, 5)) return "Weekdays"
    if (days == setOf(6, 7)) return "Weekends"
    val names = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
    return days.sorted().mapNotNull { names[it] }.joinToString(", ")
}
