package com.drummer.speed.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.drummer.speed.R
import com.drummer.speed.data.model.SessionResult
import com.drummer.speed.ui.screens.HistoryPage
import com.drummer.speed.ui.screens.PracticePage
import com.drummer.speed.ui.screens.StatisticsPage
import com.drummer.speed.viewmodel.DrumViewModel

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewModel: DrumViewModel,
    showResultSummary: Boolean,
    onResultSummaryChange: (Boolean) -> Unit,
    onLastResultChange: (SessionResult) -> Unit
) {
    val history by viewModel.history.collectAsState()
    val selectedIds = remember { mutableStateListOf<String>() }
    var sortType by remember { mutableStateOf("latest") }
    var showSortMenu by remember { mutableStateOf(false) }
    var revealedItemId by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Reset on Tab Change
    LaunchedEffect(currentRoute) {
        selectedIds.clear()
        revealedItemId = null
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Practice.route,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(0)) },
        exitTransition = { fadeOut(animationSpec = tween(0)) },
        popEnterTransition = { fadeIn(animationSpec = tween(0)) },
        popExitTransition = { fadeOut(animationSpec = tween(0)) }
    ) {
        composable(Screen.Practice.route) {
            PracticePage(
                viewModel = viewModel,
                focusManager = LocalFocusManager.current
            )
        }
        composable(Screen.Statistics.route) {
            StatisticsPage(history = history)
        }
        composable(Screen.History.route) {
            val sortedHistory = remember(history, sortType) {
                when (sortType) {
                    "latest" -> history.sortedByDescending { it.timestamp }
                    "oldest" -> history.sortedBy { it.timestamp }
                    "most" -> history.sortedByDescending { it.strokes }
                    else -> history
                }
            }
            HistoryPage(
                history = history,
                sortedHistory = sortedHistory,
                selectedIds = selectedIds,
                isSelectionMode = selectedIds.isNotEmpty(),
                revealedItemId = revealedItemId,
                onRevealedItemChange = { revealedItemId = it },
                showSortMenu = showSortMenu,
                onSortMenuToggle = { showSortMenu = it },
                onSortTypeChange = { sortType = it },
                onSelectAll = { selectedIds.clear(); selectedIds.addAll(history.map { it.id }) },
                onClearSelection = { selectedIds.clear() },
                onDeleteSelected = { showDeleteConfirmDialog = true },
                onToggleSelection = { id -> if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id) },
                onDelete = { viewModel.deleteResult(it) }
            )

            // Delete confirmation dialog
            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    shape = RoundedCornerShape(28.dp),
                    icon = {
                        Icon(
                            Icons.Default.Delete,
                            null,
                            modifier = Modifier.padding(top = 16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.delete_all),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.delete_selected_confirm),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showDeleteConfirmDialog = false },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(stringResource(R.string.no).uppercase())
                            }
                            Button(
                                onClick = {
                                    viewModel.deleteSelectedResults(selectedIds.toList())
                                    selectedIds.clear()
                                    showDeleteConfirmDialog = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(stringResource(R.string.yes).uppercase(), fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {}
                )
            }
        }
    }
}
