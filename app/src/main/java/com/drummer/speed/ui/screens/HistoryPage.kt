package com.drummer.speed.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drummer.speed.data.model.SessionResult
import com.drummer.speed.ui.components.HistoryItem

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import com.drummer.speed.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryPage(
    history: List<SessionResult>,
    sortedHistory: List<SessionResult>,
    selectedIds: List<String>,
    isSelectionMode: Boolean,
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(R.string.history), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    if (allSelected) onClearSelection()
                                    else onSelectAll()
                                }
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.select_all),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = { checked ->
                                    if (checked) onSelectAll() else onClearSelection()
                                }
                            )
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
        if (history.isEmpty()) {
            item { Text(stringResource(R.string.no_data), color = Color.Gray, modifier = Modifier.padding(16.dp)) }
        } else {
            items(sortedHistory, key = { it.id }) { result ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart) {
                            itemToDelete = result
                            false // Do not dismiss yet, show dialog first
                        } else false
                    }
                )

                LaunchedEffect(itemToDelete) {
                    if (itemToDelete == null) {
                        dismissState.reset()
                    }
                }

                val isSelected = selectedIds.contains(result.id)
                
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        val color by remember {
                            derivedStateOf {
                                when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.EndToStart -> Color.Red
                                    else -> Color.Transparent
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 4.dp)
                                .graphicsLayer { 
                                    // Optimization for GPU layer
                                }
                                .background(color, CardDefaults.shape),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White,
                                    modifier = Modifier.padding(end = 16.dp)
                                )
                            }
                        }
                    }
                ) {
                    HistoryItem(
                        result = result,
                        strokesLabel = stringResource(R.string.strokes),
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onLongClick = {
                            if (!isSelected) onToggleSelection(result.id)
                        },
                        onClick = {
                            if (isSelectionMode) {
                                onToggleSelection(result.id)
                            }
                        }
                    )
                }
            }
        }
    }
}
