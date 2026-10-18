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
                    val drawable = PathGeometry.drawablePoints(points)
                    DaySummary(
                        dayStartMillis = dayStart,
                        firstPointAt = points.first().recordedAt,
                        lastPointAt = points.last().recordedAt,
                        distanceMeters = PathGeometry.totalDistanceMeters(points),
                        pointCount = points.size,
                        thumbnailPoints = downsample(drawable, 48)
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
        return DayDetail(
            dayStartMillis = dayStartMillis,
            points = points,
            drawablePoints = PathGeometry.drawablePoints(points),
            distanceMeters = PathGeometry.totalDistanceMeters(points),
            timeline = PathGeometry.buildTimeline(points, labels)
        )
    }

    override suspend fun insertPoint(point: TrackPoint): Long {
        return trackPointDao.insert(TrackPointEntity.fromDomain(point.copy(id = 0L)))
    }

    override suspend fun deleteAll() {
        trackPointDao.deleteAll()
        placeLabelDao.deleteAll()
    }

    override fun observePlaceLabels(): Flow<List<PlaceLabel>> {
        return placeLabelDao.observeAll().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun upsertPlaceLabel(label: PlaceLabel): Long {
        return placeLabelDao.upsert(PlaceLabelEntity.fromDomain(label))
    }

    override suspend fun deletePlaceLabel(id: Long) {
        placeLabelDao.deleteById(id)
    }

    private fun downsample(points: List<LatLng>, maxPoints: Int): List<LatLng> {
        if (points.size <= maxPoints) return points
        val step = (points.size - 1).toFloat() / (maxPoints - 1)
        return List(maxPoints) { index ->
            points[(index * step).toInt().coerceIn(0, points.lastIndex)]
        }
    }
}
