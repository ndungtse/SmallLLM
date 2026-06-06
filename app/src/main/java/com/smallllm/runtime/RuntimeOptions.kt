package com.smallllm.runtime

/** Hardware backend an [LlmRuntime] should prefer, when it supports a choice. */
enum class Backend { CPU, GPU, NPU }

/**
 * Engine-agnostic generation settings handed to [LlmRuntime.initialize]. Runtimes map these onto
 * their own config types and ignore anything they don't support.
 */
data class RuntimeOptions(
    val backend: Backend = Backend.CPU,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    val temperature: Float = 0.8f,
    val maxTokens: Int = 1024,
    val systemPrompt: String? = null,
)
