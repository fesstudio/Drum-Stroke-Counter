package com.drummer.speed.ui.components

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drummer.speed.R

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        title = { Text(stringResource(R.string.about)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(text = stringResource(R.string.about_content), fontSize = 14.sp, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${stringResource(R.string.version)} $versionName",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
fun ExitConfirmDialog(context: Context, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exit_title)) },
        text = { Text(stringResource(R.string.exit_confirm)) },
        confirmButton = {
            TextButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                Text(stringResource(R.string.yes), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.no)) } }
    )
}

@Composable
fun UpdateDialog(downloadUrl: String, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_title)) },
        text = { Text(stringResource(R.string.update_available_desc)) },
        confirmButton = {
            Button(onClick = { uriHandler.openUri(downloadUrl); onDismiss() }) {
                Text(stringResource(R.string.update_button))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_later)) } }
    )
}

@Composable
fun SelectionDeleteConfirmDialog(onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.delete_all)) },
        text = { Text(stringResource(R.string.delete_selected_confirm)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.yes), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { }
    )
}

@Composable
fun CalibrationDialog(
    step: Int,
    progress: Float,
    hits: Int,
    onDismiss: () -> Unit,
    onStart: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calibration_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val text = when (step) {
                    0 -> "Click Start to begin calibration. Make sure you are in a typical practice environment."
                    1 -> stringResource(R.string.calibration_desc_silence)
                    2 -> stringResource(R.string.calibration_desc_hits)
                    3 -> stringResource(R.string.calibration_success)
                    else -> ""
                }
                Text(text)
                Spacer(Modifier.height(16.dp))
                
                if (step == 1) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                }
                
                if (step == 2) {
                    Text(
                        text = stringResource(R.string.calibration_hits_count, hits),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    LinearProgressIndicator(progress = { hits / 5f }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            if (step == 0) {
                Button(onClick = onStart) { Text(stringResource(R.string.start_calibration)) }
            } else if (step == 3) {
                Button(onClick = onDismiss) { Text("OK") }
            }
        },
        dismissButton = {
            if (step != 1 && step != 2) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.no)) }
            }
        }
    )
}
