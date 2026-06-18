package com.smallllm.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smallllm.SmallLlmApplication
import com.smallllm.data.download.DownloadStatus
import com.smallllm.data.model.ModelSpec
import com.smallllm.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetailUiState(
    val spec: ModelSpec? = null,
    val status: DownloadStatus = DownloadStatus.NotStarted,
)

/** Shows one model's details and its download/delete controls. */
class ModelDetailViewModel(
    private val container: AppContainer,
    private val modelName: String,
) : ViewModel() {

    private val spec = MutableStateFlow<ModelSpec?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState = combine(
        spec,
        spec.flatMapLatest { s ->
            if (s == null) flowOf(DownloadStatus.NotStarted) else container.downloadRepository.statusFlow(s)
        },
    ) { s, status -> DetailUiState(s, status) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    init {
        viewModelScope.launch { spec.value = container.modelRegistry.findByName(modelName) }
    }

    fun download() = spec.value?.let {
        container.downloadRepository.download(
            it,
            accessToken = if (it.requiresAccessToken) container.hfAccessToken else null,
        )
    }
    fun cancel() = spec.value?.let { container.downloadRepository.cancel(it) }
    fun delete() = spec.value?.let { container.modelStorage.delete(it) }

    companion object {
        fun provideFactory(modelName: String) = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SmallLlmApplication
                ModelDetailViewModel(app.container, modelName)
            }
        }
    }
}
