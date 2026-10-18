package com.whereami.locus.tracking

import com.whereami.locus.domain.TrackingStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class MotionClassifierTest {

    @Test
    fun firstFix_isMoving() {
        val classifier = MotionClassifier(stillAfterMs = 45_000L)
        classifier.reset(0L)
        val status = classifier.onFix(40.0, -74.0, speedMps = null, accuracyMeters = 10f, nowElapsed = 0L)
        assertEquals(TrackingStatus.Moving, status)
    }

    @Test
    fun speedAboveThreshold_keepsMoving() {
        val classifier = MotionClassifier(stillAfterMs = 45_000L)
        classifier.reset(0L)
        classifier.onFix(40.0, -74.0, null, 10f, 0L)
        val status = classifier.onFix(40.0, -74.0, speedMps = 1.5f, accuracyMeters = 10f, nowElapsed = 5_000L)
        assertEquals(TrackingStatus.Moving, status)
    }

    @Test
    fun noMovementForStillWindow_becomesStill() {
        val classifier = MotionClassifier(stillAfterMs = 45_000L)
        classifier.reset(0L)
        classifier.onFix(40.0, -74.0, null, 5f, 0L)
        // Tiny GPS jitter under move threshold
        classifier.onFix(40.00001, -74.0, speedMps = 0.1f, accuracyMeters = 5f, nowElapsed = 10_000L)
        val status = classifier.onTick(50_000L)
        assertEquals(TrackingStatus.Still, status)
    }

    @Test
    fun walkingAwayFromAnchor_becomesMoving() {
        val classifier = MotionClassifier(stillAfterMs = 45_000L, stillRadiusMeters = 25.0)
        classifier.reset(0L)
        classifier.onFix(40.0, -74.0, null, 5f, 0L)
        classifier.onTick(50_000L) // Still
        // ~40m north
        val status = classifier.onFix(40.0004, -74.0, speedMps = 0.2f, accuracyMeters = 5f, nowElapsed = 55_000L)
        assertEquals(TrackingStatus.Moving, status)
    }
}
