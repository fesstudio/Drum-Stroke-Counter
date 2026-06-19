package com.drummer.speed.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drummer.speed.R
import com.drummer.speed.ui.components.CompactSettingRow
import com.drummer.speed.util.TimeFormatter
import com.drummer.speed.viewmodel.DrumViewModel

@Composable
fun PracticePage(
    viewModel: DrumViewModel,
    focusManager: FocusManager
) {
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current
    val context = LocalContext.current

    DisposableEffect(uiState.isRunning) {
        if (uiState.isRunning) {
            (view.context as? Activity)?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            (view.context as? Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            (view.context as? Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val animatedStrokeCount by animateIntAsState(
        targetValue = uiState.strokeCount,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "strokeCount"
    )

    val timerLimit = uiState.timerInput.toIntOrNull() ?: 30
    val progress = if (timerLimit > 0) uiState.timeLeft.toFloat() / timerLimit else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = LinearEasing),
        label = "timerProgress"
    )

    if (uiState.isRunning) {
        RunningPracticeView(
            animatedStrokeCount = animatedStrokeCount,
            timeLeft = uiState.timeLeft,
            animatedProgress = animatedProgress,
            onStop = { viewModel.stopPractice() }
        )
    } else {
        IdlePracticeView(
            animatedStrokeCount = animatedStrokeCount,
            uiState = uiState,
            viewModel = viewModel,
            focusManager = focusManager,
            context = context
        )
    }
}

@Composable
private fun RunningPracticeView(
    animatedStrokeCount: Int,
    timeLeft: Int,
    animatedProgress: Float,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .padding(24.dp)
        ) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 10.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                strokeCap = StrokeCap.Round
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.strokes).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = animatedStrokeCount.toString(),
                    fontSize = 100.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.graphicsLayer { }
                )
                Text(
                    text = TimeFormatter.format(timeLeft, LocalContext.current),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStop,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(Icons.Default.Stop, null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.stop).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun IdlePracticeView(
    animatedStrokeCount: Int,
    uiState: com.drummer.speed.viewmodel.PracticeUiState,
    viewModel: DrumViewModel,
    focusManager: FocusManager,
    context: android.content.Context
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Section (Counter)
        Box(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = animatedStrokeCount.toString(),
                    fontSize = 110.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.graphicsLayer { }
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "${stringResource(R.string.time)}: ${TimeFormatter.format(uiState.timeLeft, context)}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Compact Settings Card
        Card(
            modifier = Modifier
                .weight(2.2f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Duration
                CompactSettingRow(
                    icon = Icons.Default.Timer,
                    label = stringResource(R.string.duration),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.incrementTimer(-5) }, modifier = Modifier.size(32.dp)) {
                            Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        BasicTextField(
                            value = uiState.timerInput,
                            onValueChange = { viewModel.updateTimer(it) },
                            modifier = Modifier.width(45.dp),
                            textStyle = TextStyle(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                        )

                        Text("s", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                        IconButton(onClick = { viewModel.incrementTimer(5) }, modifier = Modifier.size(32.dp)) {
                            Text("+", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Metronome
                CompactSettingRow(
                    icon = Icons.Default.MusicNote,
                    label = stringResource(R.string.metronome),
                    color = MaterialTheme.colorScheme.secondary
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.isMetronomeEnabled) {
                            IconButton(onClick = { viewModel.incrementBpm(-5) }, modifier = Modifier.size(32.dp)) {
                                Text("-", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.secondary)
                            }

                            BasicTextField(
                                value = uiState.bpmInput,
                                onValueChange = { viewModel.updateBpm(it) },
                                modifier = Modifier.width(45.dp),
                                textStyle = TextStyle(
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                singleLine = true,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary)
                            )

                            Text("BPM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                            IconButton(onClick = { viewModel.incrementBpm(5) }, modifier = Modifier.size(32.dp)) {
                                Text("+", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        Switch(
                            checked = uiState.isMetronomeEnabled,
                            onCheckedChange = { viewModel.toggleMetronome(it) },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Sensitivity
                Column {
                    CompactSettingRow(
                        icon = Icons.Default.Tune,
                        label = stringResource(R.string.sensitivity),
                        color = MaterialTheme.colorScheme.tertiary
                    ) {
                        Button(
                            onClick = { viewModel.startSmartCalibration() },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text(stringResource(R.string.calibrate).uppercase(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = uiState.sensitivity,
                            onValueChange = { viewModel.updateSensitivity(it) },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.tertiary, activeTrackColor = MaterialTheme.colorScheme.tertiary)
                        )
                        Text(
                            text = "${uiState.sensitivityInput}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .weight(0.8f)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.resetPractice() },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                enabled = !uiState.isCountingDown
            ) {
                Text(
                    text = stringResource(R.string.reset).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    viewModel.startPractice(goText = context.getString(R.string.go)) { _ -> }
                },
                modifier = Modifier
                    .weight(2f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                enabled = !uiState.isCountingDown
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.start).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
