package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WakeUpColorScheme = lightColorScheme(
    primary = WakeUpCrimson,
    onPrimary = WakeUpSurface,
    primaryContainer = WakeUpCrimsonContainer,
    onPrimaryContainer = WakeUpCrimsonPressed,
    secondary = WakeUpTextPrimary,
    onSecondary = WakeUpSurface,
    secondaryContainer = WakeUpSurfaceSecondary,
    onSecondaryContainer = WakeUpTextPrimary,
    tertiary = WakeUpTextSecondary,
    background = WakeUpBackground,
    onBackground = WakeUpTextPrimary,
    surface = WakeUpSurface,
    onSurface = WakeUpTextPrimary,
    surfaceVariant = WakeUpSurfaceSecondary,
    onSurfaceVariant = WakeUpTextSecondary,
    outline = WakeUpDivider,
    outlineVariant = WakeUpDisabled
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = WakeUpBackground.toArgb()
                it.navigationBarColor = WakeUpBackground.toArgb()
                val controller = WindowCompat.getInsetsController(it, view)
                controller.isAppearanceLightStatusBars = true
                controller.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = WakeUpColorScheme,
        typography = Typography,
        content = content
    )
}
