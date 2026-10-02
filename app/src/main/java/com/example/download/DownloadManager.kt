package com.example.download

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import com.example.data.local.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val fileName: String,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val isFinished: Boolean = false,
    val error: String? = null,
    val isVideo: Boolean = false,
    val localFilePath: String? = null
)

class MediaDownloader(
    private val context: Context,
    private val onTrackDownloaded: suspend (TrackEntity) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _activeDownloads = MutableStateFlow<List<DownloadTask>>(emptyList())
    val activeDownloads = _activeDownloads.asStateFlow()

    private val ongoingCalls = ConcurrentHashMap<String, Call>()

    suspend fun startDownload(
        url: String,
        customName: String? = null,
        forceVideo: Boolean? = null
    ): Result<TrackEntity> = withContext(Dispatchers.IO) {
        val trimmedUrl = url.trim()
        if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
            return@withContext Result.failure(IllegalArgumentException("Invalid URL: Must start with http:// or https://"))
        }

        val taskId = UUID.randomUUID().toString()
        val rawFileName = getFileNameFromUrl(trimmedUrl)
        val initialName = if (!customName.isNullOrBlank()) customName.trim() else rawFileName
        val isVideo = forceVideo ?: isVideoUrl(trimmedUrl, null)

        val task = DownloadTask(
            id = taskId,
            url = trimmedUrl,
            fileName = initialName,
            isVideo = isVideo
        )

        _activeDownloads.value = listOf(task) + _activeDownloads.value

        try {
            val request = Request.Builder()
                .url(trimmedUrl)
                .header("User-Agent", "AuraTune-Downloader/1.0 (Android)")
                .build()

            val call = client.newCall(request)
            ongoingCalls[taskId] = call
            val response = call.execute()

            if (!response.isSuccessful) {
                val errorMsg = "HTTP error ${response.code}: ${response.message}"
                updateTaskError(taskId, errorMsg)
                ongoingCalls.remove(taskId)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val body = response.body ?: run {
                val errorMsg = "Response body is empty"
                updateTaskError(taskId, errorMsg)
                ongoingCalls.remove(taskId)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val contentType = response.header("Content-Type") ?: ""
            val isActuallyVideo = forceVideo ?: (isVideo || contentType.contains("video", ignoreCase = true))

            // Save directly to device storage directory (Music or Movies)
            val externalDir = context.getExternalFilesDir(
                if (isActuallyVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_MUSIC
            )
            val targetDir = externalDir ?: File(
                context.filesDir,
                if (isActuallyVideo) "downloaded_videos" else "downloaded_music"
            )
            targetDir.mkdirs()

            val fileExt = determineExtension(trimmedUrl, contentType, isActuallyVideo)
            val baseSanitizedName = sanitizeFileName(initialName.substringBeforeLast('.'))
            val outputFile = File(targetDir, "${baseSanitizedName}_${System.currentTimeMillis()}.$fileExt")

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var lastUpdate = System.currentTimeMillis()
                    var bytesSinceLastUpdate = 0L
                    var speed = 0L

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        bytesSinceLastUpdate += read

                        val now = System.currentTimeMillis()
                        val diff = now - lastUpdate
                        if (diff >= 300 || downloadedBytes == totalBytes) {
                            if (diff > 0) {
                                speed = (bytesSinceLastUpdate * 1000) / diff
                            }
                            lastUpdate = now
                            bytesSinceLastUpdate = 0L
                            val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0.5f
                            updateTaskProgress(taskId, progress, downloadedBytes, totalBytes, speed)
                        }
                    }
                    output.flush()
                }
            }

            ongoingCalls.remove(taskId)

            var durationMs = 0L
            var title = baseSanitizedName.replace('_', ' ').replace('-', ' ').trim()
            var artist = "Direct Download"

            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(outputFile.absolutePath)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMs = durationStr?.toLongOrNull() ?: 0L
                val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                if (!metaTitle.isNullOrBlank()) title = metaTitle.trim()
                if (!metaArtist.isNullOrBlank()) artist = metaArtist.trim()
                retriever.release()
            } catch (_: Exception) {
                // Ignore metadata fallback
            }

            val track = TrackEntity(
                id = UUID.randomUUID().toString(),
                title = title.ifBlank { "Offline Track" },
                artist = artist,
                album = if (isActuallyVideo) "Downloaded Videos" else "Downloaded Audio",
                durationMs = durationMs,
                mediaUri = outputFile.absolutePath,
                isVideo = isActuallyVideo,
                fileSizeBytes = outputFile.length(),
                sourceUrl = trimmedUrl
            )

            // Save to Room database
            onTrackDownloaded(track)

            // Mark task finished
            updateTaskSuccess(taskId, outputFile.length(), outputFile.absolutePath)

            Result.success(track)
        } catch (e: Exception) {
            ongoingCalls.remove(taskId)
            updateTaskError(taskId, e.localizedMessage ?: "Download failed")
            Result.failure(e)
        }
    }

    private fun updateTaskProgress(id: String, progress: Float, downloaded: Long, total: Long, speed: Long) {
        _activeDownloads.value = _activeDownloads.value.map {
            if (it.id == id) it.copy(
                progress = progress,
                downloadedBytes = downloaded,
                totalBytes = total,
                speedBytesPerSec = speed
            ) else it
        }
    }

    private fun updateTaskSuccess(id: String, finalSize: Long, path: String) {
        _activeDownloads.value = _activeDownloads.value.map {
            if (it.id == id) it.copy(
                isFinished = true,
                progress = 1f,
                downloadedBytes = finalSize,
                totalBytes = finalSize,
                speedBytesPerSec = 0L,
                localFilePath = path
            ) else it
        }
    }

    private fun updateTaskError(id: String, error: String) {
        _activeDownloads.value = _activeDownloads.value.map {
            if (it.id == id) it.copy(
                isFinished = true,
                error = error,
                speedBytesPerSec = 0L
            ) else it
        }
    }

    fun cancelTask(id: String) {
        ongoingCalls[id]?.cancel()
        ongoingCalls.remove(id)
        updateTaskError(id, "Cancelled by user")
    }

    fun dismissTask(id: String) {
        cancelTask(id)
        _activeDownloads.value = _activeDownloads.value.filterNot { it.id == id }
    }

    private fun getFileNameFromUrl(url: String): String {
        return try {
            val path = Uri.parse(url).lastPathSegment ?: "direct_download"
            URLDecoder.decode(path, "UTF-8").substringBefore('?')
        } catch (_: Exception) {
            "media_${System.currentTimeMillis()}"
        }
    }

    private fun isVideoUrl(url: String, contentType: String?): Boolean {
        val lower = url.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") ||
                lower.endsWith(".mov") || lower.endsWith(".3gp") ||
                (contentType?.contains("video", ignoreCase = true) == true)
    }

    private fun determineExtension(url: String, contentType: String, isVideo: Boolean): String {
        val lower = url.lowercase().substringBefore('?')
        when {
            lower.endsWith(".mp3") -> return "mp3"
            lower.endsWith(".m4a") -> return "m4a"
            lower.endsWith(".wav") -> return "wav"
            lower.endsWith(".aac") -> return "aac"
            lower.endsWith(".flac") -> return "flac"
            lower.endsWith(".ogg") -> return "ogg"
            lower.endsWith(".mp4") -> return "mp4"
            lower.endsWith(".mkv") -> return "mkv"
            lower.endsWith(".webm") -> return "webm"
        }
        return if (isVideo) "mp4" else "mp3"
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(40)
    }
}
