package com.whereami.locus.domain

import kotlinx.coroutines.flow.Flow

interface TrackingController {
    val isTrackingEnabled: Flow<Boolean>
    val status: Flow<TrackingStatus>
    val trackingMode: Flow<TrackingMode>
    suspend fun setTrackingEnabled(enabled: Boolean)
    suspend fun setTrackingMode(mode: TrackingMode)
    fun startTracking()
    fun stopTracking()
    fun onAppForeground()
    fun onAppBackground()
    fun resumeIfEnabled()
}
