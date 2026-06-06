package com.smallllm.data.download

/** Observable state of a model download, surfaced to the UI. */
sealed interface DownloadStatus {

    /** Not downloaded and no download in flight. */
    data object NotStarted : DownloadStatus

    /** Download running. [percent] is -1 when total size is unknown. */
    data class Downloading(
        val receivedBytes: Long,
        val totalBytes: Long,
        val percent: Int,
        val bytesPerSecond: Long,
    ) : DownloadStatus

    /** File is fully downloaded and ready to load. */
    data object Completed : DownloadStatus

    /** Download failed; [message] is a short human-readable reason. */
    data class Failed(val message: String) : DownloadStatus
}
