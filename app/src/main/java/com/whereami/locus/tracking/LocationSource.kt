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
import androidx.core.content.ContextCompat
import com.whereami.locus.data.SettingsStore
import com.whereami.locus.domain.PathGeometry
import com.whereami.locus.domain.TrackPoint
import com.whereami.locus.domain.TrackingStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

class LocationSource(
    private val context: Context
) {
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _status = MutableStateFlow(TrackingStatus.Paused)
    val status: StateFlow<TrackingStatus> = _status.asStateFlow()

    private var lastAccepted: Location? = null
    private var lastAcceptedAtElapsed: Long = 0L
    private var lastMovementAtElapsed: Long = 0L

    fun observeLocations(sampleIntervalMs: Long): Flow<TrackPoint> = callbackFlow {
        if (!hasFineLocationPermission()) {
            _status.value = TrackingStatus.Paused
            close()
            return@callbackFlow
        }

        lastMovementAtElapsed = android.os.SystemClock.elapsedRealtime()
        _status.value = TrackingStatus.Moving

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val accepted = maybeAccept(location, sampleIntervalMs) ?: return
                trySend(accepted.toTrackPoint())
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) {
                _status.value = TrackingStatus.Paused
            }
        }

        val provider = preferredProvider()
        if (provider == null) {
            _status.value = TrackingStatus.Paused
            close()
            return@callbackFlow
        }

        try {
            locationManager.requestLocationUpdates(
                provider,
                sampleIntervalMs,
                SettingsStore.MIN_DISPLACEMENT_METERS,
                listener,
                Looper.getMainLooper()
            )
            locationManager.getLastKnownLocation(provider)?.let { last ->
                maybeAccept(last, sampleIntervalMs)?.let { trySend(it.toTrackPoint()) }
            }
        } catch (_: SecurityException) {
            _status.value = TrackingStatus.Paused
            close()
            return@callbackFlow
        }

        awaitClose {
            locationManager.removeUpdates(listener)
            _status.value = TrackingStatus.Paused
        }
    }

    fun markPaused() {
        _status.value = TrackingStatus.Paused
    }

    private fun maybeAccept(location: Location, baseIntervalMs: Long): Location? {
        val nowElapsed = android.os.SystemClock.elapsedRealtime()
        val previous = lastAccepted
        if (previous != null) {
            val distance = PathGeometry.haversineMeters(
                previous.latitude,
                previous.longitude,
                location.latitude,
                location.longitude
            )
            val elapsedSinceAccept = nowElapsed - lastAcceptedAtElapsed
            val moved = distance >= SettingsStore.MIN_DISPLACEMENT_METERS ||
                (location.hasSpeed() && location.speed > 0.5f)

            if (moved) {
                lastMovementAtElapsed = nowElapsed
                _status.value = TrackingStatus.Moving
            } else {
                val stillFor = nowElapsed - lastMovementAtElapsed
                if (stillFor >= SettingsStore.STILL_AFTER_MS) {
                    _status.value = TrackingStatus.Still
                    if (elapsedSinceAccept < SettingsStore.STILL_SAMPLE_INTERVAL_MS) {
                        return null
                    }
                } else {
                    _status.value = TrackingStatus.Moving
                    if (elapsedSinceAccept < baseIntervalMs &&
                        distance < SettingsStore.MIN_DISPLACEMENT_METERS
                    ) {
                        return null
                    }
                }
            }
        } else {
            lastMovementAtElapsed = nowElapsed
            _status.value = TrackingStatus.Moving
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
