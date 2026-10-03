package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String = "AuraTune Offline",
    val durationMs: Long = 0L,
    val mediaUri: String,
    val artUri: String? = null,
    val isVideo: Boolean = false,
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L,
    val sourceUrl: String? = null,
    val orderIndex: Int = 0,
    val coverColorHex: Long = 0xFF6366F1,
    val playCount: Int = 0,
    val lastPlayedTime: Long = 0L
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val coverColorHex: Long = 0xFF6366F1,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    indices = [Index("playlistId"), Index("trackId")]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: String,
    val orderIndex: Int = 0
)

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val mediaUri: String,
    val mediaType: String, // "AUDIO", "VIDEO", "PHOTO"
    val sizeBytes: Long = 0L,
    val durationMs: Long = 0L,
    val dateHidden: Long = System.currentTimeMillis(),
    val originalArtist: String = ""
)
