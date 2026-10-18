package com.whereami.locus.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object PathGeometry {
    const val MAX_DRAW_ACCURACY_METERS = 30f
    const val SEGMENT_GAP_MILLIS = 10 * 60 * 1000L
    const val STOP_RADIUS_METERS = 40.0
    const val STOP_MIN_DURATION_MILLIS = 3 * 60 * 1000L

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun haversineMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    fun haversineMeters(a: LatLng, b: LatLng): Double =
        haversineMeters(a.latitude, a.longitude, b.latitude, b.longitude)

    fun drawablePoints(points: List<TrackPoint>): List<LatLng> =
        points
            .filter { it.accuracyMeters <= MAX_DRAW_ACCURACY_METERS }
            .map { LatLng(it.latitude, it.longitude) }

    fun splitSegments(points: List<TrackPoint>): List<List<TrackPoint>> {
        if (points.isEmpty()) return emptyList()
        val segments = mutableListOf<MutableList<TrackPoint>>()
        var current = mutableListOf(points.first())
        for (i in 1 until points.size) {
            val previous = points[i - 1]
            val point = points[i]
            if (point.recordedAt - previous.recordedAt > SEGMENT_GAP_MILLIS) {
                segments += current
                current = mutableListOf(point)
            } else {
                current += point
            }
        }
        segments += current
        return segments
    }

    fun totalDistanceMeters(points: List<TrackPoint>): Double {
        val drawable = points.filter { it.accuracyMeters <= MAX_DRAW_ACCURACY_METERS }
        if (drawable.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until drawable.size) {
            val previous = drawable[i - 1]
            val point = drawable[i]
            if (point.recordedAt - previous.recordedAt > SEGMENT_GAP_MILLIS) continue
            total += haversineMeters(
                previous.latitude,
                previous.longitude,
                point.latitude,
                point.longitude
            )
        }
        return total
    }

    fun buildTimeline(
        points: List<TrackPoint>,
        placeLabels: List<PlaceLabel> = emptyList()
    ): List<TimelineItem> {
        if (points.isEmpty()) return emptyList()
        val usable = points.filter { it.accuracyMeters <= MAX_DRAW_ACCURACY_METERS }
        if (usable.isEmpty()) return emptyList()

        val stops = detectStops(usable)
        if (stops.isEmpty()) {
            return listOf(
                TimelineItem.Trip(
                    points = usable.map { LatLng(it.latitude, it.longitude) },
                    startedAt = usable.first().recordedAt,
                    endedAt = usable.last().recordedAt,
                    distanceMeters = totalDistanceMeters(usable)
                )
            )
        }

        val timeline = mutableListOf<TimelineItem>()
        var cursor = 0
        for (stop in stops) {
            if (cursor < stop.startIndex) {
                val tripPoints = usable.subList(cursor, stop.startIndex)
                if (tripPoints.size >= 2) {
                    timeline += TimelineItem.Trip(
                        points = tripPoints.map { LatLng(it.latitude, it.longitude) },
                        startedAt = tripPoints.first().recordedAt,
                        endedAt = tripPoints.last().recordedAt,
                        distanceMeters = totalDistanceMeters(tripPoints)
                    )
                }
            }
            val center = LatLng(
                stop.points.map { it.latitude }.average(),
                stop.points.map { it.longitude }.average()
            )
            timeline += TimelineItem.Stop(
                center = center,
                arrivedAt = stop.points.first().recordedAt,
                departedAt = stop.points.last().recordedAt,
                placeName = matchPlaceName(center, placeLabels)
            )
            cursor = stop.endIndexExclusive
        }
        if (cursor < usable.size) {
            val tripPoints = usable.subList(cursor, usable.size)
            if (tripPoints.size >= 2) {
                timeline += TimelineItem.Trip(
                    points = tripPoints.map { LatLng(it.latitude, it.longitude) },
                    startedAt = tripPoints.first().recordedAt,
                    endedAt = tripPoints.last().recordedAt,
                    distanceMeters = totalDistanceMeters(tripPoints)
                )
            }
        }
        return timeline
    }

    fun matchPlaceName(center: LatLng, placeLabels: List<PlaceLabel>): String? {
        return placeLabels
            .map { label ->
                label to haversineMeters(
                    center.latitude,
                    center.longitude,
                    label.latitude,
                    label.longitude
                )
            }
            .filter { (label, distance) -> distance <= label.radiusMeters }
            .minByOrNull { it.second }
            ?.first
            ?.name
    }

    private data class RawStop(
        val points: List<TrackPoint>,
        val startIndex: Int,
        val endIndexExclusive: Int
    )

    private fun detectStops(points: List<TrackPoint>): List<RawStop> {
        val stops = mutableListOf<RawStop>()
        var i = 0
        while (i < points.size) {
            val cluster = mutableListOf(points[i])
            var j = i + 1
            while (j < points.size) {
                val candidate = points[j]
                val centerLat = cluster.map { it.latitude }.average()
                val centerLon = cluster.map { it.longitude }.average()
                val distance = haversineMeters(
                    centerLat,
                    centerLon,
                    candidate.latitude,
                    candidate.longitude
                )
                if (distance <= STOP_RADIUS_METERS) {
                    cluster += candidate
                    j++
                } else {
                    break
                }
            }
            val duration = cluster.last().recordedAt - cluster.first().recordedAt
            if (duration >= STOP_MIN_DURATION_MILLIS && cluster.size >= 2) {
                stops += RawStop(cluster, i, j)
                i = j
            } else {
                i++
            }
        }
        return stops
    }
}
