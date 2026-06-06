package com.smallllm.data.model

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads the curated model allowlist from `assets/model_registry.json` and caches it in memory.
 *
 * Bundled-in-assets keeps the catalog available offline (this is a research/offline-first app). A
 * remote refresh with disk-cache fallback — the AI Edge Gallery's approach — can be layered on later
 * without changing callers.
 */
class ModelRegistry(
    private val context: Context,
    private val gson: Gson = Gson(),
) {
    @Volatile
    private var cached: List<ModelSpec>? = null

    /** Returns all models, parsing the assets JSON on first call. */
    suspend fun models(): List<ModelSpec> {
        cached?.let { return it }
        return withContext(Dispatchers.IO) {
            cached ?: load().also { cached = it }
        }
    }

    /** Looks up a single model by its [ModelSpec.name]. */
    suspend fun findByName(name: String): ModelSpec? = models().firstOrNull { it.name == name }

    private fun load(): List<ModelSpec> =
        try {
            val json = context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
            gson.fromJson(json, ModelAllowlist::class.java).models
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model registry from assets", e)
            emptyList()
        }

    private companion object {
        const val TAG = "ModelRegistry"
        const val ASSET_FILE = "model_registry.json"
    }
}
