package com.smallllm.runtime.litertlm

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend as LiteRtBackend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.SamplerConfig
import com.smallllm.runtime.Backend
import com.smallllm.runtime.LlmRuntime
import com.smallllm.runtime.RuntimeOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * [LlmRuntime] backed by Google LiteRT-LM (`.litertlm` / `.task` models). Wraps an [Engine] and a
 * [Conversation]; streams tokens by bridging LiteRT-LM's [MessageCallback] into a [callbackFlow].
 *
 * Modeled on the AI Edge Gallery's `LlmChatModelHelper`.
 */
class LiteRtLmRuntime(private val context: Context) : LlmRuntime {

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var conversationConfig: ConversationConfig? = null

    override suspend fun initialize(modelPath: String, options: RuntimeOptions) {
        withContext(Dispatchers.Default) {
            val backend = when (options.backend) {
                Backend.CPU -> LiteRtBackend.CPU()
                Backend.GPU -> LiteRtBackend.GPU()
                Backend.NPU -> LiteRtBackend.NPU(nativeLibraryDir = context.applicationInfo.nativeLibraryDir)
            }
            val engine = Engine(
                EngineConfig(
                    modelPath = modelPath,
                    backend = backend,
                    maxNumTokens = options.maxTokens,
                )
            )
            engine.initialize() // can take several seconds
            this@LiteRtLmRuntime.engine = engine

            val config = ConversationConfig(
                samplerConfig = SamplerConfig(
                    topK = options.topK,
                    topP = options.topP.toDouble(),
                    temperature = options.temperature.toDouble(),
                ),
                systemInstruction = options.systemPrompt?.takeIf { it.isNotBlank() }?.let { Contents.of(it) },
            )
            conversationConfig = config
            conversation = engine.createConversation(config)
        }
    }

    override fun generate(prompt: String): Flow<String> = callbackFlow {
        val conversation = conversation
            ?: throw IllegalStateException("Runtime not initialized")

        conversation.sendMessageAsync(
            Contents.of(prompt),
            object : MessageCallback {
                override fun onMessage(message: Message) {
                    trySend(message.toString())
                }

                override fun onDone() {
                    close()
                }

                override fun onError(throwable: Throwable) {
                    if (throwable is CancellationException) {
                        close()
                    } else {
                        Log.e(TAG, "Inference error", throwable)
                        close(throwable)
                    }
                }
            },
        )
        awaitClose { }
    }

    override suspend fun stop() {
        runCatching { conversation?.cancelProcess() }
    }

    override suspend fun resetConversation() {
        withContext(Dispatchers.Default) {
            val engine = engine ?: return@withContext
            val config = conversationConfig ?: return@withContext
            runCatching { conversation?.close() }
            conversation = engine.createConversation(config)
        }
    }

    override fun close() {
        runCatching { conversation?.close() }
        runCatching { engine?.close() }
        conversation = null
        engine = null
    }

    private companion object {
        const val TAG = "LiteRtLmRuntime"
    }
}
