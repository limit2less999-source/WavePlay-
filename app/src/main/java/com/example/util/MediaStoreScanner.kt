package com.example.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.local.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaStoreScanner {

    private val VIBRANT_COLORS = listOf(
        0xFF8B5CF6, 0xFFEC4899, 0xFF3B82F6, 0xFF10B981, 0xFFF59E0B, 0xFFEF4444, 0xFF06B6D4
    )

    suspend fun scanDeviceAudios(context: Context): List<TrackEntity> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<TrackEntity>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID
        )

        // Filter files larger than 10KB, don't discard if duration is unindexed
        val selection = "${MediaStore.Audio.Media.SIZE} > ?"
        val selectionArgs = arrayOf("10240")
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                var index = 0
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol)
                    val title = if (rawTitle.isNullOrBlank()) "Track $id" else rawTitle
                    val rawArtist = cursor.getString(artistCol)
                    val artist = if (rawArtist.isNullOrBlank() || rawArtist.equals("<unknown>", ignoreCase = true)) "Local Audio" else rawArtist
                    val rawAlbum = cursor.getString(albumCol)
                    val album = if (rawAlbum.isNullOrBlank() || rawAlbum.equals("<unknown>", ignoreCase = true)) "Device Audio" else rawAlbum
                    var duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    // If duration is 0 or missing in MediaStore, fallback to MediaMetadataRetriever
                    if (duration <= 0) {
                        try {
                            val retriever = android.media.MediaMetadataRetriever()
                            retriever.setDataSource(context, uri)
                            val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                            retriever.release()
                            if (time != null) {
                                duration = time.toLongOrNull() ?: 0L
                            }
                        } catch (_: Exception) {}
                    }

                    var albumArtUri: String? = null
                    if (albumIdCol != -1) {
                        try {
                            val albumId = cursor.getLong(albumIdCol)
                            if (albumId > 0) {
                                albumArtUri = ContentUris.withAppendedId(
                                    Uri.parse("content://media/external/audio/albumart"),
                                    albumId
                                ).toString()
                            }
                        } catch (_: Exception) {}
                    }

                    val color = VIBRANT_COLORS[index % VIBRANT_COLORS.size]
                    tracks.add(
                        TrackEntity(
                            id = "device_audio_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            mediaUri = uri.toString(),
                            artUri = albumArtUri,
                            isVideo = false,
                            fileSizeBytes = size,
                            coverColorHex = color,
                            orderIndex = index
                        )
                    )
                    index++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        tracks
    }

    suspend fun scanDeviceVideos(context: Context): List<TrackEntity> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<TrackEntity>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.ARTIST,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )

        val selection = "${MediaStore.Video.Media.SIZE} > ?"
        val selectionArgs = arrayOf("10240")
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val artistCol = cursor.getColumnIndex(MediaStore.Video.Media.ARTIST)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

                var index = 0
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol)
                    val title = if (rawTitle.isNullOrBlank()) "Video $id" else rawTitle
                    val artist = if (artistCol != -1) cursor.getString(artistCol) ?: "Device Video" else "Device Video"
                    var duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    if (duration <= 0) {
                        try {
                            val retriever = android.media.MediaMetadataRetriever()
                            retriever.setDataSource(context, uri)
                            val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                            retriever.release()
                            if (time != null) {
                                duration = time.toLongOrNull() ?: 0L
                            }
                        } catch (_: Exception) {}
                    }

                    videos.add(
                        TrackEntity(
                            id = "device_video_$id",
                            title = title,
                            artist = if (artist.equals("<unknown>", ignoreCase = true)) "Device Video" else artist,
                            album = "Device Videos",
                            durationMs = duration,
                            mediaUri = uri.toString(),
                            isVideo = true,
                            fileSizeBytes = size,
                            coverColorHex = VIBRANT_COLORS[index % VIBRANT_COLORS.size],
                            orderIndex = index
                        )
                    )
                    index++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        videos
    }
}
