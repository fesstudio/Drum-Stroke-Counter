package com.drummer.speed.ui.screens

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import com.drummer.speed.R
import com.drummer.speed.data.model.SessionResult
import com.drummer.speed.ui.components.ResultStat
import com.drummer.speed.utils.FileHelper

@Composable
fun ResultSummaryScreen(
    result: SessionResult,
    onClose: () -> Unit,
    formatTime: (Int) -> String
) {
    val context = LocalContext.current
    val view = LocalView.current
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text(text = stringResource(R.string.title), fontSize = 24.sp, fontWeight = FontWeight.Medium)
        Text(text = "${result.strokes}", fontSize = 120.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ResultStat(stringResource(R.string.duration), formatTime(result.duration))
            result.bpm?.let { ResultStat("BPM", it.toString()) }
        }
        Spacer(modifier = Modifier.height(40.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose, modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.back), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(24.dp))
            IconButton(onClick = {
                val bitmap = view.drawToBitmap()
                if (FileHelper.saveBitmapToGallery(context, bitmap)) {
                    Toast.makeText(context, context.getString(R.string.save_success), Toast.LENGTH_SHORT).show()
                }
            }, modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)) {
                Icon(Icons.Default.Download, contentDescription = stringResource(R.string.save), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(modifier = Modifier.width(24.dp))
            IconButton(onClick = {
                val bitmap = view.drawToBitmap()
                val file = FileHelper.saveBitmapToCache(context, bitmap)
                if (file != null) {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        setDataAndType(uri, "image/jpeg")
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, String.format(context.getString(R.string.share_caption), result.strokes))
                        clipData = ClipData.newRawUri("", uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_result)))
                }
            }, modifier = Modifier.size(56.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape)) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}
