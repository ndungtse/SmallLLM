@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.smallllm.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smallllm.data.download.DownloadStatus
import com.smallllm.data.model.ModelSpec
import com.smallllm.ui.components.DownloadActions
import java.util.Locale

@Composable
fun GalleryScreen(
    onModelClick: (String) -> Unit,
    dynamicColor: Boolean,
    onToggleDynamicColor: () -> Unit,
    viewModel: GalleryViewModel = viewModel(factory = GalleryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("SmallLLM — Models") },
                actions = {
                    IconButton(onClick = onToggleDynamicColor) {
                        Icon(
                            Icons.Filled.Palette,
                            contentDescription = if (dynamicColor) {
                                "Dynamic color on — switch to brand colors"
                            } else {
                                "Brand colors on — switch to dynamic color"
                            },
                            tint = if (dynamicColor) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
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
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(spec.name, style = MaterialTheme.typography.titleLarge)
            Text(
                spec.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(spec.params) })
                AssistChip(onClick = {}, label = { Text(formatBytes(spec.sizeInBytes)) })
                AssistChip(onClick = {}, label = { Text(spec.runtime.name) })
                if (spec.supportsTools) AssistChip(onClick = {}, label = { Text("tools") })
            }
            DownloadActions(
                status = status,
                onDownload = onDownload,
                onCancel = onCancel,
                onDelete = onDelete,
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "—"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format(Locale.US, "%.1f GB", mb / 1024)
    else String.format(Locale.US, "%.0f MB", mb)
}
