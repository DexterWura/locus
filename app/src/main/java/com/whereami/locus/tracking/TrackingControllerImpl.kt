package com.whereami.locus.tracking

import android.content.Context
import com.whereami.locus.data.SettingsStore
import com.whereami.locus.domain.TrackingController
import com.whereami.locus.domain.TrackingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TrackingControllerImpl(
    private val context: Context,
    private val settingsStore: SettingsStore,
    private val locationSource: LocationSource,
    private val appScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : TrackingController {
    override val isTrackingEnabled: Flow<Boolean> = settingsStore.trackingEnabled
    override val status: Flow<TrackingStatus> = locationSource.status

    override suspend fun setTrackingEnabled(enabled: Boolean) {
        settingsStore.setTrackingEnabled(enabled)
        if (enabled) {
            startTracking()
        } else {
            stopTracking()
        }
    }

    override fun startTracking() {
        TrackingService.start(context.applicationContext)
    }

    override fun stopTracking() {
        TrackingService.stop(context.applicationContext)
        locationSource.markPaused()
    }

    override fun resumeIfEnabled() {
        appScope.launch {
            if (settingsStore.trackingEnabled.first()) {
                startTracking()
            }
        }
    }
}
