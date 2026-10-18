package com.whereami.locus.tracking

import com.whereami.locus.domain.PathGeometry
import com.whereami.locus.domain.TrackingStatus
import kotlin.math.max

/**
 * Classifies Moving vs Still from successive GPS fixes.
 * Status can also advance to Still on a timer tick when no new fix arrives.
 */
class MotionClassifier(
    private val moveSpeedMps: Float = 0.6f,
    private val moveDistanceMeters: Double = 8.0,
    private val stillAfterMs: Long = 45_000L,
    private val stillRadiusMeters: Double = 25.0
) {
    private var lastLat: Double? = null
    private var lastLon: Double? = null
    private var anchorLat: Double? = null
    private var anchorLon: Double? = null
    private var lastSignificantMoveAtElapsed: Long = 0L
    private var current: TrackingStatus = TrackingStatus.Paused

    fun reset(nowElapsed: Long) {
        lastLat = null
        lastLon = null
        anchorLat = null
        anchorLon = null
        lastSignificantMoveAtElapsed = nowElapsed
        current = TrackingStatus.Moving
    }

    fun markPaused() {
        current = TrackingStatus.Paused
    }

    fun status(): TrackingStatus = current

    fun onFix(
        latitude: Double,
        longitude: Double,
        speedMps: Float?,
        accuracyMeters: Float,
        nowElapsed: Long
    ): TrackingStatus {
        if (current == TrackingStatus.Paused) {
            current = TrackingStatus.Moving
        }

        val previousLat = lastLat
        val previousLon = lastLon
        lastLat = latitude
        lastLon = longitude

        if (previousLat == null || previousLon == null) {
            anchorLat = latitude
            anchorLon = longitude
            lastSignificantMoveAtElapsed = nowElapsed
            current = TrackingStatus.Moving
            return current
        }

        val stepDistance = PathGeometry.haversineMeters(
            previousLat,
            previousLon,
            latitude,
            longitude
        )
        val moveThreshold = max(moveDistanceMeters, accuracyMeters * 0.5)
        val fastEnough = speedMps != null && speedMps >= moveSpeedMps
        val stepped = stepDistance >= moveThreshold

        val anchorDistance = if (anchorLat != null && anchorLon != null) {
            PathGeometry.haversineMeters(anchorLat!!, anchorLon!!, latitude, longitude)
        } else {
            0.0
        }
        val leftStillCluster = anchorDistance >= stillRadiusMeters

        if (fastEnough || stepped || leftStillCluster) {
            lastSignificantMoveAtElapsed = nowElapsed
            anchorLat = latitude
            anchorLon = longitude
            current = TrackingStatus.Moving
            return current
        }

        return evaluateStill(nowElapsed)
    }

    fun onTick(nowElapsed: Long): TrackingStatus {
        if (current == TrackingStatus.Paused) return current
        if (lastLat == null) return current
        return evaluateStill(nowElapsed)
    }

    private fun evaluateStill(nowElapsed: Long): TrackingStatus {
        val idleFor = nowElapsed - lastSignificantMoveAtElapsed
        current = if (idleFor >= stillAfterMs) {
            TrackingStatus.Still
        } else {
            TrackingStatus.Moving
        }
        return current
    }
}
