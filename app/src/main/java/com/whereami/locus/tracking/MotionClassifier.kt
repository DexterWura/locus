package com.whereami.locus.tracking

import com.whereami.locus.domain.PathGeometry
import com.whereami.locus.domain.TrackingStatus
import kotlin.math.max

/**
 * Classifies Moving vs Still from successive GPS fixes.
 * Defaults to Still until movement is confirmed, so GPS jitter does not look like walking.
 */
class MotionClassifier(
    private val moveSpeedMps: Float = 1.2f,
    private val moveDistanceMeters: Double = 15.0,
    private val stillAfterMs: Long = 25_000L,
    private val stillRadiusMeters: Double = 45.0
) {
    private var lastLat: Double? = null
    private var lastLon: Double? = null
    private var anchorLat: Double? = null
    private var anchorLon: Double? = null
    private var lastSignificantMoveAtElapsed: Long = 0L
    private var consecutiveMoveSignals: Int = 0
    private var current: TrackingStatus = TrackingStatus.Paused

    fun reset(nowElapsed: Long) {
        lastLat = null
        lastLon = null
        anchorLat = null
        anchorLon = null
        lastSignificantMoveAtElapsed = nowElapsed
        consecutiveMoveSignals = 0
        current = TrackingStatus.Still
    }

    fun markPaused() {
        current = TrackingStatus.Paused
        consecutiveMoveSignals = 0
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
            current = TrackingStatus.Still
        }

        val previousLat = lastLat
        val previousLon = lastLon
        lastLat = latitude
        lastLon = longitude

        if (previousLat == null || previousLon == null) {
            anchorLat = latitude
            anchorLon = longitude
            lastSignificantMoveAtElapsed = nowElapsed
            consecutiveMoveSignals = 0
            current = TrackingStatus.Still
            return current
        }

        val stepDistance = PathGeometry.haversineMeters(
            previousLat,
            previousLon,
            latitude,
            longitude
        )
        val moveThreshold = max(moveDistanceMeters, accuracyMeters.toDouble() * 1.25)
        val reliableSpeed =
            speedMps != null &&
                accuracyMeters <= 35f &&
                speedMps >= moveSpeedMps
        val stepped = stepDistance >= moveThreshold

        val anchorDistance = if (anchorLat != null && anchorLon != null) {
            PathGeometry.haversineMeters(anchorLat!!, anchorLon!!, latitude, longitude)
        } else {
            0.0
        }
        val leaveClusterThreshold = max(stillRadiusMeters, accuracyMeters.toDouble() * 2.0)
        val leftStillCluster = anchorDistance >= leaveClusterThreshold

        if (reliableSpeed || stepped || leftStillCluster) {
            consecutiveMoveSignals += 1
            val confirmed = reliableSpeed || consecutiveMoveSignals >= 2
            if (confirmed) {
                lastSignificantMoveAtElapsed = nowElapsed
                anchorLat = latitude
                anchorLon = longitude
                current = TrackingStatus.Moving
                return current
            }
            return evaluateStill(nowElapsed)
        }

        consecutiveMoveSignals = 0
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
        } else if (current == TrackingStatus.Moving) {
            TrackingStatus.Moving
        } else {
            TrackingStatus.Still
        }
        return current
    }
}
