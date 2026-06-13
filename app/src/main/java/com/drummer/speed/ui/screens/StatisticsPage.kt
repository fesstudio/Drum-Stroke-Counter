package com.drummer.speed.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drummer.speed.R
import com.drummer.speed.data.model.SessionResult
import java.util.*

@Composable
fun StatisticsPage(
    history: List<SessionResult>
) {
    val totalStrokes = remember(history) { history.sumOf { it.strokes } }
    val avgStrokes = remember(history) { if (history.isEmpty()) 0 else totalStrokes / history.size }
    val maxStrokes = remember(history) { history.maxOfOrNull { it.strokes } ?: 0 }
    val totalSessions = history.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.statistics_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        if (history.isEmpty()) {
            item {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            item {
                AnimatedBarChart(history = history)
                Spacer(modifier = Modifier.height(16.dp))
            }
            item {
                StatCard(label = stringResource(R.string.total_strokes), value = totalStrokes.toString())
                StatCard(label = stringResource(R.string.avg_strokes), value = avgStrokes.toString())
                StatCard(label = stringResource(R.string.max_strokes), value = maxStrokes.toString())
                StatCard(label = stringResource(R.string.total_sessions), value = totalSessions.toString())
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun AnimatedBarChart(history: List<SessionResult>) {
    val lastSessions = remember(history) {
        history.take(7).reversed()
    }
    val maxInChart = remember(lastSessions) {
        (lastSessions.maxOfOrNull { it.strokes } ?: 0).coerceAtLeast(1)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.stats_trend),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    lastSessions.forEach { result ->
                        val barHeightFactor = result.strokes.toFloat() / maxInChart
                        val animatedHeight = remember { Animatable(0f) }
                        
                        LaunchedEffect(result.id) {
                            animatedHeight.animateTo(
                                targetValue = barHeightFactor,
                                animationSpec = tween(durationMillis = 1000)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(animatedHeight.value.coerceAtLeast(0.01f))
                                        .fillMaxWidth(0.6f)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.strokes.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontSize = 14.sp)
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}
