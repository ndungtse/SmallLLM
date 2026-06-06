package com.smallllm.data.model

/**
 * Which on-device inference engine loads a given model. Each value maps to one
 * [com.smallllm.runtime.LlmRuntime] implementation via [com.smallllm.runtime.RuntimeFactory].
 */
enum class RuntimeType {
    /** Google LiteRT-LM — `.litertlm` / `.task` models (e.g. Qwen3, FunctionGemma). */
    LITERT_LM,

    /** llama.cpp via JNI — GGUF models (e.g. SmolLM2-135M). */
    LLAMA_CPP,
}
