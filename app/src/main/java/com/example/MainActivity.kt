package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AlarmEntity
import com.example.ui.screens.AddEditAlarmScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.WakeUpBackground
import com.example.ui.viewmodel.AlarmViewModel

sealed interface AppScreen {
    data object Home : AppScreen
    data object AddAlarm : AppScreen
    data class EditAlarm(val alarm: AlarmEntity) : AppScreen
}

class MainActivity : ComponentActivity() {

    private val viewModel: AlarmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                // Request Notification Permission on Android 13+
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) {}

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = WakeUpBackground
                ) {
                    WakeUpApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun WakeUpApp(viewModel: AlarmViewModel) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val nextAlarmPair by viewModel.nextAlarmPair.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            is AppScreen.Home -> {
                HomeScreen(
                    alarms = alarms,
                    nextAlarmPair = nextAlarmPair,
                    onAddAlarm = { currentScreen = AppScreen.AddAlarm },
                    onEditAlarm = { alarm -> currentScreen = AppScreen.EditAlarm(alarm) },
                    onToggleAlarm = { alarm, isEnabled -> viewModel.toggleAlarm(alarm, isEnabled) },
                    onDeleteAlarm = { alarm -> viewModel.deleteAlarm(alarm) },
                    onSaveRoutine = { parsed -> viewModel.saveParsedRoutine(parsed) },
                    onOpenSettings = { showSettingsDialog = true }
                )
            }

            is AppScreen.AddAlarm -> {
                AddEditAlarmScreen(
                    initialAlarm = null,
                    onSave = { newAlarm ->
                        viewModel.saveAlarm(newAlarm) {
                            currentScreen = AppScreen.Home
                        }
                    },
                    onCancel = { currentScreen = AppScreen.Home }
                )
            }

            is AppScreen.EditAlarm -> {
                AddEditAlarmScreen(
                    initialAlarm = screen.alarm,
                    onSave = { updatedAlarm ->
                        viewModel.saveAlarm(updatedAlarm) {
                            currentScreen = AppScreen.Home
                        }
                    },
                    onCancel = { currentScreen = AppScreen.Home }
                )
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { showSettingsDialog = false },
            onTestAlarmIn5Seconds = {
                viewModel.scheduleTestAlarmInSeconds(5)
            }
        )
    }
}
