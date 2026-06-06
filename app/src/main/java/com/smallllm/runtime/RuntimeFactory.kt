package com.smallllm.runtime

import android.content.Context
import com.smallllm.data.model.RuntimeType
import com.smallllm.runtime.litertlm.LiteRtLmRuntime
import com.smallllm.runtime.llamacpp.LlamaCppRuntime

/**
 * Creates the right [LlmRuntime] for a model's [RuntimeType]. The single place that knows about
 * concrete engines, so the rest of the app stays engine-agnostic. Each call returns a fresh runtime
 * instance (one model loaded at a time).
 */
class RuntimeFactory(private val context: Context) {
    fun create(type: RuntimeType): LlmRuntime = when (type) {
        RuntimeType.LITERT_LM -> LiteRtLmRuntime(context.applicationContext)
        RuntimeType.LLAMA_CPP -> LlamaCppRuntime()
    }
}
