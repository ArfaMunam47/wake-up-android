package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlarmEntity
import com.example.scheduler.AlarmScheduler
import com.example.ui.AlarmRingingActivity
import com.example.ui.components.TactileAddButton
import com.example.ui.components.TactileCrimsonButton
import com.example.ui.components.TactileSwitch
import com.example.ui.theme.WakeUpBackground
import com.example.ui.theme.WakeUpCrimson
import com.example.ui.theme.WakeUpDivider
import com.example.ui.theme.WakeUpSurface
import com.example.ui.theme.WakeUpTextPrimary
import com.example.ui.theme.WakeUpTextSecondary
import com.example.ui.theme.WakeUpTextTertiary
import com.example.util.ParsedRoutine
import com.example.util.RoutineParser
import kotlinx.coroutines.launch
import java.util.Calendar

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
    var showMenu by remember { mutableStateOf(false) }
    var selectedNavTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(WakeUpSurface),
        containerColor = WakeUpSurface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ReferenceBottomNavigationBar(
                selectedTab = selectedNavTab,
                onTabSelected = { selectedNavTab = it }
            )
        },
        floatingActionButton = {
            TactileAddButton(
                onClick = onAddAlarm,
                modifier = Modifier
                    .padding(bottom = 8.dp, end = 8.dp),
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
            // Clean Top Bar: "Alarm" + Overflow Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Alarm",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        color = WakeUpTextPrimary
                    )
                )

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = WakeUpTextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Settings & Permissions") },
                            onClick = {
                                showMenu = false
                                onOpenSettings()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Load Student Routine (Mon/Wed/Fri 7 AM)") },
                            onClick = {
                                showMenu = false
                                val preset = RoutineParser.parse(
                                    "I have college on Monday, Wednesday, and Friday. I need to wake up at 7 AM. Repeat alarms every 5 minutes, with a maximum of four alarms."
                                )
                                onSaveRoutine(preset)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Student routine loaded (Mon, Wed, Fri 7:00 AM)")
                                }
                            }
                        )
                    }
                }
            }

            if (selectedNavTab == 0) {
                // Main Alarm List
                if (alarms.isEmpty()) {
                    EmptyAlarmsView(
                        onCreateFirstAlarm = onAddAlarm,
                        onLoadSampleRoutine = {
                            val preset = RoutineParser.parse(
                                "I have college on Monday, Wednesday, and Friday. I need to wake up at 7 AM. Repeat alarms every 5 minutes, with a maximum of four alarms."
                            )
                            onSaveRoutine(preset)
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        itemsIndexed(
                            items = alarms,
                            key = { _, item -> item.id }
                        ) { index, alarm ->
                            AlarmListRow(
                                alarm = alarm,
                                onToggle = { isChecked -> onToggleAlarm(alarm, isChecked) },
                                onClick = { onEditAlarm(alarm) },
                                onLongClick = { alarmToDelete = alarm },
                                onPreviewTest = {
                                    val intent = Intent(context, AlarmRingingActivity::class.java).apply {
                                        putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarm.id)
                                        putExtra(AlarmScheduler.EXTRA_SEQUENCE_INDEX, 0)
                                        putExtra(
                                            AlarmScheduler.EXTRA_MAX_SEQUENCE,
                                            if (alarm.isSequenceEnabled) alarm.maxSequenceCount else 1
                                        )
                                        putExtra(AlarmScheduler.EXTRA_OCCURRENCE_DATE, "TEST_PREVIEW")
                                        putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, alarm.label.ifBlank { "Test Preview" })
                                        putExtra(AlarmScheduler.EXTRA_CHALLENGE_TYPE, alarm.challengeType)
                                        putExtra(AlarmScheduler.EXTRA_ALARM_SOUND, alarm.soundName)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                }
                            )

                            // Delicate thin horizontal divider line matching reference image
                            Divider(
                                color = WakeUpDivider,
                                thickness = 1.dp,
                                modifier = Modifier.padding(start = 24.dp, end = 24.dp)
                            )
                        }
                    }
                }
            } else {
                // Secondary tabs matching reference bottom bar
                CompanionTabView(
                    tabIndex = selectedNavTab,
                    onBackToAlarms = { selectedNavTab = 0 }
                )
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
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Delete ${alarm.formattedTime()} (${alarm.label.ifBlank { "Alarm" }})?",
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
                    Text("Delete", color = WakeUpCrimson, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { alarmToDelete = null }) {
                    Text("Cancel", color = WakeUpTextSecondary)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = WakeUpSurface
        )
    }
}

/**
 * Individual Alarm Row matching the reference image layout:
 * - Left: Large alarm time (e.g. 7:30 AM), label (Wake Up!), repeat days (Mon, Tue, Wed, Thur, Fri)
 * - Right: Reference switch with crimson sliding knob
 */
@Composable
private fun AlarmListRow(
    alarm: AlarmEntity,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPreviewTest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Prominent Alarm Time (26sp bold, clean)
            Text(
                text = alarm.formattedTime(),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (alarm.isEnabled) WakeUpTextPrimary else WakeUpTextTertiary
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Label (e.g., Wake Up!)
            Text(
                text = alarm.label.ifBlank { "Wake Up!" },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    color = if (alarm.isEnabled) WakeUpTextSecondary else WakeUpTextTertiary,
                    fontWeight = FontWeight.Normal
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Recurring Days (e.g., Mon, Tue, Wed, Thur, Fri)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = alarm.formattedDays(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        color = if (alarm.isEnabled) WakeUpTextTertiary else Color(0xFFC0C0C5),
                        fontWeight = FontWeight.Normal
                    )
                )

                val soundDisplayName = alarm.getSoundEnum().displayName
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "• $soundDisplayName",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = if (alarm.isEnabled) WakeUpTextSecondary else WakeUpTextTertiary,
                        fontWeight = FontWeight.Normal
                    )
                )

                if (alarm.isSequenceEnabled) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${alarm.maxSequenceCount} alarms (${alarm.sequenceIntervalMinutes}m)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = if (alarm.isEnabled) WakeUpCrimson else WakeUpTextTertiary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Quick preview ring icon
            IconButton(
                onClick = onPreviewTest,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Test Wake Up",
                    tint = if (alarm.isEnabled) WakeUpCrimson.copy(alpha = 0.8f) else WakeUpTextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Tactile physical switch with sliding knob
            TactileSwitch(
                checked = alarm.isEnabled,
                onCheckedChange = onToggle,
                testTag = "switch_${alarm.id}"
            )
        }
    }
}

/**
 * Bottom Navigation Bar matching the reference image:
 * Alarm (selected in crimson), World Clock, Stopwatch, Timer
 */
@Composable
private fun ReferenceBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val items = listOf(
        Triple("Alarm", Icons.Default.Alarm, 0),
        Triple("World Clock", Icons.Outlined.Language, 1),
        Triple("Stopwatch", Icons.Outlined.Timer, 2),
        Triple("Timer", Icons.Outlined.HourglassEmpty, 3)
    )

    NavigationBar(
        containerColor = WakeUpSurface,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(64.dp)
    ) {
        items.forEach { (label, icon, index) ->
            val isSelected = selectedTab == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = WakeUpCrimson,
                    selectedTextColor = WakeUpCrimson,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = Color(0xFF8E8E93),
                    unselectedTextColor = Color(0xFF8E8E93)
                )
            )
        }
    }
}

@Composable
private fun EmptyAlarmsView(
    onCreateFirstAlarm: () -> Unit,
    onLoadSampleRoutine: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFF7F7F8)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = null,
                tint = WakeUpTextTertiary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Alarms",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = WakeUpTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Tap + to add your first waking routine, or load the student weekday schedule.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = WakeUpTextSecondary,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        TactileCrimsonButton(
            text = "Create Alarm",
            onClick = onCreateFirstAlarm,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(onClick = onLoadSampleRoutine) {
            Text(
                text = "Load Routine (Mon/Wed/Fri 7:00 AM)",
                color = WakeUpCrimson,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
private fun CompanionTabView(
    tabIndex: Int,
    onBackToAlarms: () -> Unit
) {
    val tabTitle = when (tabIndex) {
        1 -> "World Clock"
        2 -> "Stopwatch"
        else -> "Timer"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = tabTitle,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = WakeUpTextPrimary
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Wake Up focus is on smart waking routines and alarm sequences.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = WakeUpTextSecondary,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        TextButton(onClick = onBackToAlarms) {
            Text(
                text = "← Back to Alarms",
                color = WakeUpCrimson,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}
