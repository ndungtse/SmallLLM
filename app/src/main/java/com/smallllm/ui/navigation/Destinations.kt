package com.smallllm.ui.navigation

import android.net.Uri

/** Navigation routes for the app: Gallery → Model detail → Chat. */
object Destinations {
    const val GALLERY = "gallery"

    const val MODEL_NAME_ARG = "modelName"
    const val DETAIL = "detail/{$MODEL_NAME_ARG}"
    const val CHAT = "chat/{$MODEL_NAME_ARG}"

    fun detail(modelName: String) = "detail/${Uri.encode(modelName)}"
    fun chat(modelName: String) = "chat/${Uri.encode(modelName)}"
}
