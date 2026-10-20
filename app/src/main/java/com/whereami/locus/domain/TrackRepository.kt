package com.whereami.locus.domain

import kotlinx.coroutines.flow.Flow

interface TrackRepository {
    fun observePointsForDay(dayStartMillis: Long): Flow<List<TrackPoint>>
    fun observeDaySummaries(): Flow<List<DaySummary>>
    suspend fun getPointsForDay(dayStartMillis: Long): List<TrackPoint>
    suspend fun getDayDetail(dayStartMillis: Long): DayDetail
    suspend fun insertPoint(point: TrackPoint): Long
    suspend fun getUnuploadedPoints(limit: Int): List<TrackPoint>
    suspend fun markPointsUploaded(ids: List<Long>)
    suspend fun deleteAll()
    fun observePlaceLabels(): Flow<List<PlaceLabel>>
    suspend fun upsertPlaceLabel(label: PlaceLabel): Long
    suspend fun deletePlaceLabel(id: Long)
}
