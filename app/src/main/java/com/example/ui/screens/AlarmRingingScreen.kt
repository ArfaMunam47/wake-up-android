package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChallengeType
import com.example.ui.components.TactileCard
import com.example.ui.components.TactileCrimsonButton
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun AlarmRingingScreen(
    alarmId: Long,
    sequenceIndex: Int,
    maxSequence: Int,
    occurrenceDate: String,
    label: String,
    challengeType: String,
    onDismissComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTimeStr by remember {
        mutableStateOf(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()))
    }

    // Keep live clock ticking every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        }
    }

    // Gentle pulse animation for the ringing bell
    val infiniteTransition = rememberInfiniteTransition(label = "ring_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Emergency hold-to-dismiss state (3 seconds)
    var isEmergencyHolding by remember { mutableStateOf(false) }
    var emergencyProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isEmergencyHolding) {
        if (isEmergencyHolding) {
            val startTime = System.currentTimeMillis()
            val duration = 3000L
            while (isEmergencyHolding && emergencyProgress < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                emergencyProgress = (elapsed / duration.toFloat()).coerceIn(0f, 1f)
                if (emergencyProgress >= 1f) {
                    onDismissComplete()
                    break
                }
                delay(30)
            }
        } else {
            emergencyProgress = 0f
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WakeUpBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Section: Time, Label, and Sequence Progress
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Pulsing Alarm Icon
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .scale(pulseScale)
                    .shadow(4.dp, CircleShape, spotColor = WakeUpButtonShadow)
                    .clip(CircleShape)
                    .background(WakeUpCrimson)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = "Ringing Alarm",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large Current Time
            Text(
                text = currentTimeStr,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = WakeUpTextPrimary,
                    fontSize = 48.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Alarm Label
            Text(
                text = label.ifBlank { "Wake Up Routine" },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = WakeUpTextPrimary
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sequence Pill
            if (maxSequence > 1) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(WakeUpCrimsonContainer)
                        .border(1.dp, WakeUpCrimsonBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Alarm ${sequenceIndex + 1} of $maxSequence in sequence",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = WakeUpCrimson
                        )
                    )
                }
            }
        }

        // Middle Section: Dismissal Challenge Flow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when (challengeType) {
                ChallengeType.MATHS.name -> {
                    MathsChallengeCard(onSolved = onDismissComplete)
                }
                ChallengeType.QR_CODE.name -> {
                    QrCodeChallengeCard(onSolved = onDismissComplete)
                }
                else -> {
                    StandardDismissCard(onDismiss = onDismissComplete)
                }
            }
        }

        // Bottom Section: Routine Guarantee & Emergency Deliberate Bypass
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Solving dismisses today's remaining alarms without disabling your future scheduled routine.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = WakeUpTextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Emergency Hold-to-Dismiss Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(WakeUpSurface)
                    .border(1.dp, WakeUpDivider, RoundedCornerShape(14.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isEmergencyHolding = true
                                tryAwaitRelease()
                                isEmergencyHolding = false
                            }
                        )
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                // Progress Fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth(emergencyProgress)
                        .height(48.dp)
                        .background(WakeUpCrimsonContainer)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = WakeUpTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEmergencyHolding) "Hold for 3s to emergency dismiss..." else "Hold for Emergency Bypass",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (isEmergencyHolding) WakeUpCrimson else WakeUpTextSecondary
                        )
                    )
                }
            }
        }
    }
}

/**
 * Real, functional Maths Challenge with arithmetic generation and numeric keypad
 */
@Composable
private fun MathsChallengeCard(
    onSolved: () -> Unit
) {
    // Generate arithmetic problem
    var num1 by remember { mutableIntStateOf(Random.nextInt(12, 45)) }
    var num2 by remember { mutableIntStateOf(Random.nextInt(11, 39)) }
    val isAddition = remember { Random.nextBoolean() }
    val expectedAnswer = remember(num1, num2) { if (isAddition) num1 + num2 else num1 - num2 }

    var userAnswerInput by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    TactileCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 3.dp,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = null,
                    tint = WakeUpCrimson,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MATHS WAKE-UP CHALLENGE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = WakeUpCrimson
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Arithmetic Question Display
            val operator = if (isAddition) "+" else "-"
            Text(
                text = "$num1 $operator $num2 = ?",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = WakeUpTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // User Answer Box
            Box(
                modifier = Modifier
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .size(width = 160.dp, height = 50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(WakeUpSurfaceSecondary)
                    .border(
                        1.5.dp,
                        if (hasError) WakeUpCrimson else if (isSuccess) Color(0xFF2E7D32) else WakeUpDivider,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (userAnswerInput.isEmpty()) "Enter answer" else userAnswerInput,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (userAnswerInput.isEmpty()) WakeUpTextTertiary else WakeUpTextPrimary
                    )
                )
            }

            if (hasError) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Incorrect, try again!",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = WakeUpCrimson,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dedicated Numeric Keypad (1-9, 0, Backspace, Submit)
            val keypad = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "OK")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                keypad.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { key ->
                            KeypadButton(
                                text = key,
                                isPrimary = key == "OK",
                                onClick = {
                                    hasError = false
                                    when (key) {
                                        "C" -> {
                                            if (userAnswerInput.isNotEmpty()) {
                                                userAnswerInput = userAnswerInput.dropLast(1)
                                            }
                                        }
                                        "OK" -> {
                                            val parsed = userAnswerInput.toIntOrNull()
                                            if (parsed == expectedAnswer) {
                                                isSuccess = true
                                                coroutineScope.launch {
                                                    delay(300)
                                                    onSolved()
                                                }
                                            } else {
                                                hasError = true
                                                coroutineScope.launch {
                                                    // Trigger shake animation
                                                    repeat(3) {
                                                        shakeOffset.animateTo(15f, tween(50))
                                                        shakeOffset.animateTo(-15f, tween(50))
                                                    }
                                                    shakeOffset.animateTo(0f, tween(50))
                                                }
                                            }
                                        }
                                        else -> {
                                            if (userAnswerInput.length < 5) {
                                                userAnswerInput += key
                                            }
                                        }
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
private fun KeypadButton(
    text: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = if (isPrimary) WakeUpCrimson else WakeUpSurfaceSecondary
    val textColor = if (isPrimary) Color.White else WakeUpTextPrimary
    val borderColor = if (isPrimary) WakeUpCrimsonPressed else WakeUpDivider

    Box(
        modifier = Modifier
            .size(width = 68.dp, height = 44.dp)
            .shadow(
                elevation = if (isPrimary) 2.dp else 1.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = if (isPrimary) WakeUpButtonShadow else WakeUpCardShadow
            )
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("keypad_$text"),
        contentAlignment = Alignment.Center
    ) {
        if (text == "C") {
            Icon(
                imageVector = Icons.Default.Backspace,
                contentDescription = "Backspace",
                tint = WakeUpTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            )
        }
    }
}

@Composable
private fun StandardDismissCard(
    onDismiss: () -> Unit
) {
    TactileCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 3.dp,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Time to start your day",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = WakeUpTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap below to silence this alarm and complete today's waking sequence.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = WakeUpTextSecondary,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            TactileCrimsonButton(
                text = "I'm Awake — Dismiss Alarm",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                testTag = "standard_dismiss_button"
            )
        }
    }
}

@Composable
private fun QrCodeChallengeCard(
    onSolved: () -> Unit
) {
    TactileCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 3.dp,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = WakeUpCrimson,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "QR Code Challenge (Prototype)",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = WakeUpTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Place a QR code in your bathroom or kitchen. In prototype mode, confirm below to simulate a successful barcode scan.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = WakeUpTextSecondary,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            TactileCrimsonButton(
                text = "Simulate QR Scan & Dismiss",
                onClick = onSolved,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
