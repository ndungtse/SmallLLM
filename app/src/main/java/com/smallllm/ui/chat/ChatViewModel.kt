package com.smallllm.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smallllm.SmallLlmApplication
import com.smallllm.di.AppContainer
import com.smallllm.runtime.LlmRuntime
import com.smallllm.runtime.RuntimeOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Role { USER, ASSISTANT }

data class ChatMessage(val role: Role, val text: String)

sealed interface LoadState {
    data object Loading : LoadState
    data object Ready : LoadState
    data object NotDownloaded : LoadState
    data class Error(val message: String) : LoadState
}

data class ChatUiState(
    val modelName: String = "",
    val loadState: LoadState = LoadState.Loading,
    val messages: List<ChatMessage> = emptyList(),
    val isGenerating: Boolean = false,
)

/** Drives one chat session: loads the model into its runtime and streams responses. */
class ChatViewModel(
    private val container: AppContainer,
    private val modelName: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(modelName = modelName))
    val uiState = _uiState.asStateFlow()

    private var runtime: LlmRuntime? = null
    private var generateJob: Job? = null

    init {
        viewModelScope.launch {
            val spec = container.modelRegistry.findByName(modelName)
            if (spec == null) {
                _uiState.update { it.copy(loadState = LoadState.Error("Unknown model")) }
                return@launch
            }
            if (!container.modelStorage.isDownloaded(spec)) {
                _uiState.update { it.copy(loadState = LoadState.NotDownloaded) }
                return@launch
            }
            try {
                val rt = container.runtimeFactory.create(spec.runtime)
                rt.initialize(
                    modelPath = container.modelStorage.modelPath(spec),
                    options = RuntimeOptions(
                        topK = spec.sampling.topK,
                        topP = spec.sampling.topP,
                        temperature = spec.sampling.temperature,
                        maxTokens = spec.sampling.maxTokens,
                    ),
                )
                runtime = rt
                _uiState.update { it.copy(loadState = LoadState.Ready) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Throwable, not Exception: native model loads can fail with Errors
                // (e.g. UnsatisfiedLinkError) that must not crash the app.
                _uiState.update { it.copy(loadState = LoadState.Error(e.message ?: "Failed to load model")) }
            }
        }
    }

    fun send(text: String) {
        val prompt = text.trim()
        val rt = runtime ?: return
        if (prompt.isEmpty() || _uiState.value.isGenerating) return

        _uiState.update {
            it.copy(
                messages = it.messages + ChatMessage(Role.USER, prompt) + ChatMessage(Role.ASSISTANT, ""),
                isGenerating = true,
            )
        }

        generateJob = viewModelScope.launch {
            rt.generate(prompt)
                .onCompletion { _uiState.update { s -> s.copy(isGenerating = false) } }
                .collect { chunk -> appendToLastAssistant(chunk) }
        }
    }

    fun stop() {
        viewModelScope.launch { runtime?.stop() }
        generateJob?.cancel()
        _uiState.update { it.copy(isGenerating = false) }
    }

    fun reset() {
        generateJob?.cancel()
        viewModelScope.launch { runtime?.resetConversation() }
        _uiState.update { it.copy(messages = emptyList(), isGenerating = false) }
    }

    private fun appendToLastAssistant(chunk: String) {
        _uiState.update { state ->
            val messages = state.messages.toMutableList()
            val lastIndex = messages.indexOfLast { it.role == Role.ASSISTANT }
            if (lastIndex >= 0) {
                val current = messages[lastIndex]
                messages[lastIndex] = current.copy(text = current.text + chunk)
            }
            state.copy(messages = messages)
        }
    }

    override fun onCleared() {
        generateJob?.cancel()
        runtime?.close()
        runtime = null
    }

    companion object {
        fun provideFactory(modelName: String) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SmallLlmApplication
                ChatViewModel(app.container, modelName)
            }
        }
    }
}
