package com.smallllm.data.model

import com.google.gson.annotations.SerializedName

/** Root of `assets/model_registry.json`: the curated list of models the app offers. */
data class ModelAllowlist(
    @SerializedName("models") val models: List<ModelSpec> = emptyList(),
)
