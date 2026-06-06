package com.smallllm.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smallllm.data.download.DownloadStatus
import com.smallllm.data.model.ModelSpec
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onModelClick: (String) -> Unit,
    viewModel: GalleryViewModel = viewModel(factory = GalleryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("SmallLLM — Models") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.models, key = { it.name }) { spec ->
                ModelCard(
                    spec = spec,
                    status = state.statuses[spec.name] ?: DownloadStatus.NotStarted,
                    onClick = { onModelClick(spec.name) },
                    onDownload = { viewModel.download(spec) },
                    onCancel = { viewModel.cancel(spec) },
                    onDelete = { viewModel.delete(spec) },
                )
            }
        }
    }
}

@Composable
private fun ModelCard(
    spec: ModelSpec,
    status: DownloadStatus,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(spec.name, style = MaterialTheme.typography.titleMedium)
            Text(
                spec.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(spec.params) })
                AssistChip(onClick = {}, label = { Text(formatBytes(spec.sizeInBytes)) })
                AssistChip(onClick = {}, label = { Text(spec.runtime.name) })
                if (spec.supportsTools) AssistChip(onClick = {}, label = { Text("tools") })
            }
            DownloadControls(status, onDownload, onCancel, onDelete)
        }
    }
}

@Composable
private fun DownloadControls(
    status: DownloadStatus,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    when (status) {
        is DownloadStatus.NotStarted ->
            Button(onClick = onDownload) { Text("Download") }

        is DownloadStatus.Downloading -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (status.percent >= 0) {
                LinearProgressIndicator(
                    progress = { status.percent / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val pct = if (status.percent >= 0) "${status.percent}%" else "…"
                Text("$pct  ·  ${formatRate(status.bytesPerSecond)}", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
            }
        }

        is DownloadStatus.Completed -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Downloaded", style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onDelete) { Text("Delete") }
        }

        is DownloadStatus.Failed -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Failed: ${status.message}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            Button(onClick = onDownload) { Text("Retry") }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "—"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format(Locale.US, "%.1f GB", mb / 1024)
    else String.format(Locale.US, "%.0f MB", mb)
}

private fun formatRate(bytesPerSecond: Long): String {
    if (bytesPerSecond <= 0) return "—"
    val mb = bytesPerSecond / (1024.0 * 1024.0)
    return if (mb >= 1) String.format(Locale.US, "%.1f MB/s", mb)
    else String.format(Locale.US, "%.0f KB/s", bytesPerSecond / 1024.0)
}
