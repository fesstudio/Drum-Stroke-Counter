package com.drummer.speed.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.drummer.speed.R
import com.drummer.speed.data.model.SessionResult
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryItem(
    result: SessionResult,
    strokesLabel: String,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val locale = LocalContext.current.resources.configuration.locales[0]
    val sdf = remember(locale) { SimpleDateFormat("dd/MM/yy HH:mm", locale) }
    val dateString = remember(result.timestamp, locale) { sdf.format(Date(result.timestamp)) }
    
    val secLabel = stringResource(R.string.sec)
    val durationString = remember(result.duration, locale, secLabel) {
        val totalSeconds = result.duration
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        if (minutes > 0) String.format(locale, "%d:%02d", minutes, seconds) else "${seconds}${secLabel}"
    }
    
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer {
                // Pre-render card layer for smoother scrolling
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(text = dateString, fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "($durationString)", fontSize = 12.sp, color = Color.Gray)
                    result.bpm?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "• BPM: $it", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Text(text = "${result.strokes} $strokesLabel", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}
