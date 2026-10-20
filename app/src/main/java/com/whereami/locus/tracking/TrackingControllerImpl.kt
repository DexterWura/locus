package com.whereami.locus.tracking

import android.content.Context
import com.whereami.locus.data.SettingsStore
import com.whereami.locus.data.sync.LocationUploader
import com.whereami.locus.domain.TrackRepository
import com.whereami.locus.domain.TrackingController
import com.whereami.locus.domain.TrackingMode
import com.whereami.locus.domain.TrackingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TrackingControllerImpl(
    private val context: Context,
    private val settingsStore: SettingsStore,
    private val locationSource: LocationSource,
    private val trackRepository: TrackRepository,
    private val locationUploader: LocationUploader,
    private val appScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : TrackingController {
    private val foregroundTracker = ForegroundTracker(
        locationSource = locationSource,
        trackRepository = trackRepository,
        settingsStore = settingsStore,
        locationUploader = locationUploader,
        scope = appScope
    )

    override val isTrackingEnabled: StateFlow<Boolean> = settingsStore.trackingEnabled
    override val status: Flow<TrackingStatus> = locationSource.status
    override val trackingMode: StateFlow<TrackingMode> = settingsStore.trackingMode

    override suspend fun setTrackingEnabled(enabled: Boolean) {
        settingsStore.setTrackingEnabled(enabled)
        if (enabled) {
            startTracking()
        } else {
            stopTracking()
        }
    }

    override suspend fun setTrackingMode(mode: TrackingMode) {
        val wasEnabled = settingsStore.trackingEnabled.value
        if (wasEnabled) {
            stopTracking()
        }
        settingsStore.setTrackingMode(mode)
        if (wasEnabled) {
            startTracking()
        }
    }

    override fun startTracking() {
        when (settingsStore.trackingMode.value) {
            TrackingMode.Background -> {
                foregroundTracker.stop()
                if (LocationPermissions.hasFine(context)) {
                    TrackingService.start(context.applicationContext)
                }
            }
            TrackingMode.Foreground -> {
                TrackingService.stop(context.applicationContext)
                if (LocationPermissions.hasFine(context)) {
                    foregroundTracker.start()
                }
            }
        }
    }

    override fun stopTracking() {
        TrackingService.stop(context.applicationContext)
        foregroundTracker.stop()
        locationSource.markPaused()
    }

    override fun onAppForeground() {
        if (!settingsStore.trackingEnabled.value) return
        if (settingsStore.trackingMode.value == TrackingMode.Foreground) {
            foregroundTracker.start()
        }
    }

    override fun onAppBackground() {
        if (settingsStore.trackingMode.value == TrackingMode.Foreground) {
            foregroundTracker.stop()
        }
    }

    override fun resumeIfEnabled() {
        appScope.launch {
            if (!settingsStore.trackingEnabled.value) return@launch
            when (settingsStore.trackingMode.value) {
                TrackingMode.Background -> {
                    if (LocationPermissions.hasFine(context)) {
                        TrackingService.start(context.applicationContext)
                    }
                }
                TrackingMode.Foreground -> {
                    // Wait for MainActivity onStart to begin collecting.
                }
            }
        }
    }
}
