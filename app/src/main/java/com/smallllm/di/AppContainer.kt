package com.smallllm.di

import android.content.Context
import com.smallllm.data.download.DownloadRepository
import com.smallllm.data.model.ModelRegistry
import com.smallllm.data.storage.ModelStorage
import com.smallllm.runtime.RuntimeFactory

/**
 * Manual dependency container (no Hilt yet — kept lean per the architecture docs). Created once by
 * [com.smallllm.SmallLlmApplication] and read by ViewModels via the application instance.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val modelRegistry: ModelRegistry by lazy { ModelRegistry(appContext) }
    val modelStorage: ModelStorage by lazy { ModelStorage(appContext) }
    val downloadRepository: DownloadRepository by lazy { DownloadRepository(appContext, modelStorage) }
    val runtimeFactory: RuntimeFactory by lazy { RuntimeFactory(appContext) }
}
