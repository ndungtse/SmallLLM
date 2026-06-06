package com.smallllm.llamacpp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Kotlin surface for running GGUF models through llama.cpp via JNI (`libsmollm.so`, built from
 * `src/main/cpp`). Adapted from the proven SmolChat-Android `smollm` module.
 *
 * The native library is loaded lazily on first [load]; if it hasn't been built yet (NDK + CMake +
 * the llama.cpp submodule — see README.md), that call throws and the caller surfaces a friendly
 * error. Everything compiles without the `.so` because the `native` methods are only declarations.
 */
class SmolLM {

    /** Inference tuning passed to the native engine at load time. */
    data class InferenceParams(
        val minP: Float = 0.05f,
        val temperature: Float = 0.8f,
        val topK: Int = 40,
        val topP: Float = 0.95f,
        val contextSize: Long = 2048L,
        val numThreads: Int = 4,
    )

    private var handle: Long = 0L

    /** Loads a GGUF model from [modelPath]. Heavy + blocking — call off the main thread. */
    fun load(modelPath: String, params: InferenceParams = InferenceParams()) {
        ensureLibraryLoaded()
        handle = nativeLoad(
            modelPath, params.minP, params.temperature,
            params.topK, params.topP, params.contextSize, params.numThreads,
        )
    }

    fun addSystemPrompt(prompt: String) = nativeAddChatMessage(handle, prompt, "system")
    fun addUserMessage(message: String) = nativeAddChatMessage(handle, message, "user")
    fun addAssistantMessage(message: String) = nativeAddChatMessage(handle, message, "assistant")

    /** Streams generated token pieces for [query] until the model reports completion. */
    fun generate(query: String): Flow<String> = flow {
        check(handle != 0L) { "Model not loaded" }
        nativeStartCompletion(handle, query)
        while (true) {
            val piece = nativeCompletionLoop(handle) ?: break
            emit(piece)
        }
    }.flowOn(Dispatchers.Default)

    /** Requests the in-flight generation to stop after the current token. */
    fun stop() {
        if (handle != 0L) nativeStop(handle)
    }

    /** Frees the native model + context. */
    fun close() {
        if (handle != 0L) {
            nativeFree(handle)
            handle = 0L
        }
    }

    private external fun nativeLoad(
        modelPath: String, minP: Float, temperature: Float,
        topK: Int, topP: Float, contextSize: Long, numThreads: Int,
    ): Long

    private external fun nativeAddChatMessage(handle: Long, message: String, role: String)
    private external fun nativeStartCompletion(handle: Long, query: String)
    private external fun nativeCompletionLoop(handle: Long): String?
    private external fun nativeStop(handle: Long)
    private external fun nativeFree(handle: Long)

    private companion object {
        @Volatile
        private var libraryLoaded = false

        @Synchronized
        fun ensureLibraryLoaded() {
            if (!libraryLoaded) {
                System.loadLibrary("smollm")
                libraryLoaded = true
            }
        }
    }
}
