package com.drummer.speed.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.drummer.speed.R
import com.drummer.speed.data.model.SessionResult
import com.drummer.speed.ui.components.HistoryItem

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
    onDeleteSelected: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onDelete: (SessionResult) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Selection mode bar
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Select all checkbox
                    Checkbox(
                        checked = selectedIds.size == history.size,
                        onCheckedChange = {
                            if (selectedIds.size == history.size) onClearSelection()
                            else onSelectAll()
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // Selected count
                    Text(
                        text = "${selectedIds.size} ${stringResource(R.string.selected)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )

                    // Delete selected button (icon only)
                    IconButton(
                        onClick = onDeleteSelected,
                        enabled = selectedIds.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_all),
                            tint = if (selectedIds.isNotEmpty())
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    // Close selection button (icon only)
                    IconButton(onClick = onClearSelection) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                }
            }
        }

        // Normal header
        AnimatedVisibility(
            visible = !isSelectionMode,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.history_tab),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (history.isNotEmpty()) {
                        Text(
                            text = "${history.size} ${stringResource(R.string.sessions).lowercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Sort button
                Box {
                    FilledTonalIconButton(
                        onClick = { onSortMenuToggle(true) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Sort, stringResource(R.string.sort_latest))
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { onSortMenuToggle(false) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_latest)) },
                            onClick = { onSortTypeChange("latest"); onSortMenuToggle(false) },
                            leadingIcon = { Icon(Icons.Default.Schedule, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_oldest)) },
                            onClick = { onSortTypeChange("oldest"); onSortMenuToggle(false) },
                            leadingIcon = { Icon(Icons.Default.History, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sort_most)) },
                            onClick = { onSortTypeChange("most"); onSortMenuToggle(false) },
                            leadingIcon = { Icon(Icons.Default.TrendingUp, null) }
                        )
                    }
                }
            }
        }

        // Content
        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_history),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
            ) {
                items(sortedHistory, key = { it.id }) { result ->
                    HistoryItem(
                        result = result,
                        isSelected = selectedIds.contains(result.id),
                        isSelectionMode = isSelectionMode,
                        isRevealed = revealedItemId == result.id,
                        onToggleSelection = { onToggleSelection(result.id) },
                        onDelete = { onDelete(result) },
                        onRevealChange = { revealed ->
                            onRevealedItemChange(if (revealed) result.id else null)
                        }
                    )
                }
            }
        }
    }
}
