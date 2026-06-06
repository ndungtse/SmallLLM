package com.smallllm.runtime.llamacpp

import com.smallllm.llamacpp.SmolLM
import com.smallllm.runtime.LlmRuntime
import com.smallllm.runtime.RuntimeOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * [LlmRuntime] backed by llama.cpp (GGUF models) through the `:llamacpp` module's [SmolLM] JNI
 * surface. The native layer is wired in a later step; until then [SmolLM] throws and only the
 * LiteRT-LM path is usable.
 */
class LlamaCppRuntime : LlmRuntime {

    private val smolLM = SmolLM()
    private var modelPath: String? = null
    private var options: RuntimeOptions = RuntimeOptions()

    override suspend fun initialize(modelPath: String, options: RuntimeOptions) {
        this.modelPath = modelPath
        this.options = options
        withContext(Dispatchers.Default) {
            smolLM.load(modelPath, options.toInferenceParams())
            options.systemPrompt?.takeIf { it.isNotBlank() }?.let { smolLM.addSystemPrompt(it) }
        }
    }

    override fun generate(prompt: String): Flow<String> = smolLM.generate(prompt)

    override suspend fun stop() {
        runCatching { smolLM.stop() }
    }

    override suspend fun resetConversation() {
        val path = modelPath ?: return
        withContext(Dispatchers.Default) {
            smolLM.close()
            smolLM.load(path, options.toInferenceParams())
            options.systemPrompt?.takeIf { it.isNotBlank() }?.let { smolLM.addSystemPrompt(it) }
        }
    }

    override fun close() {
        runCatching { smolLM.close() }
    }

    private fun RuntimeOptions.toInferenceParams() = SmolLM.InferenceParams(
        temperature = temperature,
        topK = topK,
        topP = topP,
    )
}
