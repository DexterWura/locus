package com.whereami.locus.domain

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object PathGeometry {
    /** Indoor / urban GPS often reports 30–50 m; keep those points on the trail. */
    const val MAX_DRAW_ACCURACY_METERS = 65f
    const val SEGMENT_GAP_MILLIS = 10 * 60 * 1000L
    const val STOP_RADIUS_METERS = 40.0
    const val STOP_MIN_DURATION_MILLIS = 3 * 60 * 1000L

    const val MAX_IMPLIED_SPEED_MPS = 70.0
    const val STILL_COLLAPSE_RADIUS_METERS = 18.0
    const val STILL_COLLAPSE_MIN_DURATION_MILLIS = 45_000L
    const val DENOISE_MAX_OFFSET_METERS = 8.0

    private const val EARTH_RADIUS_METERS = 6_371_000.0
    private const val METERS_PER_DEGREE_LAT = 111_320.0

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
        drawableSegments(points).flatten()

    fun drawableSegments(points: List<TrackPoint>): List<List<LatLng>> {
        return cleanedTrackSegments(points)
            .map { segment -> segment.map { LatLng(it.latitude, it.longitude) } }
            .filter { it.isNotEmpty() }
    }

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
        val cleaned = cleanedTrackPoints(points)
        if (cleaned.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until cleaned.size) {
            val previous = cleaned[i - 1]
            val point = cleaned[i]
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

    private fun cleanedTrackPoints(points: List<TrackPoint>): List<TrackPoint> =
        cleanedTrackSegments(points).flatten()

    private fun cleanedTrackSegments(points: List<TrackPoint>): List<List<TrackPoint>> {
        val accurate = points.filter { it.accuracyMeters <= MAX_DRAW_ACCURACY_METERS }
        if (accurate.isEmpty()) return emptyList()
        val despiked = dropSpeedSpikes(accurate)
        val collapsed = collapseStillClusters(despiked)
        return splitSegments(collapsed).map { denoiseWithinAccuracy(it) }
    }

    private fun dropSpeedSpikes(points: List<TrackPoint>): List<TrackPoint> {
        if (points.isEmpty()) return emptyList()
        val kept = mutableListOf(points.first())
        for (i in 1 until points.size) {
            val candidate = points[i]
            val previous = kept.last()
            val dtSeconds = (candidate.recordedAt - previous.recordedAt) / 1000.0
            if (dtSeconds <= 0.0) continue
            val distance = haversineMeters(
                previous.latitude,
                previous.longitude,
                candidate.latitude,
                candidate.longitude
            )
            if (distance / dtSeconds > MAX_IMPLIED_SPEED_MPS) continue
            kept += candidate
        }
        return kept
    }

    private fun collapseStillClusters(points: List<TrackPoint>): List<TrackPoint> {
        if (points.isEmpty()) return emptyList()
        val result = mutableListOf<TrackPoint>()
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
                if (distance <= STILL_COLLAPSE_RADIUS_METERS) {
                    cluster += candidate
                    j++
                } else {
                    break
                }
            }
            val duration = cluster.last().recordedAt - cluster.first().recordedAt
            if (duration >= STILL_COLLAPSE_MIN_DURATION_MILLIS && cluster.size >= 2) {
                result += accuracyWeightedPoint(cluster)
                i = j
            } else {
                result += points[i]
                i++
            }
        }
        return result
    }

    private fun accuracyWeightedPoint(cluster: List<TrackPoint>): TrackPoint {
        var weightSum = 0.0
        var latSum = 0.0
        var lonSum = 0.0
        for (point in cluster) {
            val weight = 1.0 / max(point.accuracyMeters.toDouble(), 1.0)
            weightSum += weight
            latSum += point.latitude * weight
            lonSum += point.longitude * weight
        }
        val bestAccuracy = cluster.minOf { it.accuracyMeters }
        return cluster.first().copy(
            latitude = latSum / weightSum,
            longitude = lonSum / weightSum,
            accuracyMeters = bestAccuracy,
            altitudeMeters = null,
            speedMetersPerSecond = null,
            bearingDegrees = null,
            recordedAt = cluster.first().recordedAt
        )
    }

    private fun denoiseWithinAccuracy(points: List<TrackPoint>): List<TrackPoint> {
        if (points.size < 3) return points
        val kept = mutableListOf(points.first())
        var i = 1
        while (i < points.lastIndex) {
            val candidate = points[i]
            val left = kept.last()
            val right = points[i + 1]
            val offset = perpendicularDistanceMeters(
                left.latitude,
                left.longitude,
                candidate.latitude,
                candidate.longitude,
                right.latitude,
                right.longitude
            )
            val tolerance = min(candidate.accuracyMeters.toDouble(), DENOISE_MAX_OFFSET_METERS)
            if (offset <= tolerance) {
                i++
            } else {
                kept += candidate
                i++
            }
        }
        kept += points.last()
        return kept
    }

    private fun perpendicularDistanceMeters(
        aLat: Double,
        aLon: Double,
        bLat: Double,
        bLon: Double,
        cLat: Double,
        cLon: Double
    ): Double {
        val midLat = Math.toRadians((aLat + cLat) / 2.0)
        val metersPerDegreeLon = METERS_PER_DEGREE_LAT * cos(midLat)
        val ax = 0.0
        val ay = 0.0
        val bx = (bLon - aLon) * metersPerDegreeLon
        val by = (bLat - aLat) * METERS_PER_DEGREE_LAT
        val cx = (cLon - aLon) * metersPerDegreeLon
        val cy = (cLat - aLat) * METERS_PER_DEGREE_LAT
        val acLen = sqrt(cx * cx + cy * cy)
        if (acLen < 1e-6) {
            return haversineMeters(aLat, aLon, bLat, bLon)
        }
        return abs(cx * (by - ay) - cy * (bx - ax)) / acLen
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
