package com.drummer.speed.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import com.drummer.speed.R

@Composable
fun PracticePage(
    isRunning: Boolean,
    strokeCount: Int,
    timeLeft: Int,
    timerSeconds: Int,
    timerInput: String,
    onTimerInputChange: (String) -> Unit,
    onTimerIncrement: (Int) -> Unit,
    isMetronomeEnabled: Boolean,
    onMetronomeToggle: (Boolean) -> Unit,
    bpmInput: String,
    onBpmInputChange: (String) -> Unit,
    onBpmIncrement: (Int) -> Unit,
    sensitivity: Float,
    onSensitivityChange: (Float) -> Unit,
    sensitivityInput: String,
    onSensitivityInputChange: (String) -> Unit,
    onStartCalibration: () -> Unit,
    isCountingDown: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    formatTime: (Int) -> String,
    focusManager: FocusManager
) {
    if (isRunning) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = stringResource(R.string.title), fontSize = 24.sp, fontWeight = FontWeight.Medium)
            Text(
                text = strokeCount.toString(), 
                fontSize = 150.sp, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.graphicsLayer {
                    // Pre-render optimization for large text changes
                }
            )
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = formatTime(timeLeft), 
                fontSize = 60.sp, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.graphicsLayer {
                    // Isolation for high frequency updates
                }
            )
            Spacer(modifier = Modifier.height(64.dp))
            Button(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth(0.6f).height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.stop), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            item(span = { GridItemSpan(1) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = stringResource(R.string.title), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(
                        text = strokeCount.toString(), 
                        fontSize = 80.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.graphicsLayer { }
                    )
                    Text(
                        text = "${stringResource(R.string.time)}: ${formatTime(timeLeft)}", 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.graphicsLayer { }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.duration), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.weight(1f))
                            if (timeLeft == timerSeconds) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { onTimerIncrement(-5) }, modifier = Modifier.size(32.dp)) {
                                        Text("-5", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedTextField(
                                        value = timerInput,
                                        onValueChange = onTimerInputChange,
                                        modifier = Modifier.width(70.dp).height(50.dp),
                                        textStyle = TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                        singleLine = true,
                                        shape = MaterialTheme.shapes.small
                                    )
                                    IconButton(onClick = { onTimerIncrement(5) }, modifier = Modifier.size(32.dp)) {
                                        Text("+5", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Text(text = formatTime(timerSeconds), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.metronome), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            
                            if (isMetronomeEnabled) {
                                Spacer(Modifier.weight(1f))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { onBpmIncrement(-5) }, modifier = Modifier.size(24.dp)) { Text("-", fontWeight = FontWeight.Bold) }
                                    OutlinedTextField(
                                        value = bpmInput,
                                        onValueChange = { 
                                            onBpmInputChange(it)
                                        },
                                        modifier = Modifier.width(70.dp).height(50.dp),
                                        textStyle = TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                        singleLine = true,
                                        shape = MaterialTheme.shapes.small
                                    )
                                    IconButton(onClick = { onBpmIncrement(5) }, modifier = Modifier.size(24.dp)) { Text("+", fontWeight = FontWeight.Bold) }
                                }
                                Spacer(Modifier.weight(1f))
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                            
                            Switch(checked = isMetronomeEnabled, onCheckedChange = onMetronomeToggle, modifier = Modifier.scale(0.8f))
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(text = stringResource(R.string.sensitivity), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = onStartCalibration) {
                                Text(stringResource(R.string.calibrate), fontSize = 12.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = sensitivityInput,
                                onValueChange = onSensitivityInputChange,
                                modifier = Modifier.width(60.dp).height(50.dp),
                                textStyle = TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                singleLine = true,
                                shape = MaterialTheme.shapes.small
                            )
                            Text(text = " %", fontSize = 13.sp)
                        }
                        Slider(value = sensitivity, onValueChange = onSensitivityChange, modifier = Modifier.height(24.dp).padding(top = 8.dp))
                    }
                }
            }

            item(span = { GridItemSpan(1) }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onStart,
                            modifier = Modifier.weight(1f).height(50.dp),
                            enabled = !isCountingDown
                        ) { Text(if (isRunning) stringResource(R.string.stop) else stringResource(R.string.start)) }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(onClick = onReset, modifier = Modifier.weight(1f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), enabled = !isCountingDown) { Text(stringResource(R.string.reset)) }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
