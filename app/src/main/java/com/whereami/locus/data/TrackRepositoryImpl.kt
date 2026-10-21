package com.whereami.locus.data

import com.whereami.locus.data.db.LocusDatabase
import com.whereami.locus.data.db.PlaceLabelEntity
import com.whereami.locus.data.db.TrackPointEntity
import com.whereami.locus.domain.DayDetail
import com.whereami.locus.domain.DaySummary
import com.whereami.locus.domain.DayTime
import com.whereami.locus.domain.LatLng
import com.whereami.locus.domain.PathGeometry
import com.whereami.locus.domain.PlaceLabel
import com.whereami.locus.domain.TrackPoint
import com.whereami.locus.domain.TrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TrackRepositoryImpl(
    private val database: LocusDatabase
) : TrackRepository {
    private val trackPointDao = database.trackPointDao()
    private val placeLabelDao = database.placeLabelDao()

    override fun observePointsForDay(dayStartMillis: Long): Flow<List<TrackPoint>> {
        val end = DayTime.endOfDayMillis(dayStartMillis)
        return trackPointDao.observeBetween(dayStartMillis, end).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeDaySummaries(): Flow<List<DaySummary>> {
        return trackPointDao.observeAll().map { entities ->
            entities
                .map { it.toDomain() }
                .groupBy { DayTime.startOfDayMillis(it.recordedAt) }
                .map { (dayStart, points) ->
                    val segments = PathGeometry.drawableSegments(points)
                    val drawable = segments.flatten()
                    val thumbnailSegments = downsampleSegments(segments, 48)
                    DaySummary(
                        dayStartMillis = dayStart,
                        firstPointAt = points.first().recordedAt,
                        lastPointAt = points.last().recordedAt,
                        distanceMeters = PathGeometry.totalDistanceMeters(points),
                        pointCount = points.size,
                        thumbnailPoints = thumbnailSegments.flatten(),
                        thumbnailSegments = thumbnailSegments
                    )
                }
                .sortedByDescending { it.dayStartMillis }
        }
    }

    override suspend fun getPointsForDay(dayStartMillis: Long): List<TrackPoint> {
        val end = DayTime.endOfDayMillis(dayStartMillis)
        return trackPointDao.getBetween(dayStartMillis, end).map { it.toDomain() }
    }

    override suspend fun getDayDetail(dayStartMillis: Long): DayDetail {
        val points = getPointsForDay(dayStartMillis)
        val labels = placeLabelDao.getAll().map { it.toDomain() }
        val segments = PathGeometry.drawableSegments(points)
        return DayDetail(
            dayStartMillis = dayStartMillis,
            points = points,
            drawablePoints = segments.flatten(),
            drawableSegments = segments,
            distanceMeters = PathGeometry.totalDistanceMeters(points),
            timeline = PathGeometry.buildTimeline(points, labels)
        )
    }

    override suspend fun insertPoint(point: TrackPoint): Long {
        return trackPointDao.insert(TrackPointEntity.fromDomain(point.copy(id = 0L)))
    }

    override suspend fun getUnuploadedPoints(limit: Int): List<TrackPoint> {
        return trackPointDao.getUnuploaded(limit).map { it.toDomain() }
    }

    override suspend fun markPointsUploaded(ids: List<Long>) {
        if (ids.isEmpty()) return
        trackPointDao.markUploaded(ids, System.currentTimeMillis())
    }

    override suspend fun deleteAll() {
        trackPointDao.deleteAll()
        placeLabelDao.deleteAll()
    }

    override fun observePlaceLabels(): Flow<List<PlaceLabel>> {
        return placeLabelDao.observeAll().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getPlaceLabels(): List<PlaceLabel> {
        return placeLabelDao.getAll().map { it.toDomain() }
    }

    override suspend fun upsertPlaceLabel(label: PlaceLabel): Long {
        return placeLabelDao.upsert(PlaceLabelEntity.fromDomain(label))
    }

    override suspend fun deletePlaceLabel(id: Long) {
        placeLabelDao.deleteById(id)
    }

    private fun downsampleSegments(
        segments: List<List<LatLng>>,
        maxPoints: Int
    ): List<List<LatLng>> {
        val total = segments.sumOf { it.size }
        if (total <= maxPoints) return segments
        return segments.map { segment ->
            val share = ((segment.size.toFloat() / total) * maxPoints)
                .toInt()
                .coerceAtLeast(if (segment.size >= 2) 2 else segment.size)
            downsample(segment, share.coerceAtMost(segment.size))
        }
    }

    private fun downsample(points: List<LatLng>, maxPoints: Int): List<LatLng> {
        if (points.size <= maxPoints) return points
        val step = (points.size - 1).toFloat() / (maxPoints - 1)
        return List(maxPoints) { index ->
            points[(index * step).toInt().coerceIn(0, points.lastIndex)]
        }
    }
}
