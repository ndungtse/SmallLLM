package com.smallllm.data.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.smallllm.data.model.ModelSpec
import com.smallllm.data.storage.ModelStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Starts, cancels, and observes model downloads via [WorkManager], translating raw `WorkInfo` into
 * the [DownloadStatus] the UI consumes. Each model has a unique work name so re-enqueueing replaces
 * (and resumes) rather than duplicating.
 */
class DownloadRepository(
    context: Context,
    private val storage: ModelStorage,
) {
    private val workManager = WorkManager.getInstance(context)

    fun download(spec: ModelSpec, accessToken: String? = null) {
        val inputData = Data.Builder()
            .putString(DownloadWorker.KEY_URL, spec.downloadUrl)
            .putString(DownloadWorker.KEY_MODEL_NAME, spec.name)
            .putString(DownloadWorker.KEY_DEST_DIR, storage.modelDir(spec).absolutePath)
            .putString(DownloadWorker.KEY_FILE_NAME, spec.fileName)
            .putLong(DownloadWorker.KEY_TOTAL_BYTES, spec.sizeInBytes)
            .apply { if (accessToken != null) putString(DownloadWorker.KEY_ACCESS_TOKEN, accessToken) }
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .addTag(TAG)
            .build()

        workManager.enqueueUniqueWork(uniqueName(spec), ExistingWorkPolicy.KEEP, request)
    }

    fun cancel(spec: ModelSpec) = workManager.cancelUniqueWork(uniqueName(spec))

    fun statusFlow(spec: ModelSpec): Flow<DownloadStatus> =
        workManager.getWorkInfosForUniqueWorkFlow(uniqueName(spec)).map { infos ->
            when (val state = infos.lastOrNull()?.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED ->
                    DownloadStatus.Downloading(0, spec.sizeInBytes, 0, 0)

                WorkInfo.State.RUNNING -> {
                    val p = infos.last().progress
                    DownloadStatus.Downloading(
                        receivedBytes = p.getLong(DownloadWorker.KEY_RECEIVED_BYTES, 0),
                        totalBytes = p.getLong(DownloadWorker.KEY_TOTAL_BYTES, spec.sizeInBytes),
                        percent = p.getInt(DownloadWorker.KEY_PERCENT, 0),
                        bytesPerSecond = p.getLong(DownloadWorker.KEY_RATE, 0),
                    )
                }

                WorkInfo.State.SUCCEEDED -> DownloadStatus.Completed

                WorkInfo.State.FAILED ->
                    DownloadStatus.Failed(
                        infos.last().outputData.getString(DownloadWorker.KEY_ERROR) ?: "Download failed"
                    )

                WorkInfo.State.CANCELLED, null ->
                    if (storage.isDownloaded(spec)) DownloadStatus.Completed else DownloadStatus.NotStarted
            }
        }

    private fun uniqueName(spec: ModelSpec) = "download:${spec.normalizedName}:${spec.version}"

    private companion object {
        const val TAG = "model_download"
    }
}
