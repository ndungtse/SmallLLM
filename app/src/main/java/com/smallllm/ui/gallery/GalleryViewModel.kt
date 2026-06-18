package com.smallllm.ui.gallery

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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GalleryUiState(
    val models: List<ModelSpec> = emptyList(),
    val statuses: Map<String, DownloadStatus> = emptyMap(),
    val loading: Boolean = true,
)

/** Lists registry models and tracks each one's download status. */
class GalleryViewModel(private val container: AppContainer) : ViewModel() {

    private val models = MutableStateFlow<List<ModelSpec>>(emptyList())
    private val statuses = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    private val loading = MutableStateFlow(true)

    val uiState = combine(models, statuses, loading) { m, s, l -> GalleryUiState(m, s, l) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GalleryUiState())

    init {
        viewModelScope.launch {
            val list = container.modelRegistry.models()
            models.value = list
            loading.value = false
            list.forEach { spec ->
                launch {
                    container.downloadRepository.statusFlow(spec).collect { status ->
                        statuses.update { it + (spec.name to status) }
                    }
                }
            }
        }
    }

    fun download(spec: ModelSpec) =
        container.downloadRepository.download(
            spec,
            accessToken = if (spec.requiresAccessToken) container.hfAccessToken else null,
        )

    fun cancel(spec: ModelSpec) = container.downloadRepository.cancel(spec)

    fun delete(spec: ModelSpec) {
        container.modelStorage.delete(spec)
        statuses.update { it + (spec.name to DownloadStatus.NotStarted) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as SmallLlmApplication
                GalleryViewModel(app.container)
            }
        }
    }
}
