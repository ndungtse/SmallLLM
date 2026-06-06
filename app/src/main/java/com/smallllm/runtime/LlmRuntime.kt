package com.smallllm.runtime

import kotlinx.coroutines.flow.Flow

/**
 * One interface over every on-device inference engine (LiteRT-LM, llama.cpp, …). The UI talks only to
 * this; [RuntimeFactory] picks the concrete engine from a model's [com.smallllm.data.model.RuntimeType].
 *
 * Lifecycle: [initialize] once (heavy — load the model off the main thread) → [generate] per prompt →
 * [resetConversation] to start a fresh chat → [close] to free native resources.
 */
interface LlmRuntime {

    /** Loads [modelPath] into memory with [options]. Suspends; safe to call off the main thread. */
    suspend fun initialize(modelPath: String, options: RuntimeOptions)

    /** Streams generated token chunks for [prompt]. Collection ends when the model is done. */
    fun generate(prompt: String): Flow<String>

    /** Cancels the in-flight [generate], if any. */
    suspend fun stop()

    /** Clears conversation history but keeps the model loaded. */
    suspend fun resetConversation()

    /** Releases the model and all native resources. */
    fun close()
}
