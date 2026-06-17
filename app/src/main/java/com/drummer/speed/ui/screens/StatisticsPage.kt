package com.drummer.speed.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import java.text.SimpleDateFormat
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drummer.speed.R
import com.drummer.speed.data.model.SessionResult
import com.drummer.speed.ui.components.EmptyStateView
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
                EmptyStateView(
                    icon = Icons.Default.BarChart,
                    message = stringResource(R.string.no_data),
                    modifier = Modifier.fillParentMaxHeight(0.7f)
                )
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
    
    val dateFormatter = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.stats_trend),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.last_7_sessions),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                // Background Grid Lines
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(4) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp)) // Space for labels
                }

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
                                // Bar with Gradient
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight(animatedHeight.value.coerceAtLeast(0.01f))
                                        .fillMaxWidth(0.5f)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                                )
                                            )
                                        )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Value Label
                            Text(
                                text = result.strokes.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            // Date Label
                            Text(
                                text = dateFormatter.format(Date(result.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
