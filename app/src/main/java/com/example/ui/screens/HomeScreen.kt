package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlarmEntity
import com.example.data.model.ChallengeType
import com.example.scheduler.AlarmScheduler
import com.example.ui.AlarmRingingActivity
import com.example.ui.components.TactileAddButton
import com.example.ui.components.TactileCard
import com.example.ui.components.TactileCrimsonButton
import com.example.ui.components.TactileSwitch
import com.example.ui.theme.WakeUpBackground
import com.example.ui.theme.WakeUpButtonShadow
import com.example.ui.theme.WakeUpCardShadow
import com.example.ui.theme.WakeUpCrimson
import com.example.ui.theme.WakeUpCrimsonBorder
import com.example.ui.theme.WakeUpCrimsonContainer
import com.example.ui.theme.WakeUpDisabled
import com.example.ui.theme.WakeUpDivider
import com.example.ui.theme.WakeUpSurface
import com.example.ui.theme.WakeUpSurfaceSecondary
import com.example.ui.theme.WakeUpTextPrimary
import com.example.ui.theme.WakeUpTextSecondary
import com.example.ui.theme.WakeUpTextTertiary
import com.example.util.ParsedRoutine
import com.example.util.RoutineParser
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    alarms: List<AlarmEntity>,
    nextAlarmPair: Pair<AlarmEntity, Calendar>?,
    onAddAlarm: () -> Unit,
    onEditAlarm: (AlarmEntity) -> Unit,
    onToggleAlarm: (AlarmEntity, Boolean) -> Unit,
    onDeleteAlarm: (AlarmEntity) -> Unit,
    onSaveRoutine: (ParsedRoutine) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var alarmToDelete by remember { mutableStateOf<AlarmEntity?>(null) }
    var isRoutineInputExpanded by remember { mutableStateOf(false) }
    var routinePromptText by remember { mutableStateOf("") }
    var parsedRoutinePreview by remember { mutableStateOf<ParsedRoutine?>(null) }

    val currentDateStr = remember {
        val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        sdf.format(Date())
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(WakeUpBackground),
        containerColor = WakeUpBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            TactileAddButton(
                onClick = onAddAlarm,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp, end = 16.dp),
                testTag = "home_add_button"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Header Bar
            HomeHeaderBar(
                currentDate = currentDateStr,
                onOpenSettings = onOpenSettings
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Next Scheduled Alarm Summary Card
                item {
                    NextAlarmSummaryCard(
                        nextAlarmPair = nextAlarmPair
                    )
                }

                // Quick Routine Setup Assistant Card
                item {
                    RoutineAssistantCard(
                        isExpanded = isRoutineInputExpanded,
                        promptText = routinePromptText,
                        onToggleExpand = { isRoutineInputExpanded = !isRoutineInputExpanded },
                        onTextChanged = { routinePromptText = it },
                        onInterpret = {
                            if (routinePromptText.isNotBlank()) {
                                parsedRoutinePreview = RoutineParser.parse(routinePromptText)
                            }
                        },
                        onApplyPreset = { preset ->
                            routinePromptText = preset
                            parsedRoutinePreview = RoutineParser.parse(preset)
                        }
                    )
                }

                // Section Title: Your Alarms
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ALARMS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = WakeUpTextSecondary
                            )
                        )
                        Text(
                            text = "${alarms.size} configured",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = WakeUpTextTertiary
                            )
                        )
                    }
                }

                // Empty State or Alarm List
                if (alarms.isEmpty()) {
                    item {
                        EmptyAlarmsState(
                            onCreateFirstAlarm = onAddAlarm,
                            onLoadSampleCollegeRoutine = {
                                val collegePreset = RoutineParser.parse(
                                    "I have college on Monday, Wednesday, and Friday. I need to wake up at 7 AM. Repeat alarms every 5 minutes, with a maximum of four alarms."
                                )
                                onSaveRoutine(collegePreset)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("College waking routine created (Mon/Wed/Fri 7:00 AM)")
                                }
                            }
                        )
                    }
                } else {
                    items(
                        items = alarms,
                        key = { it.id }
                    ) { alarm ->
                        AlarmListItem(
                            alarm = alarm,
                            onToggle = { isChecked -> onToggleAlarm(alarm, isChecked) },
                            onClick = { onEditAlarm(alarm) },
                            onDelete = { alarmToDelete = alarm },
                            onPreviewTest = {
                                // Launch test wake-up challenge immediately
                                val intent = Intent(context, AlarmRingingActivity::class.java).apply {
                                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
                                    putExtra(AlarmScheduler.EXTRA_SEQUENCE_INDEX, 0)
                                    putExtra(AlarmScheduler.EXTRA_MAX_SEQUENCE, if (alarm.isSequenceEnabled) alarm.maxSequenceCount else 1)
                                    putExtra(AlarmScheduler.EXTRA_OCCURRENCE_DATE, "TEST_PREVIEW")
                                    putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, alarm.label.ifBlank { "Test Preview" })
                                    putExtra(AlarmScheduler.EXTRA_CHALLENGE_TYPE, alarm.challengeType)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    alarmToDelete?.let { alarm ->
        AlertDialog(
            onDismissRequest = { alarmToDelete = null },
            title = {
                Text(
                    text = "Delete Alarm",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${alarm.label.ifBlank { alarm.formattedTime() }}'? All scheduled sequence alarms will be cancelled.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAlarm(alarm)
                        alarmToDelete = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Alarm deleted")
                        }
                    }
                ) {
                    Text("Delete", color = WakeUpCrimson, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { alarmToDelete = null }) {
                    Text("Cancel", color = WakeUpTextSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = WakeUpSurface
        )
    }

    // Routine Interpretation Review Dialog
    parsedRoutinePreview?.let { parsed ->
        AlertDialog(
            onDismissRequest = { parsedRoutinePreview = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = WakeUpCrimson,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Review Interpreted Routine",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Wake Up interpreted your routine without requiring cloud AI:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WakeUpTextSecondary
                    )

                    TactileCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = WakeUpSurfaceSecondary,
                        elevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Label: ${parsed.label}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "Time: ${parsed.formattedTime()}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = WakeUpCrimson
                                )
                            )
                            Text(
                                text = "Days: ${parsed.formattedDays()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Sequence: ${parsed.maxSequenceCount} alarms (${parsed.intervalMinutes} min intervals)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TactileCrimsonButton(
                    text = "Save Routine",
                    onClick = {
                        onSaveRoutine(parsed)
                        parsedRoutinePreview = null
                        isRoutineInputExpanded = false
                        routinePromptText = ""
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Routine saved! Alarms scheduled.")
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { parsedRoutinePreview = null }) {
                    Text("Adjust", color = WakeUpTextSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = WakeUpSurface
        )
    }
}

@Composable
private fun HomeHeaderBar(
    currentDate: String,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Wake Up",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    color = WakeUpTextPrimary
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = currentDate,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = WakeUpTextSecondary,
                    fontWeight = FontWeight.Normal
                )
            )
        }

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(44.dp)
                .shadow(1.dp, CircleShape, spotColor = WakeUpCardShadow)
                .clip(CircleShape)
                .background(WakeUpSurface)
                .border(1.dp, WakeUpDivider, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings & Diagnostics",
                tint = WakeUpTextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun NextAlarmSummaryCard(
    nextAlarmPair: Pair<AlarmEntity, Calendar>?
) {
    TactileCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 3.dp,
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = WakeUpCrimson,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "YOUR NEXT ALARM",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = WakeUpCrimson
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (nextAlarmPair != null) {
                    val alarm = nextAlarmPair.first
                    val targetCal = nextAlarmPair.second
                    val remainingMillis = targetCal.timeInMillis - System.currentTimeMillis()
                    val hours = (remainingMillis / (1000 * 60 * 60)).coerceAtLeast(0)
                    val minutes = ((remainingMillis / (1000 * 60)) % 60).coerceAtLeast(0)

                    Text(
                        text = alarm.formattedTime(),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = WakeUpTextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val dayLabel = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(targetCal.time)
                    val remainingText = if (hours > 0) "in ${hours}h ${minutes}m" else "in ${minutes}m"

                    Text(
                        text = "$dayLabel • $remainingText",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = WakeUpTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    if (alarm.isSequenceEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sequence: ${alarm.maxSequenceCount} alarms (${alarm.sequenceIntervalMinutes}m interval)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = WakeUpTextTertiary
                            )
                        )
                    }
                } else {
                    Text(
                        text = "No Alarms Scheduled",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = WakeUpTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Set a routine or tap + to create an alarm",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = WakeUpTextSecondary
                        )
                    )
                }
            }

            // Decorative soft clock indicator
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(WakeUpSurfaceSecondary)
                    .border(1.dp, WakeUpDivider, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = if (nextAlarmPair != null) WakeUpCrimson else WakeUpDisabled,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun RoutineAssistantCard(
    isExpanded: Boolean,
    promptText: String,
    onToggleExpand: () -> Unit,
    onTextChanged: (String) -> Unit,
    onInterpret: () -> Unit,
    onApplyPreset: (String) -> Unit
) {
    TactileCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(20.dp),
        backgroundColor = WakeUpSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(WakeUpCrimsonContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = WakeUpCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Set Routine with Natural Text",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = WakeUpTextPrimary
                            )
                        )
                        Text(
                            text = if (isExpanded) "Type your schedule below" else "Tap to quickly create a recurring schedule",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = WakeUpTextSecondary
                            )
                        )
                    }
                }

                Text(
                    text = if (isExpanded) "Hide" else "Open",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = WakeUpCrimson,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = onTextChanged,
                        placeholder = {
                            Text(
                                "e.g. I have college Mon, Wed, Fri at 7 AM. 5 min intervals, 4 alarms max",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WakeUpTextTertiary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WakeUpCrimson,
                            unfocusedBorderColor = WakeUpDivider,
                            focusedContainerColor = WakeUpSurfaceSecondary,
                            unfocusedContainerColor = WakeUpSurfaceSecondary
                        ),
                        maxLines = 3
                    )

                    // Presets chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Or pick a routine template:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = WakeUpTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RoutineChip(
                                text = "College MWF 7am",
                                onClick = {
                                    onApplyPreset("I have college on Monday, Wednesday, and Friday at 7 AM. 4 alarms every 5 mins.")
                                }
                            )
                            RoutineChip(
                                text = "Work Weekdays 6:30am",
                                onClick = {
                                    onApplyPreset("Work on weekdays at 6:30 AM. 3 alarms every 5 mins.")
                                }
                            )
                        }
                    }

                    TactileCrimsonButton(
                        text = "Interpret & Save Routine",
                        onClick = onInterpret,
                        enabled = promptText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(WakeUpSurfaceSecondary)
            .border(1.dp, WakeUpDivider, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = WakeUpTextPrimary,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun AlarmListItem(
    alarm: AlarmEntity,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onPreviewTest: () -> Unit
) {
    TactileCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(20.dp),
        onClick = onClick
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
                // Time & Label
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alarm.formattedTime(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (alarm.isEnabled) WakeUpTextPrimary else WakeUpTextTertiary
                        )
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = alarm.label.ifBlank { "Alarm" },
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = if (alarm.isEnabled) WakeUpTextPrimary else WakeUpTextTertiary,
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Switch
                TactileSwitch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    testTag = "switch_${alarm.id}"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata row: Days, Sequence & Challenge tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Recurring Days tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(WakeUpSurfaceSecondary)
                            .border(1.dp, WakeUpDivider, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = alarm.formattedDays(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (alarm.isEnabled) WakeUpTextPrimary else WakeUpTextTertiary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    // Sequence tag
                    if (alarm.isSequenceEnabled) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(WakeUpCrimsonContainer)
                                .border(1.dp, WakeUpCrimsonBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${alarm.maxSequenceCount}x (${alarm.sequenceIntervalMinutes}m)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = WakeUpCrimson,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    // Challenge tag
                    val challengeIcon = when (alarm.getChallengeTypeEnum()) {
                        ChallengeType.MATHS -> Icons.Default.Calculate
                        ChallengeType.QR_CODE -> Icons.Default.QrCode
                        ChallengeType.STANDARD -> Icons.Default.NotificationsActive
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(WakeUpSurfaceSecondary)
                            .border(1.dp, WakeUpDivider, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = challengeIcon,
                                contentDescription = null,
                                tint = WakeUpTextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (alarm.getChallengeTypeEnum()) {
                                    ChallengeType.MATHS -> "Maths"
                                    ChallengeType.QR_CODE -> "QR"
                                    ChallengeType.STANDARD -> "Standard"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = WakeUpTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }

                // Actions: Preview Test & Delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPreviewTest,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Test / Preview Ringing Flow",
                            tint = WakeUpCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete Alarm",
                            tint = WakeUpTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAlarmsState(
    onCreateFirstAlarm: () -> Unit,
    onLoadSampleCollegeRoutine: () -> Unit
) {
    TactileCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        elevation = 2.dp,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(WakeUpCrimsonContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = WakeUpCrimson,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Alarms Created Yet",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = WakeUpTextPrimary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Wake Up lets you set a weekly recurring routine once. Define days, times, and sequential waking alarms without manual re-creation.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = WakeUpTextSecondary,
                    lineHeight = 22.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            TactileCrimsonButton(
                text = "Create First Alarm",
                onClick = onCreateFirstAlarm,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(onClick = onLoadSampleCollegeRoutine) {
                Text(
                    text = "Load Student Schedule (Mon/Wed/Fri 7:00 AM)",
                    color = WakeUpCrimson,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}
