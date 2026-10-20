package com.whereami.locus.domain

data class TrackPoint(
    val id: Long = 0L,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val altitudeMeters: Double? = null,
    val speedMetersPerSecond: Float? = null,
    val bearingDegrees: Float? = null,
    val recordedAt: Long
)

data class PlaceLabel(
    val id: Long = 0L,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = 80f
)

data class DaySummary(
    val dayStartMillis: Long,
    val firstPointAt: Long,
    val lastPointAt: Long,
    val distanceMeters: Double,
    val pointCount: Int,
    val thumbnailPoints: List<LatLng>,
    val thumbnailSegments: List<List<LatLng>> = emptyList()
)

data class LatLng(
    val latitude: Double,
    val longitude: Double
)

sealed class TimelineItem {
    data class Stop(
        val center: LatLng,
        val arrivedAt: Long,
        val departedAt: Long,
        val placeName: String? = null
    ) : TimelineItem()

    data class Trip(
        val points: List<LatLng>,
        val startedAt: Long,
        val endedAt: Long,
        val distanceMeters: Double
    ) : TimelineItem()
}

data class DayDetail(
    val dayStartMillis: Long,
    val points: List<TrackPoint>,
    val drawablePoints: List<LatLng>,
    val drawableSegments: List<List<LatLng>>,
    val distanceMeters: Double,
    val timeline: List<TimelineItem>
)

enum class TrackingStatus {
    Moving,
    Still,
    Paused
}
