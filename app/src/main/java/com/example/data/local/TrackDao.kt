package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks WHERE isVideo = 0 ORDER BY orderIndex ASC, dateAdded DESC")
    fun getAllAudioTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isVideo = 1 ORDER BY orderIndex ASC, dateAdded DESC")
    fun getAllVideoTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1 ORDER BY orderIndex ASC, dateAdded DESC")
    fun getFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Update
    suspend fun updateTracks(tracks: List<TrackEntity>)

    @Query("UPDATE tracks SET isFavorite = :isFav WHERE id = :trackId")
    suspend fun updateFavorite(trackId: String, isFav: Boolean)

    @Query("UPDATE tracks SET title = :newTitle, coverColorHex = :newCoverColor WHERE id = :trackId")
    suspend fun updateTitleAndCover(trackId: String, newTitle: String, newCoverColor: Long)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrackById(trackId: String)

    @Query("SELECT COUNT(*) FROM tracks WHERE isVideo = 0")
    suspend fun getAudioTrackCount(): Int
}
