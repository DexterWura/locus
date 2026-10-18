package com.whereami.locus.domain

import kotlinx.coroutines.flow.Flow

interface TrackingController {
    val isTrackingEnabled: Flow<Boolean>
    val status: Flow<TrackingStatus>
    suspend fun setTrackingEnabled(enabled: Boolean)
    fun startTracking()
    fun stopTracking()
    fun resumeIfEnabled()
}
