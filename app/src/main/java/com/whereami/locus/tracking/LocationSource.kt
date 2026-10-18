package com.whereami.locus.tracking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.whereami.locus.data.SettingsStore
import com.whereami.locus.domain.PathGeometry
import com.whereami.locus.domain.TrackPoint
import com.whereami.locus.domain.TrackingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class LocationSource(
    private val context: Context
) {
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val motionClassifier = MotionClassifier(
        stillAfterMs = SettingsStore.STILL_AFTER_MS
    )

    private val _status = MutableStateFlow(TrackingStatus.Paused)
    val status: StateFlow<TrackingStatus> = _status.asStateFlow()

    private val _speedMps = MutableStateFlow<Float?>(null)
    val speedMps: StateFlow<Float?> = _speedMps.asStateFlow()

    private var lastAccepted: Location? = null
    private var lastAcceptedAtElapsed: Long = 0L

    fun observeLocations(sampleIntervalMs: Long): Flow<TrackPoint> = callbackFlow {
        if (!hasFineLocationPermission()) {
            publishPaused()
            close()
            return@callbackFlow
        }

        val now = SystemClock.elapsedRealtime()
        motionClassifier.reset(now)
        publishStatus(motionClassifier.status())
        lastAccepted = null
        lastAcceptedAtElapsed = 0L

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val elapsed = SystemClock.elapsedRealtime()
                val speed = if (location.hasSpeed()) location.speed else null
                _speedMps.value = speed

                val motion = motionClassifier.onFix(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    speedMps = speed,
                    accuracyMeters = location.accuracy,
                    nowElapsed = elapsed
                )
                publishStatus(motion)

                val accepted = maybeAcceptForStorage(location, sampleIntervalMs, motion) ?: return
                trySend(accepted.toTrackPoint())
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) {
                publishPaused()
            }
        }

        val provider = preferredProvider()
        if (provider == null) {
            publishPaused()
            close()
            return@callbackFlow
        }

        // minDistance = 0 so we keep receiving fixes while stationary and can flip to Still.
        val pollIntervalMs = minOf(sampleIntervalMs, 5_000L).coerceAtLeast(2_000L)
        try {
            locationManager.requestLocationUpdates(
                provider,
                pollIntervalMs,
                0f,
                listener,
                Looper.getMainLooper()
            )
            locationManager.getLastKnownLocation(provider)?.let { last ->
                listener.onLocationChanged(last)
            }
        } catch (_: SecurityException) {
            publishPaused()
            close()
            return@callbackFlow
        }

        val heartbeatJob = launchHeartbeat()

        awaitClose {
            heartbeatJob.cancel()
            locationManager.removeUpdates(listener)
            publishPaused()
            _speedMps.value = null
        }
    }

    private fun CoroutineScope.launchHeartbeat(): Job = launch(Dispatchers.Default) {
        while (isActive) {
            delay(3_000L)
            val next = motionClassifier.onTick(SystemClock.elapsedRealtime())
            if (next != TrackingStatus.Paused) {
                publishStatus(next)
                if (next == TrackingStatus.Still) {
                    _speedMps.value = 0f
                }
            }
        }
    }

    fun markPaused() {
        motionClassifier.markPaused()
        publishPaused()
        _speedMps.value = null
    }

    private fun publishStatus(status: TrackingStatus) {
        if (_status.value != status) {
            _status.value = status
        }
    }

    private fun publishPaused() {
        motionClassifier.markPaused()
        _status.value = TrackingStatus.Paused
    }

    private fun maybeAcceptForStorage(
        location: Location,
        baseIntervalMs: Long,
        motion: TrackingStatus
    ): Location? {
        val nowElapsed = SystemClock.elapsedRealtime()
        val previous = lastAccepted
        if (previous != null) {
            val distance = PathGeometry.haversineMeters(
                previous.latitude,
                previous.longitude,
                location.latitude,
                location.longitude
            )
            val elapsedSinceAccept = nowElapsed - lastAcceptedAtElapsed
            val requiredInterval = if (motion == TrackingStatus.Still) {
                SettingsStore.STILL_SAMPLE_INTERVAL_MS
            } else {
                baseIntervalMs
            }
            val movedEnough = distance >= SettingsStore.MIN_DISPLACEMENT_METERS
            if (!movedEnough && elapsedSinceAccept < requiredInterval) {
                return null
            }
            if (motion == TrackingStatus.Still && !movedEnough &&
                elapsedSinceAccept < SettingsStore.STILL_SAMPLE_INTERVAL_MS
            ) {
                return null
            }
        }

        lastAccepted = location
        lastAcceptedAtElapsed = nowElapsed
        return location
    }

    private fun preferredProvider(): String? {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER) ->
                LocationManager.FUSED_PROVIDER

            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER

            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                LocationManager.NETWORK_PROVIDER

            else -> null
        }
    }

    private fun hasFineLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun Location.toTrackPoint(): TrackPoint {
        return TrackPoint(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracy,
            altitudeMeters = if (hasAltitude()) altitude else null,
            speedMetersPerSecond = if (hasSpeed()) speed else null,
            bearingDegrees = if (hasBearing()) bearing else null,
            recordedAt = if (time > 0L) time else System.currentTimeMillis()
        )
    }
}
