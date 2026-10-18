package com.whereami.locus.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.whereami.locus.domain.PlaceLabel
import com.whereami.locus.domain.TrackPoint

@Entity(tableName = "track_points")
data class TrackPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val altitudeMeters: Double? = null,
    val speedMetersPerSecond: Float? = null,
    val bearingDegrees: Float? = null,
    val recordedAt: Long
) {
    fun toDomain(): TrackPoint = TrackPoint(
        id = id,
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracyMeters,
        altitudeMeters = altitudeMeters,
        speedMetersPerSecond = speedMetersPerSecond,
        bearingDegrees = bearingDegrees,
        recordedAt = recordedAt
    )

    companion object {
        fun fromDomain(point: TrackPoint): TrackPointEntity = TrackPointEntity(
            id = point.id,
            latitude = point.latitude,
            longitude = point.longitude,
            accuracyMeters = point.accuracyMeters,
            altitudeMeters = point.altitudeMeters,
            speedMetersPerSecond = point.speedMetersPerSecond,
            bearingDegrees = point.bearingDegrees,
            recordedAt = point.recordedAt
        )
    }
}

@Entity(tableName = "place_labels")
data class PlaceLabelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float
) {
    fun toDomain(): PlaceLabel = PlaceLabel(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        radiusMeters = radiusMeters
    )

    companion object {
        fun fromDomain(label: PlaceLabel): PlaceLabelEntity = PlaceLabelEntity(
            id = label.id,
            name = label.name,
            latitude = label.latitude,
            longitude = label.longitude,
            radiusMeters = label.radiusMeters
        )
    }
}
