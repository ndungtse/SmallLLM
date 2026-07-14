@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.smallllm.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smallllm.data.download.DownloadStatus
import java.util.Locale

/**
 * Shared download-state UI for both the gallery cards and the model-detail screen — the two
 * used to duplicate this DownloadStatus state machine (NotStarted / Downloading / Completed /
 * Failed). Rendering (expressive buttons + a wavy progress indicator) now lives in one place.
 *
 * @param fullWidth stretch the primary buttons to fill their parent (detail-screen layout).
 * @param onOpenChat when non-null (detail screen), the Completed state offers an "Open chat"
 *   primary action; when null (gallery), it shows a compact "Downloaded" label instead.
 */
@Composable
fun DownloadActions(
    status: DownloadStatus,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = false,
    onOpenChat: (() -> Unit)? = null,
) {
    val buttonModifier = if (fullWidth) Modifier.fillMaxWidth() else Modifier
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (status) {
            is DownloadStatus.NotStarted ->
                Button(onClick = onDownload, modifier = buttonModifier) { Text("Download") }

            is DownloadStatus.Downloading -> {
                if (status.percent >= 0) {
                    LinearWavyProgressIndicator(
                        progress = { status.percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val pct = if (status.percent >= 0) "${status.percent}%" else "…"
                    Text(
                        "$pct  ·  ${formatRate(status.bytesPerSecond)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(onClick = onCancel) { Text("Cancel") }
                }
            }

            is DownloadStatus.Completed -> {
                if (onOpenChat != null) {
                    Button(onClick = onOpenChat, modifier = buttonModifier) { Text("Open chat") }
                    OutlinedButton(onClick = onDelete, modifier = buttonModifier) { Text("Delete") }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Downloaded", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.weight(1f))
                        OutlinedButton(onClick = onDelete) { Text("Delete") }
                    }
                }
            }

            is DownloadStatus.Failed -> {
                Text(
                    "Failed: ${status.message}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
                Button(onClick = onDownload, modifier = buttonModifier) { Text("Retry") }
            }
        }
    }
}

private fun formatRate(bytesPerSecond: Long): String {
    if (bytesPerSecond <= 0) return "—"
    val mb = bytesPerSecond / (1024.0 * 1024.0)
    return if (mb >= 1) String.format(Locale.US, "%.1f MB/s", mb)
    else String.format(Locale.US, "%.0f KB/s", bytesPerSecond / 1024.0)
}
