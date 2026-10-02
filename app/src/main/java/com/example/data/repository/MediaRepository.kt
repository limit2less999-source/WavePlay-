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
}
