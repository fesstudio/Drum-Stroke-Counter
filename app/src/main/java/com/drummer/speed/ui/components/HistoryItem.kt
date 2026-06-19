package com.drummer.speed.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drummer.speed.R
import com.drummer.speed.data.model.SessionResult
import com.drummer.speed.util.DateFormatter
import com.drummer.speed.util.TimeFormatter
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private val SwipeThreshold = 150f
private val RevealOffsetDp = 72f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryItem(
    result: SessionResult,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isRevealed: Boolean,
    onToggleSelection: () -> Unit,
    onDelete: () -> Unit,
    onRevealChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val locale = remember(config) { java.util.Locale.forLanguageTag(config.locales[0].language) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        label = "containerColor"
    )

    val strokesPerSec = if (result.duration > 0) result.strokes.toFloat() / result.duration else 0f

    val revealOffsetPx = with(density) { RevealOffsetDp.dp.toPx() }

    // Animate offset when isRevealed or isSelectionMode changes
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(isRevealed, isSelectionMode) {
        if (isSelectionMode) {
            offsetX.snapTo(0f)
        } else {
            offsetX.animateTo(
                targetValue = if (isRevealed) -revealOffsetPx else 0f,
                animationSpec = tween(200)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        // Background delete icon (only visible area behind the card)
        if (!isSelectionMode) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                // Red square background behind the delete icon
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(52.dp)
                        .background(
                            MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            onDelete()
                            onRevealChange(false)
                        }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_all),
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Foreground card (moves with swipe)
        Card(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .then(
                    if (!isSelectionMode) {
                        Modifier.pointerInput(isSelectionMode) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        if (offsetX.value < -SwipeThreshold) {
                                            offsetX.animateTo(-revealOffsetPx)
                                            onRevealChange(true)
                                        } else if (offsetX.value < -revealOffsetPx / 2) {
                                            offsetX.animateTo(-revealOffsetPx)
                                            onRevealChange(true)
                                        } else {
                                            offsetX.animateTo(0f)
                                            onRevealChange(false)
                                        }
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    scope.launch {
                                        val newOffset = (offsetX.value + dragAmount).coerceIn(-revealOffsetPx, 0f)
                                        offsetX.snapTo(newOffset)
                                    }
                                }
                            )
                        }
                    } else Modifier
                )
                .border(
                    width = 1.dp,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    },
                    shape = RoundedCornerShape(20.dp)
                )
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) onToggleSelection()
                        else onRevealChange(!isRevealed)
                    },
                    onLongClick = {
                        if (!isSelectionMode) onToggleSelection()
                    }
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isRevealed) 4.dp else 1.dp
            )
        ) {
            Column {
                // Main content row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Selection checkbox
                    AnimatedVisibility(
                        visible = isSelectionMode,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelection() },
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }

                    // Stroke count - prominent
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            text = result.strokes.toString(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            lineHeight = 30.sp
                        )
                        Text(
                            text = stringResource(R.string.strokes).lowercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(48.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    )

                    Spacer(Modifier.width(14.dp))

                    // Details column
                    Column(modifier = Modifier.weight(1f)) {
                        // Date & Time (combined)
                        Text(
                            text = "${DateFormatter.formatDate(result.timestamp, locale)} | ${DateFormatter.formatTime(result.timestamp, locale)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(6.dp))

                        // Duration & BPM in one row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DetailChip(
                                icon = Icons.Default.Timer,
                                text = TimeFormatter.formatWithSec(result.duration, context)
                            )
                            Text(
                                text = "|",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            DetailChip(
                                icon = Icons.Default.MusicNote,
                                text = "${result.bpm ?: 0} BPM"
                            )
                        }
                    }

                    // Speed indicator
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = String.format("%.1f", strokesPerSec),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "/s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailChip(
    icon: ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
