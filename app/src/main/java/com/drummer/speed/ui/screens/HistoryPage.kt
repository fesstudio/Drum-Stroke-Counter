package com.drummer.speed.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drummer.speed.R
import com.drummer.speed.data.model.SessionResult
import com.drummer.speed.ui.components.EmptyStateView
import com.drummer.speed.ui.components.HistoryItem
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryPage(
    history: List<SessionResult>,
    sortedHistory: List<SessionResult>,
    selectedIds: MutableList<String>,
    isSelectionMode: Boolean,
    revealedItemId: String?,
    onRevealedItemChange: (String?) -> Unit,
    showSortMenu: Boolean,
    onSortMenuToggle: (Boolean) -> Unit,
    onSortTypeChange: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onDelete: (SessionResult) -> Unit,
) {
    var itemToDelete by remember { mutableStateOf<SessionResult?>(null) }
    
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(stringResource(R.string.delete_all)) },
            text = { Text(stringResource(R.string.delete_single_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(itemToDelete!!)
                    itemToDelete = null
                }) {
                    Text(stringResource(R.string.yes), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { 
                    onRevealedItemChange(null)
                    if (isSelectionMode) onClearSelection()
                })
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(modifier = Modifier.height(20.dp))
                
                // Adaptive Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val title = if (isSelectionMode) {
                        stringResource(R.string.selected_count, selectedIds.size)
                    } else {
                        stringResource(R.string.history)
                    }
                    
                    Text(
                        text = title, 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold,
                        color = if (isSelectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (history.isNotEmpty() && !isSelectionMode) {
                            Box {
                                IconButton(onClick = { onSortMenuToggle(true) }) {
                                    Icon(Icons.Default.Sort, contentDescription = "Sort")
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { onSortMenuToggle(false) }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.sort_latest)) },
                                        onClick = { onSortTypeChange("latest"); onSortMenuToggle(false) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.sort_oldest)) },
                                        onClick = { onSortTypeChange("oldest"); onSortMenuToggle(false) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.sort_most)) },
                                        onClick = { onSortTypeChange("most"); onSortMenuToggle(false) }
                                    )
                                }
                            }
                        }

                        if (history.isNotEmpty() && isSelectionMode) {
                            val allSelected = selectedIds.size == history.size
                            TextButton(
                                onClick = { if (allSelected) onClearSelection() else onSelectAll() }
                            ) {
                                Text(
                                    text = if (allSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                AnimatedVisibility(
                    visible = isSelectionMode,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val allSelected = selectedIds.size == history.size
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .clickable { if (allSelected) onClearSelection() else onSelectAll() }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { if (it) onSelectAll() else onClearSelection() }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (allSelected) stringResource(R.string.deselect_all_sessions) else stringResource(R.string.select_all_sessions),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                if (!isSelectionMode) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            if (history.isEmpty()) {
                item { 
                    EmptyStateView(
                        message = stringResource(R.string.no_data),
                        modifier = Modifier.fillParentMaxHeight(0.7f)
                    )
                }
            } else {
                items(sortedHistory, key = { it.id }) { result ->
                    val offsetX = remember { Animatable(0f) }
                    val scope = rememberCoroutineScope()
                    val isRevealed by remember { derivedStateOf { offsetX.value < -10f } }

                    LaunchedEffect(revealedItemId) {
                        if (revealedItemId != result.id && offsetX.value != 0f) {
                            offsetX.animateTo(0f)
                        }
                    }
                    
                    LaunchedEffect(isSelectionMode) {
                        if (isSelectionMode && offsetX.value != 0f) {
                            offsetX.animateTo(0f)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .pointerInput(Unit) {
                                if (!isSelectionMode) {
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { change, dragAmount ->
                                            val newOffset = (offsetX.value + dragAmount).coerceIn(-180f, 0f)
                                            scope.launch { offsetX.snapTo(newOffset) }
                                            if (newOffset < -10f) onRevealedItemChange(result.id)
                                            change.consume()
                                        },
                                        onDragEnd = {
                                            scope.launch {
                                                if (offsetX.value < -80f) {
                                                    offsetX.animateTo(
                                                        targetValue = -180f,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessLow
                                                        )
                                                    )
                                                    onRevealedItemChange(result.id)
                                                } else {
                                                    offsetX.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = spring(
                                                            stiffness = Spring.StiffnessMedium
                                                        )
                                                    )
                                                    if (revealedItemId == result.id) onRevealedItemChange(null)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                            .pointerInput(isRevealed) {
                                if (isRevealed && !isSelectionMode) {
                                    detectTapGestures(onTap = {
                                        scope.launch { offsetX.animateTo(0f) }
                                        onRevealedItemChange(null)
                                    })
                                }
                            },
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        // Delete Button (Revealed Background)
                        if (!isSelectionMode) {
                            Surface(
                                onClick = { 
                                    onDelete(result)
                                    scope.launch { offsetX.snapTo(0f) }
                                    onRevealedItemChange(null)
                                },
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(42.dp),
                                shape = CircleShape,
                                color = Color.Red,
                                shadowElevation = 2.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // History Item (Foreground that slides)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                        ) {
                            val isSelected = selectedIds.contains(result.id)
                            HistoryItem(
                                result = result,
                                strokesLabel = stringResource(R.string.strokes),
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                index = sortedHistory.indexOf(result),
                                onLongClick = {
                                    if (!isSelected) onToggleSelection(result.id)
                                },
                                onClick = {
                                    if (isRevealed) {
                                        scope.launch { offsetX.animateTo(0f) }
                                        onRevealedItemChange(null)
                                    } else if (isSelectionMode) {
                                        onToggleSelection(result.id)
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
