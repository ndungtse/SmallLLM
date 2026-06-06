package com.smallllm.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads a single model file in the background, surviving app death, with a foreground
 * notification and resume-from-partial support.
 *
 * Streams to a `.tmp` file, resuming via an HTTP `Range` header when a partial file already exists,
 * then renames to the final file on success. Progress is reported through [setProgress] (throttled
 * to ~200ms) and a progress notification. Distilled from the AI Edge Gallery's `DownloadWorker`.
 */
class DownloadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val notificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationId = id.hashCode()

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model downloads",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Progress for on-device model downloads" }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = inputData.getString(KEY_URL) ?: return@withContext fail("Missing URL")
        val modelName = inputData.getString(KEY_MODEL_NAME) ?: "model"
        val destDirPath = inputData.getString(KEY_DEST_DIR) ?: return@withContext fail("Missing dir")
        val fileName = inputData.getString(KEY_FILE_NAME) ?: return@withContext fail("Missing file")
        val declaredTotal = inputData.getLong(KEY_TOTAL_BYTES, 0L)
        val accessToken = inputData.getString(KEY_ACCESS_TOKEN)

        val destDir = File(destDirPath).apply { mkdirs() }
        val finalFile = File(destDir, fileName)
        if (finalFile.exists() && finalFile.length() > 0) return@withContext Result.success()
        val tmpFile = File(destDir, "$fileName.tmp")

        try {
            setForeground(foregroundInfo(modelName, 0))

            val existing = if (tmpFile.exists()) tmpFile.length() else 0L
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 30_000
                readTimeout = 30_000
                if (accessToken != null) setRequestProperty("Authorization", "Bearer $accessToken")
                if (existing > 0) {
                    setRequestProperty("Range", "bytes=$existing-")
                    // Force identity encoding so byte offsets match for resume.
                    setRequestProperty("Accept-Encoding", "identity")
                }
                connect()
            }

            val code = connection.responseCode
            if (code !in 200..299) {
                connection.disconnect()
                return@withContext fail("HTTP $code")
            }
            val resumed = code == HttpURLConnection.HTTP_PARTIAL
            val startByte = if (resumed) existing else 0L
            val remaining = connection.contentLengthLong
            val totalBytes = when {
                remaining > 0 -> startByte + remaining
                declaredTotal > 0 -> declaredTotal
                else -> -1L
            }

            RandomAccessFile(tmpFile, "rw").use { raf ->
                if (resumed) raf.seek(startByte) else raf.setLength(0)
                connection.inputStream.use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var downloaded = startByte
                    var lastReportTs = 0L
                    var lastReportBytes = downloaded
                    while (true) {
                        if (isStopped) return@withContext Result.failure()
                        val read = input.read(buffer)
                        if (read < 0) break
                        raf.write(buffer, 0, read)
                        downloaded += read

                        val now = System.currentTimeMillis()
                        if (now - lastReportTs > PROGRESS_INTERVAL_MS) {
                            val rate = if (lastReportTs == 0L) 0L
                            else (downloaded - lastReportBytes) * 1000 / (now - lastReportTs)
                            val percent =
                                if (totalBytes > 0) (downloaded * 100 / totalBytes).toInt() else -1
                            setProgress(progressData(downloaded, totalBytes, percent, rate))
                            setForeground(foregroundInfo(modelName, percent))
                            lastReportTs = now
                            lastReportBytes = downloaded
                        }
                    }
                }
            }
            connection.disconnect()

            if (!tmpFile.renameTo(finalFile)) {
                tmpFile.copyTo(finalFile, overwrite = true)
                tmpFile.delete()
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for $modelName", e)
            fail(e.message ?: "Download error")
        }
    }

    private fun fail(message: String): Result =
        Result.failure(Data.Builder().putString(KEY_ERROR, message).build())

    private fun progressData(received: Long, total: Long, percent: Int, rate: Long): Data =
        Data.Builder()
            .putLong(KEY_RECEIVED_BYTES, received)
            .putLong(KEY_TOTAL_BYTES, total)
            .putInt(KEY_PERCENT, percent)
            .putLong(KEY_RATE, rate)
            .build()

    private fun foregroundInfo(modelName: String, percent: Int): ForegroundInfo {
        val intent = Intent(applicationContext, Class.forName("com.smallllm.MainActivity"))
            .apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val indeterminate = percent < 0
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Downloading \"$modelName\"")
            .setContentText(if (indeterminate) "Downloading…" else "$percent%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, percent.coerceAtLeast(0), indeterminate)
            .setContentIntent(pendingIntent)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    companion object {
        private const val TAG = "DownloadWorker"
        private const val CHANNEL_ID = "model_download"
        private const val BUFFER_SIZE = 8 * 1024
        private const val PROGRESS_INTERVAL_MS = 200L

        const val KEY_URL = "url"
        const val KEY_MODEL_NAME = "model_name"
        const val KEY_DEST_DIR = "dest_dir"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_ACCESS_TOKEN = "access_token"

        const val KEY_RECEIVED_BYTES = "received_bytes"
        const val KEY_PERCENT = "percent"
        const val KEY_RATE = "rate"
        const val KEY_ERROR = "error"
    }
}
