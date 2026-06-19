package com.drummer.speed.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.drummer.speed.R
import com.drummer.speed.data.model.SessionResult
import com.drummer.speed.ui.components.BestSessionCard
import com.drummer.speed.ui.components.StatCard
import com.drummer.speed.util.AudioConfig
import com.drummer.speed.util.DateFormatter
import com.drummer.speed.util.TimeFormatter
import java.util.Locale

@Composable
fun StatisticsPage(history: List<SessionResult>) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val locale = remember(config) { java.util.Locale.forLanguageTag(config.locales[0].language) }

    if (history.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.BarChart,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.no_statistics),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val totalSessions = history.size
    val totalStrokes = history.sumOf { it.strokes }
    val totalDuration = history.sumOf { it.duration }
    val avgStrokes = if (totalSessions > 0) totalStrokes.toFloat() / totalSessions else 0f
    val avgSpeed = if (totalDuration > 0) totalStrokes.toFloat() / totalDuration else 0f
    val bestSession = history.maxByOrNull { it.strokes }
    val bestSpeed = history.maxByOrNull { if (it.duration > 0) it.strokes.toFloat() / it.duration else 0f }

    // Bar chart data (last 7 sessions)
    val recentSessions = history.sortedBy { it.timestamp }.takeLast(AudioConfig.BAR_CHART_SESSIONS)
    val maxStrokes = recentSessions.maxOfOrNull { it.strokes } ?: 1

    // Animation
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(history) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(AudioConfig.BAR_CHART_ANIMATION_DURATION_MS, easing = FastOutSlowInEasing)
        )
    }

    // Capture colors outside Canvas for non-composable context
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.FitnessCenter,
                label = stringResource(R.string.total_strokes),
                value = totalStrokes.toString(),
                color = MaterialTheme.colorScheme.primary
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Speed,
                label = stringResource(R.string.avg_speed),
                value = String.format("%.1f/s", avgSpeed),
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Timer,
                label = stringResource(R.string.total_time),
                value = TimeFormatter.format(totalDuration, context),
                color = MaterialTheme.colorScheme.tertiary
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Repeat,
                label = stringResource(R.string.sessions),
                value = totalSessions.toString(),
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(24.dp))

        // Bar Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.recent_sessions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                // Bar chart with labels
                val barCount = recentSessions.size
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Value labels row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        recentSessions.forEach { result ->
                            Text(
                                text = result.strokes.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Bars
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val chartWidth = size.width
                            val chartHeight = size.height
                            val totalSpacing = chartWidth * 0.15f
                            val spacing = totalSpacing / (barCount + 1)
                            val barWidth = (chartWidth - totalSpacing) / barCount

                            // Grid lines
                            for (i in 0..AudioConfig.BAR_CHART_GRID_LINES) {
                                val y = chartHeight * (1f - i.toFloat() / AudioConfig.BAR_CHART_GRID_LINES)
                                drawLine(
                                    color = Color.Gray.copy(alpha = 0.12f),
                                    start = Offset(0f, y),
                                    end = Offset(chartWidth, y),
                                    strokeWidth = 1f
                                )
                            }

                            // Bars
                            recentSessions.forEachIndexed { index, result ->
                                val barHeight = (result.strokes.toFloat() / maxStrokes) * chartHeight * animationProgress.value
                                val x = spacing + index * (barWidth + spacing)
                                val y = chartHeight - barHeight

                                drawRoundRect(
                                    color = primaryColor,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, barHeight.coerceAtLeast(AudioConfig.BAR_CHART_MIN_HEIGHT)),
                                    cornerRadius = CornerRadius(6f, 6f)
                                )
                            }
                        }
                    }

                    // Date labels row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        recentSessions.forEach { result ->
                            Text(
                                text = DateFormatter.formatShortDate(result.timestamp, locale),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Best Sessions
        Text(
            text = stringResource(R.string.best_sessions),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        if (bestSession != null) {
            BestSessionCard(
                icon = Icons.Default.EmojiEvents,
                label = stringResource(R.string.most_strokes),
                result = bestSession,
                context = context
            )
        }

        if (bestSpeed != null) {
            Spacer(Modifier.height(8.dp))
            BestSessionCard(
                icon = Icons.Default.FlashOn,
                label = stringResource(R.string.fastest_speed),
                result = bestSpeed,
                context = context
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}
