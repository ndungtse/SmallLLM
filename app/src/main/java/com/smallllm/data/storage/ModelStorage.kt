package com.smallllm.data.storage

import android.content.Context
import com.smallllm.data.model.ModelSpec
import java.io.File

/**
 * Resolves on-disk locations for downloaded models and answers "is it here yet?".
 *
 * Layout: `{externalFilesDir}/models/{normalizedName}/{version}/{fileName}`. Versioning by
 * commit-hash means a new model version downloads alongside the old one instead of clobbering it.
 */
class ModelStorage(private val context: Context) {

    private val modelsRoot: File
        get() = File(context.getExternalFilesDir(null), "models")

    /** Directory holding a model's files for its specific version. */
    fun modelDir(spec: ModelSpec): File =
        File(modelsRoot, "${spec.normalizedName}/${spec.version}")

    /** The fully-downloaded model file. */
    fun modelFile(spec: ModelSpec): File = File(modelDir(spec), spec.fileName)

    /** Partial-download scratch file the [com.smallllm.data.download.DownloadWorker] writes to. */
    fun tmpFile(spec: ModelSpec): File = File(modelDir(spec), "${spec.fileName}.tmp")

    /** True once the final file exists and is non-empty. */
    fun isDownloaded(spec: ModelSpec): Boolean = modelFile(spec).let { it.exists() && it.length() > 0 }

    /** Absolute path of the model file (for handing to a runtime). */
    fun modelPath(spec: ModelSpec): String = modelFile(spec).absolutePath

    /** Removes all files for this model version. Returns true if anything was deleted. */
    fun delete(spec: ModelSpec): Boolean = modelDir(spec).deleteRecursively()
}
