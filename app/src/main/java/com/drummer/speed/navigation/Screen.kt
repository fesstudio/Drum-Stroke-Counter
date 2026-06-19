package com.drummer.speed.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import com.drummer.speed.R

sealed class Screen(val route: String, val labelRes: Int, val icon: ImageVector) {
    data object Practice : Screen("practice", R.string.practice, Icons.Default.Timer)
    data object Statistics : Screen("statistics", R.string.statistics_tab, Icons.Default.BarChart)
    data object History : Screen("history", R.string.history_tab, Icons.Default.History)
}
