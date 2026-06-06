package com.smallllm.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smallllm.data.download.DownloadStatus
import com.smallllm.data.model.ModelSpec

@OptIn(ExperimentalMaterial3Api::class)
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
                    TextButton(onClick = onBack) { Text("Back") }
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (spec == null) {
                Text("Loading…")
                return@Column
            }
            Text(spec.description, style = MaterialTheme.typography.bodyMedium)
            SpecRow("Model ID", spec.modelId)
            SpecRow("Parameters", spec.params)
            SpecRow("Context", "${spec.contextLength} tokens")
            SpecRow("Runtime", spec.runtime.name)
            SpecRow("Tool calling", if (spec.supportsTools) "Yes" else "No")
            SpecRow("File", spec.fileName)

            DetailActions(
                spec = spec,
                status = state.status,
                onDownload = viewModel::download,
                onCancel = viewModel::cancel,
                onDelete = viewModel::delete,
                onOpenChat = { onOpenChat(spec.name) },
            )
        }
    }
}

@Composable
private fun DetailActions(
    spec: ModelSpec,
    status: DownloadStatus,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onOpenChat: () -> Unit,
) {
    when (status) {
        is DownloadStatus.NotStarted ->
            Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) { Text("Download") }

        is DownloadStatus.Downloading -> {
            if (status.percent >= 0) {
                LinearProgressIndicator(progress = { status.percent / 100f }, modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }

        is DownloadStatus.Completed -> {
            Button(onClick = onOpenChat, modifier = Modifier.fillMaxWidth()) { Text("Open chat") }
            OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Delete") }
        }

        is DownloadStatus.Failed -> {
            Text("Failed: ${status.message}", color = MaterialTheme.colorScheme.error)
            Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) { Text("Retry") }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
