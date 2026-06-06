package com.smallllm.data.model

import com.google.gson.annotations.SerializedName

/** Sampling / generation defaults for a model. Overridable per chat session later. */
data class SamplingConfig(
    @SerializedName("topK") val topK: Int = 40,
    @SerializedName("topP") val topP: Float = 0.95f,
    @SerializedName("temperature") val temperature: Float = 0.8f,
    @SerializedName("maxTokens") val maxTokens: Int = 1024,
)

/**
 * One entry in the model registry (allowlist). Describes a downloadable on-device model: where to
 * get it, how big it is, which runtime loads it, and its default generation config.
 *
 * Mirrors (a trimmed version of) the AI Edge Gallery's `AllowedModel`, parsed from
 * `assets/model_registry.json` by [ModelRegistry].
 */
data class ModelSpec(
    @SerializedName("name") val name: String,
    @SerializedName("modelId") val modelId: String,
    @SerializedName("description") val description: String,
    @SerializedName("fileName") val fileName: String,
    @SerializedName("version") val version: String,
    @SerializedName("downloadUrl") val downloadUrl: String,
    @SerializedName("sizeInBytes") val sizeInBytes: Long,
    @SerializedName("params") val params: String,
    @SerializedName("contextLength") val contextLength: Int,
    @SerializedName("runtime") val runtime: RuntimeType,
    @SerializedName("supportsTools") val supportsTools: Boolean = false,
    @SerializedName("requiresAccessToken") val requiresAccessToken: Boolean = false,
    @SerializedName("minDeviceMemoryInGb") val minDeviceMemoryInGb: Int? = null,
    @SerializedName("sampling") val sampling: SamplingConfig = SamplingConfig(),
) {
    /** Filesystem-safe identifier derived from [name], used as the storage directory name. */
    val normalizedName: String
        get() = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
}
