@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.smallllm.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ChatScreen(
    modelName: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel(factory = ChatViewModel.provideFactory(modelName)),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(state.modelName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::reset) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reset conversation")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val load = state.loadState) {
                is LoadState.Loading ->
                    LoadingState("Loading model… (first load can take several seconds)")

                is LoadState.NotDownloaded ->
                    CenterText("This model isn't downloaded yet. Go back and download it first.")

                is LoadState.Error ->
                    CenterText("Error: ${load.message}")

                is LoadState.Ready -> {
                    MessageList(state.messages, Modifier.weight(1f))
                    // A wavy line signals the model is actively streaming a reply.
                    if (state.isGenerating) {
                        LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    InputBar(
                        enabled = !state.isGenerating,
                        isGenerating = state.isGenerating,
                        onSend = viewModel::send,
                        onStop = viewModel::stop,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageList(messages: List<ChatMessage>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, messages.lastOrNull()?.text) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // animateItem() rides the theme's expressive spring, so new/streaming bubbles
        // settle into place with natural motion instead of snapping.
        items(messages) { message ->
            MessageBubble(message, Modifier.animateItem())
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val isUser = message.role == Role.USER
    val color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    // Asymmetric shapes give each side a distinct "tail" corner, a hallmark of the
    // expressive shape language.
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 6.dp)
    } else {
        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 6.dp, bottomEnd = 22.dp)
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = color,
            contentColor = textColor,
            shape = shape,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Text(
                text = message.text.ifEmpty { "…" },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun InputBar(
    enabled: Boolean,
    isGenerating: Boolean,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message") },
            shape = RoundedCornerShape(26.dp),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Send),
        )
        if (isGenerating) {
            FilledIconButton(
                onClick = onStop,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Icon(Icons.Filled.Stop, contentDescription = "Stop generating")
            }
        } else {
            FilledIconButton(
                onClick = {
                    onSend(text)
                    text = ""
                },
                enabled = enabled && text.isNotBlank(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
private fun LoadingState(text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Shape-morphing loading indicator — the signature M3 Expressive "under ~5s" component.
        LoadingIndicator(modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(20.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CenterText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.bodyLarge)
    }
}
