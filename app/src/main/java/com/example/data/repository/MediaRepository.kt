package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.ai.GeminiMusicService
import com.example.data.local.AppDatabase
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRef
import com.example.data.local.TrackEntity
import com.example.download.MediaDownloader
import com.example.util.SampleAudioGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MediaRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val trackDao = db.trackDao()
    private val playlistDao = db.playlistDao()

    val audioTracks: Flow<List<TrackEntity>> = trackDao.getAllAudioTracks()
    val videoTracks: Flow<List<TrackEntity>> = trackDao.getAllVideoTracks()
    val favoriteTracks: Flow<List<TrackEntity>> = trackDao.getFavoriteTracks()
    val playlists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    val downloader = MediaDownloader(context) { downloadedTrack ->
        trackDao.insertTrack(downloadedTrack)
    }

    val geminiMusicService = GeminiMusicService(context) { generatedTrack ->
        trackDao.insertTrack(generatedTrack)
    }

    suspend fun initializeOfflineSamplesIfEmpty() = withContext(Dispatchers.IO) {
        val count = trackDao.getAudioTrackCount()
        if (count == 0) {
            val initialTracks = SampleAudioGenerator.generateInitialTracksIfEmpty(context)
            trackDao.insertTracks(initialTracks)

            // Create initial default playlist
            val playlistId = playlistDao.insertPlaylist(
                PlaylistEntity(
                    name = "Chill Vibes",
                    description = "Smooth offline lo-fi and electronic tracks",
                    coverColorHex = 0xFF8B5CF6
                )
            )

            initialTracks.forEachIndexed { index, track ->
                playlistDao.insertTrackToPlaylist(
                    PlaylistTrackCrossRef(
                        playlistId = playlistId,
                        trackId = track.id,
                        orderIndex = index
                    )
                )
            }
        }
    }

    suspend fun toggleFavorite(trackId: String, currentFav: Boolean) = withContext(Dispatchers.IO) {
        trackDao.updateFavorite(trackId, !currentFav)
    }

    suspend fun updateTracksOrder(orderedTracks: List<TrackEntity>) = withContext(Dispatchers.IO) {
        val updated = orderedTracks.mapIndexed { index, track ->
            track.copy(orderIndex = index)
        }
        trackDao.updateTracks(updated)
    }

    suspend fun updateTrackTitleAndCover(trackId: String, newTitle: String, newCoverColor: Long) = withContext(Dispatchers.IO) {
        trackDao.updateTitleAndCover(trackId, newTitle.trim(), newCoverColor)
    }

    suspend fun deleteTrack(track: TrackEntity) = withContext(Dispatchers.IO) {
        trackDao.deleteTrackById(track.id)
        try {
            val file = File(track.mediaUri)
            if (file.exists() && file.isFile) {
                file.delete()
            }
        } catch (_: Exception) {}
    }

    suspend fun createPlaylist(name: String, description: String, colorHex: Long): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(
            PlaylistEntity(
                name = name.trim(),
                description = description.trim(),
                coverColorHex = colorHex
            )
        )
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.clearPlaylistTracks(playlistId)
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, trackId: String) = withContext(Dispatchers.IO) {
        val currentCount = playlistDao.getTrackCountForPlaylist(playlistId)
        playlistDao.insertTrackToPlaylist(
            PlaylistTrackCrossRef(
                playlistId = playlistId,
                trackId = trackId,
                orderIndex = currentCount
            )
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    fun getTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>> {
        return playlistDao.getTracksForPlaylist(playlistId)
    }

    suspend fun importLocalMedia(uri: Uri, isVideo: Boolean, displayName: String?): TrackEntity = withContext(Dispatchers.IO) {
        val targetSubDir = if (isVideo) "imported_videos" else "imported_music"
        val dir = File(context.filesDir, targetSubDir).apply { mkdirs() }
        val ext = if (isVideo) "mp4" else "mp3"
        val safeName = (displayName ?: "imported_${System.currentTimeMillis()}").replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val destinationFile = File(dir, "${safeName}_${System.currentTimeMillis()}.$ext")

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        }

        val track = TrackEntity(
            id = UUID.randomUUID().toString(),
            title = displayName?.substringBeforeLast('.') ?: "Imported Media",
            artist = "Local Device",
            album = if (isVideo) "Imported Videos" else "Imported Music",
            durationMs = 0L,
            mediaUri = destinationFile.absolutePath,
            isVideo = isVideo,
            fileSizeBytes = destinationFile.length()
        )
        trackDao.insertTrack(track)
        track
    }

    suspend fun scanDeviceMedia(): Int = withContext(Dispatchers.IO) {
        val audios = com.example.util.MediaStoreScanner.scanDeviceAudios(context)
        val videos = com.example.util.MediaStoreScanner.scanDeviceVideos(context)
        if (audios.isNotEmpty()) {
            trackDao.insertTracks(audios)
        }
        if (videos.isNotEmpty()) {
            trackDao.insertTracks(videos)
        }
        audios.size + videos.size
    }

    val vaultItems: Flow<List<com.example.data.local.VaultItemEntity>> = db.vaultDao().getAllVaultItems()

    private fun getPrivateVaultDir(): File {
        val dir = File(context.filesDir, "safe_vault_encrypted").apply { mkdirs() }
        val noMedia = File(dir, ".nomedia")
        if (!noMedia.exists()) {
            try { noMedia.createNewFile() } catch (_: Exception) {}
        }
        return dir
    }

    suspend fun hideTrackToVault(track: TrackEntity, type: String): Unit = withContext(Dispatchers.IO) {
        val vaultDir = getPrivateVaultDir()
        val originalUriOrPath = track.mediaUri
        val ext = if (track.isVideo) "mp4" else "mp3"
        val hiddenVaultFile = File(vaultDir, "vault_${UUID.randomUUID()}.$ext")

        var resolvedFilePath: String? = null
        var bytesCopied = false

        // 1. Resolve raw file path if it's a MediaStore URI
        if (originalUriOrPath.startsWith("content://")) {
            val uri = Uri.parse(originalUriOrPath)
            try {
                val proj = arrayOf(android.provider.MediaStore.MediaColumns.DATA)
                context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                        if (idx != -1) {
                            resolvedFilePath = cursor.getString(idx)
                        }
                    }
                }
            } catch (_: Exception) {}

            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(hiddenVaultFile).use { output ->
                        input.copyTo(output)
                        bytesCopied = true
                    }
                }
            } catch (_: Exception) {}

            // Remove from MediaStore
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (_: Exception) {}
        } else {
            resolvedFilePath = originalUriOrPath
            val origFile = File(originalUriOrPath)
            if (origFile.exists()) {
                try {
                    origFile.inputStream().use { input ->
                        FileOutputStream(hiddenVaultFile).use { output ->
                            input.copyTo(output)
                            bytesCopied = true
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // 2. Erase / purge original physical file from local storage so File Manager & Google Files cannot see it
        resolvedFilePath?.let { path ->
            try {
                val origFile = File(path)
                if (origFile.exists()) {
                    val deleted = origFile.delete()
                    if (!deleted && origFile.exists()) {
                        // If file delete is locked by scoped storage, rename to hidden file with dot prefix
                        try {
                            val hiddenDot = File(origFile.parentFile, ".hidden_${origFile.name}")
                            origFile.renameTo(hiddenDot)
                        } catch (_: Exception) {}
                    }
                    // Notify Android MediaScanner so Files by Google and File Managers immediately remove it
                    android.media.MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
                }
            } catch (_: Exception) {}
        }

        val vaultPath = if (bytesCopied && hiddenVaultFile.exists() && hiddenVaultFile.length() > 0) {
            hiddenVaultFile.absolutePath
        } else {
            track.mediaUri
        }

        val vaultItem = com.example.data.local.VaultItemEntity(
            id = track.id,
            title = track.title,
            mediaUri = vaultPath,
            mediaType = type,
            sizeBytes = if (hiddenVaultFile.exists()) hiddenVaultFile.length() else track.fileSizeBytes,
            durationMs = track.durationMs,
            originalArtist = track.artist
        )
        db.vaultDao().insertVaultItem(vaultItem)
        trackDao.deleteTrackById(track.id)
    }

    suspend fun hideMediaUriToVault(uriStr: String, title: String, type: String, sizeBytes: Long = 0L, durationMs: Long = 0L): Unit = withContext(Dispatchers.IO) {
        val vaultDir = getPrivateVaultDir()
        val ext = when (type) {
            "VIDEO" -> "mp4"
            "PHOTO" -> "jpg"
            else -> "mp3"
        }
        val hiddenVaultFile = File(vaultDir, "vault_${UUID.randomUUID()}.$ext")
        var bytesCopied = false
        var resolvedFilePath: String? = null

        if (uriStr.startsWith("content://")) {
            val uri = Uri.parse(uriStr)
            try {
                val proj = arrayOf(android.provider.MediaStore.MediaColumns.DATA)
                context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                        if (idx != -1) {
                            resolvedFilePath = cursor.getString(idx)
                        }
                    }
                }
            } catch (_: Exception) {}

            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(hiddenVaultFile).use { output ->
                        input.copyTo(output)
                        bytesCopied = true
                    }
                }
            } catch (_: Exception) {}

            try {
                context.contentResolver.delete(uri, null, null)
            } catch (_: Exception) {}
        } else {
            resolvedFilePath = uriStr
            val origFile = File(uriStr)
            if (origFile.exists()) {
                try {
                    origFile.inputStream().use { input ->
                        FileOutputStream(hiddenVaultFile).use { output ->
                            input.copyTo(output)
                            bytesCopied = true
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        resolvedFilePath?.let { path ->
            try {
                val origFile = File(path)
                if (origFile.exists()) {
                    origFile.delete()
                    android.media.MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
                }
            } catch (_: Exception) {}
        }

        val vaultPath = if (bytesCopied && hiddenVaultFile.exists()) hiddenVaultFile.absolutePath else uriStr
        val vaultItem = com.example.data.local.VaultItemEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            mediaUri = vaultPath,
            mediaType = type,
            sizeBytes = if (hiddenVaultFile.exists()) hiddenVaultFile.length() else sizeBytes,
            durationMs = durationMs
        )
        db.vaultDao().insertVaultItem(vaultItem)
    }

    suspend fun restoreVaultItem(item: com.example.data.local.VaultItemEntity): Unit = withContext(Dispatchers.IO) {
        val vaultFile = File(item.mediaUri)
        var restoredPath = item.mediaUri

        if (vaultFile.exists() && vaultFile.absolutePath.startsWith(context.filesDir.absolutePath)) {
            val publicDir = when (item.mediaType) {
                "VIDEO" -> context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES)
                    ?: android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES)
                "PHOTO" -> context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
                    ?: android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                else -> context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)
                    ?: android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC)
            }
            publicDir?.mkdirs()
            val ext = when (item.mediaType) {
                "VIDEO" -> "mp4"
                "PHOTO" -> "jpg"
                else -> "mp3"
            }
            val sanitized = item.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val publicFile = File(publicDir, "${sanitized}_restored.$ext")

            try {
                vaultFile.inputStream().use { input ->
                    FileOutputStream(publicFile).use { output ->
                        input.copyTo(output)
                    }
                }
                restoredPath = publicFile.absolutePath
                vaultFile.delete()
                android.media.MediaScannerConnection.scanFile(context, arrayOf(publicFile.absolutePath), null, null)
            } catch (_: Exception) {}
        }

        if (item.mediaType == "AUDIO" || item.mediaType == "VIDEO") {
            val restoredTrack = TrackEntity(
                id = item.id,
                title = item.title,
                artist = if (item.originalArtist.isNotBlank()) item.originalArtist else "Restored Media",
                album = "Restored from Safe Vault",
                durationMs = item.durationMs,
                mediaUri = restoredPath,
                isVideo = item.mediaType == "VIDEO",
                fileSizeBytes = item.sizeBytes
            )
            trackDao.insertTrack(restoredTrack)
        }
        db.vaultDao().deleteVaultItem(item)
    }

    suspend fun deleteVaultItemPermanently(item: com.example.data.local.VaultItemEntity): Unit = withContext(Dispatchers.IO) {
        db.vaultDao().deleteVaultItem(item)
        try {
            val f = File(item.mediaUri)
            if (f.exists()) f.delete()
        } catch (_: Exception) {}
    }
}
