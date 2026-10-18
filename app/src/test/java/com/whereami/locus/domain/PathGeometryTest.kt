package com.whereami.locus.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PathGeometryTest {

    @Test
    fun haversine_samePoint_isZero() {
        val distance = PathGeometry.haversineMeters(40.0, -74.0, 40.0, -74.0)
        assertEquals(0.0, distance, 0.01)
    }

    @Test
    fun haversine_knownShortDistance() {
        // Roughly 111 meters north at the equator-ish for 0.001 degrees latitude
        val distance = PathGeometry.haversineMeters(0.0, 0.0, 0.001, 0.0)
        assertEquals(111.2, distance, 1.0)
    }

    @Test
    fun drawablePoints_dropsInaccurateFixes() {
        val points = listOf(
            point(0.0, 0.0, accuracy = 10f, at = 1_000),
            point(0.001, 0.0, accuracy = 50f, at = 2_000),
            point(0.002, 0.0, accuracy = 20f, at = 3_000)
        )
        val drawable = PathGeometry.drawablePoints(points)
        assertEquals(2, drawable.size)
        assertEquals(0.0, drawable[0].latitude, 0.0)
        assertEquals(0.002, drawable[1].latitude, 0.0)
    }

    @Test
    fun splitSegments_breaksAfterTenMinuteGap() {
        val points = listOf(
            point(0.0, 0.0, at = 0),
            point(0.001, 0.0, at = 60_000),
            point(0.002, 0.0, at = 60_000 + PathGeometry.SEGMENT_GAP_MILLIS + 1),
            point(0.003, 0.0, at = 60_000 + PathGeometry.SEGMENT_GAP_MILLIS + 30_000)
        )
        val segments = PathGeometry.splitSegments(points)
        assertEquals(2, segments.size)
        assertEquals(2, segments[0].size)
        assertEquals(2, segments[1].size)
    }

    @Test
    fun buildTimeline_detectsStopAndTrip() {
        val base = 1_000_000L
        val points = mutableListOf<TrackPoint>()
        // Moving north for a bit
        repeat(5) { i ->
            points += point(0.0 + i * 0.001, 0.0, at = base + i * 30_000L)
        }
        // Stay in place for 4 minutes
        val stopStart = base + 5 * 30_000L
        repeat(8) { i ->
            points += point(0.005, 0.0, at = stopStart + i * 30_000L)
        }
        // Move again
        val resume = stopStart + 8 * 30_000L
        repeat(4) { i ->
            points += point(0.005 + (i + 1) * 0.001, 0.0, at = resume + i * 30_000L)
        }

        val timeline = PathGeometry.buildTimeline(points)
        assertTrue(timeline.any { it is TimelineItem.Stop })
        assertTrue(timeline.any { it is TimelineItem.Trip })
        val stop = timeline.filterIsInstance<TimelineItem.Stop>().first()
        assertTrue(stop.departedAt - stop.arrivedAt >= PathGeometry.STOP_MIN_DURATION_MILLIS)
    }

    @Test
    fun matchPlaceName_usesNearestWithinRadius() {
        val center = LatLng(40.0, -74.0)
        val labels = listOf(
            PlaceLabel(name = "Home", latitude = 40.0002, longitude = -74.0, radiusMeters = 80f),
            PlaceLabel(name = "Cafe", latitude = 40.01, longitude = -74.0, radiusMeters = 80f)
        )
        assertEquals("Home", PathGeometry.matchPlaceName(center, labels))
    }

    private fun point(
        lat: Double,
        lon: Double,
        accuracy: Float = 10f,
        at: Long
    ): TrackPoint = TrackPoint(
        latitude = lat,
        longitude = lon,
        accuracyMeters = accuracy,
        recordedAt = at
    )
}
