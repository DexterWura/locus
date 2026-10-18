package com.whereami.locus.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: TrackPointEntity): Long

    @Query(
        """
        SELECT * FROM track_points
        WHERE recordedAt >= :startInclusive AND recordedAt < :endExclusive
        ORDER BY recordedAt ASC
        """
    )
    fun observeBetween(startInclusive: Long, endExclusive: Long): Flow<List<TrackPointEntity>>

    @Query(
        """
        SELECT * FROM track_points
        WHERE recordedAt >= :startInclusive AND recordedAt < :endExclusive
        ORDER BY recordedAt ASC
        """
    )
    suspend fun getBetween(startInclusive: Long, endExclusive: Long): List<TrackPointEntity>

    @Query("SELECT * FROM track_points ORDER BY recordedAt ASC")
    fun observeAll(): Flow<List<TrackPointEntity>>

    @Query("SELECT * FROM track_points ORDER BY recordedAt ASC")
    suspend fun getAll(): List<TrackPointEntity>

    @Query("DELETE FROM track_points")
    suspend fun deleteAll()
}

@Dao
interface PlaceLabelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(label: PlaceLabelEntity): Long

    @Query("SELECT * FROM place_labels ORDER BY name ASC")
    fun observeAll(): Flow<List<PlaceLabelEntity>>

    @Query("SELECT * FROM place_labels ORDER BY name ASC")
    suspend fun getAll(): List<PlaceLabelEntity>

    @Query("DELETE FROM place_labels WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM place_labels")
    suspend fun deleteAll()
}
