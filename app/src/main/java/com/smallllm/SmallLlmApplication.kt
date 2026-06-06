package com.smallllm

import android.app.Application
import com.smallllm.di.AppContainer

/**
 * Application entry point. Owns the app-wide [AppContainer] (manual DI) that ViewModels read through
 * the application instance.
 */
class SmallLlmApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
