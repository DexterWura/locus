package com.whereami.locus.tracking

import com.whereami.locus.domain.TrackingStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class MotionClassifierTest {

    @Test
    fun firstFix_isStill() {
        val classifier = MotionClassifier(stillAfterMs = 25_000L)
        classifier.reset(0L)
        val status = classifier.onFix(40.0, -74.0, speedMps = null, accuracyMeters = 10f, nowElapsed = 0L)
        assertEquals(TrackingStatus.Still, status)
    }

    @Test
    fun reliableSpeed_becomesMoving() {
        val classifier = MotionClassifier(stillAfterMs = 25_000L)
        classifier.reset(0L)
        classifier.onFix(40.0, -74.0, null, 10f, 0L)
        val status = classifier.onFix(40.0, -74.0, speedMps = 1.5f, accuracyMeters = 10f, nowElapsed = 5_000L)
        assertEquals(TrackingStatus.Moving, status)
    }

    @Test
    fun gpsJitter_staysStill() {
        val classifier = MotionClassifier(stillAfterMs = 25_000L)
        classifier.reset(0L)
        classifier.onFix(40.0, -74.0, null, 20f, 0L)
        // ~8 m jitter — under accuracy-aware threshold
        classifier.onFix(40.00007, -74.0, speedMps = 0.4f, accuracyMeters = 20f, nowElapsed = 5_000L)
        classifier.onFix(40.00005, -74.0, speedMps = 0.3f, accuracyMeters = 22f, nowElapsed = 10_000L)
        val status = classifier.onTick(30_000L)
        assertEquals(TrackingStatus.Still, status)
    }

    @Test
    fun noMovementForStillWindow_staysStill() {
        val classifier = MotionClassifier(stillAfterMs = 25_000L)
        classifier.reset(0L)
        classifier.onFix(40.0, -74.0, null, 5f, 0L)
        classifier.onFix(40.00001, -74.0, speedMps = 0.1f, accuracyMeters = 5f, nowElapsed = 10_000L)
        val status = classifier.onTick(30_000L)
        assertEquals(TrackingStatus.Still, status)
    }

    @Test
    fun walkingAwayFromAnchor_becomesMoving() {
        val classifier = MotionClassifier(stillAfterMs = 25_000L, stillRadiusMeters = 45.0)
        classifier.reset(0L)
        classifier.onFix(40.0, -74.0, null, 5f, 0L)
        classifier.onTick(30_000L)
        // First large step (~55 m) — needs confirmation
        classifier.onFix(40.0005, -74.0, speedMps = 0.2f, accuracyMeters = 5f, nowElapsed = 35_000L)
        // Second step confirms movement
        val status = classifier.onFix(40.0010, -74.0, speedMps = 0.2f, accuracyMeters = 5f, nowElapsed = 40_000L)
        assertEquals(TrackingStatus.Moving, status)
    }
}
