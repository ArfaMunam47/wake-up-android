package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlarmEntity
import com.example.data.model.AlarmSound
import com.example.data.model.ChallengeType
import com.example.service.AlarmRingManager
import com.example.ui.components.TactileCard
import com.example.ui.components.TactileCrimsonButton
import com.example.ui.components.TactileSwitch
import com.example.ui.components.TactileTimePicker
import com.example.ui.components.WeekdayChip
import com.example.ui.theme.WakeUpCrimson
import com.example.ui.theme.WakeUpCrimsonBorder
import com.example.ui.theme.WakeUpCrimsonContainer
import com.example.ui.theme.WakeUpDivider
import com.example.ui.theme.WakeUpSurface
import com.example.ui.theme.WakeUpSurfaceSecondary
import com.example.ui.theme.WakeUpTextPrimary
import com.example.ui.theme.WakeUpTextSecondary
import com.example.ui.theme.WakeUpTextTertiary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlarmScreen(
    initialAlarm: AlarmEntity?,
    onSave: (AlarmEntity) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var hour24 by remember { mutableIntStateOf(initialAlarm?.hour ?: 9) }
    var minute by remember { mutableIntStateOf(initialAlarm?.minute ?: 2) }
    var label by remember { mutableStateOf(initialAlarm?.label ?: "Wake Up!") }

    // Repeat Days: 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat, 7=Sun
    var selectedDays by remember {
        mutableStateOf(
            initialAlarm?.getRepeatDaysList()?.toSet() ?: setOf(1, 2, 3, 4, 5) // Weekdays default
        )
    }

    // Sequence Settings
    var isSequenceEnabled by remember { mutableStateOf(initialAlarm?.isSequenceEnabled ?: true) }
    var sequenceIntervalMinutes by remember { mutableIntStateOf(initialAlarm?.sequenceIntervalMinutes ?: 5) }
    var maxSequenceCount by remember { mutableIntStateOf(initialAlarm?.maxSequenceCount ?: 4) }

    // Sound Selection
    var selectedSound by remember {
        mutableStateOf(initialAlarm?.getSoundEnum() ?: AlarmSound.GENTLE)
    }
    var isSoundPickerOpen by remember { mutableStateOf(false) }

    // Challenge
    var challengeType by remember {
        mutableStateOf(initialAlarm?.challengeType ?: ChallengeType.MATHS.name)
    }

    // Stop sound preview if screen is dismissed or navigated away from
    DisposableEffect(Unit) {
        onDispose {
            AlarmRingManager.stopPreview()
        }
    }

    BackHandler {
        AlarmRingManager.stopPreview()
        if (isSoundPickerOpen) {
            isSoundPickerOpen = false
        } else {
            onCancel()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(WakeUpSurface),
        containerColor = WakeUpSurface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Save button matching reference image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WakeUpSurface)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                TactileCrimsonButton(
                    text = "Save",
                    onClick = {
                        AlarmRingManager.stopPreview()
                        val repeatDaysString = selectedDays.sorted().joinToString(",")
                        val entity = (initialAlarm ?: AlarmEntity()).copy(
                            hour = hour24,
                            minute = minute,
                            label = label.trim().ifBlank { "Wake Up!" },
                            repeatDays = repeatDaysString,
                            isEnabled = true,
                            isSequenceEnabled = isSequenceEnabled,
                            sequenceIntervalMinutes = sequenceIntervalMinutes,
                            maxSequenceCount = maxSequenceCount,
                            challengeType = challengeType,
                            soundName = selectedSound.name
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
            // Top Bar: < Set Alarm
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = WakeUpTextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "Set Alarm",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = WakeUpTextPrimary
                    )
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                // 1. Time Wheel Selector (3 rows, 3 columns)
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

                item { Spacer(modifier = Modifier.height(28.dp)) }

                // 2. Label Section
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Label",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                color = WakeUpTextSecondary,
                                fontWeight = FontWeight.Normal
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        BasicTextField(
                            value = label,
                            onValueChange = { label = it },
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WakeUpTextPrimary
                            ),
                            cursorBrush = SolidColor(WakeUpCrimson),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            decorationBox = { innerTextField ->
                                if (label.isEmpty()) {
                                    Text(
                                        text = "Wake Up!",
                                        fontSize = 16.sp,
                                        color = WakeUpTextTertiary
                                    )
                                }
                                innerTextField()
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = WakeUpDivider, thickness = 1.dp)
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                // 3. Repeat Section: S M T W T F S in a row
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Repeat",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                color = WakeUpTextSecondary,
                                fontWeight = FontWeight.Normal
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Reference day order: S, M, T, W, T, F, S (7=Sun, 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat)
                        val daysOrder = listOf(
                            7 to "S",
                            1 to "M",
                            2 to "T",
                            3 to "W",
                            4 to "T",
                            5 to "F",
                            6 to "S"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            daysOrder.forEach { (dayNum, chipLabel) ->
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

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = WakeUpDivider, thickness = 1.dp)
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                // 4. Repeated Sequence Section
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Sequence",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        color = WakeUpTextSecondary,
                                        fontWeight = FontWeight.Normal
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isSequenceEnabled)
                                        "${maxSequenceCount} alarms (${sequenceIntervalMinutes}m interval)"
                                    else
                                        "Single alarm",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = WakeUpTextPrimary
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
                                    .padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Interval selector row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Interval",
                                        style = MaterialTheme.typography.bodySmall.copy(color = WakeUpTextSecondary)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(3, 5, 10, 15).forEach { mins ->
                                            InlinePill(
                                                text = "${mins}m",
                                                isSelected = sequenceIntervalMinutes == mins,
                                                onClick = { sequenceIntervalMinutes = mins }
                                            )
                                        }
                                    }
                                }

                                // Max count selector row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Max alarms",
                                        style = MaterialTheme.typography.bodySmall.copy(color = WakeUpTextSecondary)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(2, 3, 4, 5).forEach { count ->
                                            InlinePill(
                                                text = "$count",
                                                isSelected = maxSequenceCount == count,
                                                onClick = { maxSequenceCount = count }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = WakeUpDivider, thickness = 1.dp)
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                // 5. Ringtone Section matching reference image
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSoundPickerOpen = true }
                    ) {
                        Text(
                            text = "Ringtone",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                color = WakeUpTextSecondary,
                                fontWeight = FontWeight.Normal
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedSound.displayName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WakeUpTextPrimary
                                )
                            )

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Select ringtone",
                                tint = Color(0xFFC7C7CC),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = WakeUpDivider, thickness = 1.dp)
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                // 6. Dismissal Challenge Section
                item {
                    val challengeLabel = when (challengeType) {
                        ChallengeType.MATHS.name -> "Maths challenge"
                        ChallengeType.QR_CODE.name -> "QR-Code challenge"
                        else -> "Standard dismissal"
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Cycle through challenge types
                                challengeType = when (challengeType) {
                                    ChallengeType.MATHS.name -> ChallengeType.STANDARD.name
                                    ChallengeType.STANDARD.name -> ChallengeType.QR_CODE.name
                                    else -> ChallengeType.MATHS.name
                                }
                            }
                    ) {
                        Text(
                            text = "Dismissal Challenge",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 12.sp,
                                color = WakeUpTextSecondary,
                                fontWeight = FontWeight.Normal
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = challengeLabel,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WakeUpTextPrimary
                                )
                            )

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Change challenge",
                                tint = Color(0xFFC7C7CC),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = WakeUpDivider, thickness = 1.dp)
                    }
                }
            }
        }
    }

    // Sound Picker Modal
    if (isSoundPickerOpen) {
        SoundPickerModal(
            currentSound = selectedSound,
            onSoundSelected = { sound ->
                selectedSound = sound
            },
            onDismiss = {
                AlarmRingManager.stopPreview()
                isSoundPickerOpen = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SoundPickerModal(
    currentSound: AlarmSound,
    onSoundSelected: (AlarmSound) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var previewingSound by remember { mutableStateOf<AlarmSound?>(null) }

    BasicAlertDialog(
        onDismissRequest = {
            AlarmRingManager.stopPreview()
            onDismiss()
        }
    ) {
        TactileCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            elevation = 6.dp,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = WakeUpCrimson,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Alarm Sound",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = WakeUpTextPrimary,
                            fontSize = 20.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Select a tone for this routine. Audio loops until the challenge is solved.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = WakeUpTextSecondary,
                        fontSize = 12.sp
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                val sounds = listOf(
                    Triple(
                        AlarmSound.GENTLE,
                        "Gentle Chime",
                        "Soft, soothing pentatonic chime for peaceful mornings"
                    ),
                    Triple(
                        AlarmSound.CLASSIC,
                        "Twin Bell Classic",
                        "Traditional pulsed mechanical alarm tone"
                    ),
                    Triple(
                        AlarmSound.URGENT,
                        "Urgent Alert",
                        "Fast, energetic electronic pulse for heavy sleepers"
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    sounds.forEach { (sound, title, description) ->
                        val isSelected = currentSound == sound
                        val isPlaying = previewingSound == sound

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) WakeUpCrimsonContainer else WakeUpSurfaceSecondary)
                                .border(
                                    1.dp,
                                    if (isSelected) WakeUpCrimsonBorder else WakeUpDivider,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    onSoundSelected(sound)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Selection circle indicator
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) WakeUpCrimson else Color.Transparent)
                                        .border(
                                            1.5.dp,
                                            if (isSelected) WakeUpCrimson else Color(0xFFC7C7CC),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) WakeUpCrimson else WakeUpTextPrimary,
                                            fontSize = 15.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = WakeUpTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Play / Stop Preview button
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) WakeUpCrimson else WakeUpSurface)
                                    .border(1.dp, if (isPlaying) WakeUpCrimson else WakeUpDivider, CircleShape)
                                    .clickable {
                                        AlarmRingManager.togglePreview(context, sound) { playing ->
                                            previewingSound = if (playing) sound else null
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Stop Preview" else "Play Preview",
                                    tint = if (isPlaying) Color.White else WakeUpTextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                TactileCrimsonButton(
                    text = "Done",
                    onClick = {
                        AlarmRingManager.stopPreview()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun InlinePill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) WakeUpCrimson else Color(0xFFF2F2F4)
    val textColor = if (isSelected) Color.White else WakeUpTextPrimary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                fontSize = 12.sp
            )
        )
    }
}
