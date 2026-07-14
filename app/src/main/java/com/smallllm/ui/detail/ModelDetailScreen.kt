@file:OptIn(ExperimentalMaterial3Api::class)

package com.smallllm.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smallllm.ui.components.DownloadActions

@Composable
fun ModelDetailScreen(
    modelName: String,
    onBack: () -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: ModelDetailViewModel = viewModel(factory = ModelDetailViewModel.provideFactory(modelName)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.spec?.name ?: "Model") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val spec = state.spec
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (spec == null) {
                Text("Loading…")
                return@Column
            }
            Text(
                spec.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Grouped spec container — an expressive "settings list" surface.
            val rows = listOf(
                "Model ID" to spec.modelId,
                "Parameters" to spec.params,
                "Context" to "${spec.contextLength} tokens",
                "Runtime" to spec.runtime.name,
                "Tool calling" to if (spec.supportsTools) "Yes" else "No",
                "File" to spec.fileName,
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    rows.forEachIndexed { index, (label, value) ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                        SpecRow(label, value)
                    }
                }
            }

            DownloadActions(
                status = state.status,
                onDownload = viewModel::download,
                onCancel = viewModel::cancel,
                onDelete = viewModel::delete,
                fullWidth = true,
                onOpenChat = { onOpenChat(spec.name) },
            )
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
