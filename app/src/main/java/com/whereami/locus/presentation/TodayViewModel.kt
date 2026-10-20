package com.whereami.locus.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.whereami.locus.AppContainer
import com.whereami.locus.domain.DayTime
import com.whereami.locus.domain.LatLng
import com.whereami.locus.domain.PathGeometry
import com.whereami.locus.domain.TrackPoint
import com.whereami.locus.domain.TrackingMode
import com.whereami.locus.domain.TrackingStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val points: List<TrackPoint> = emptyList(),
    val path: List<LatLng> = emptyList(),
    val pathSegments: List<List<LatLng>> = emptyList(),
    val currentPosition: LatLng? = null,
    val distanceMeters: Double = 0.0,
    val firstPointAt: Long? = null,
    val lastPointAt: Long? = null,
    val lastAccuracyMeters: Float? = null,
    val trackingEnabled: Boolean = false,
    val trackingMode: TrackingMode = TrackingMode.Foreground,
    val status: TrackingStatus = TrackingStatus.Paused,
    val speedMps: Float? = null,
    val headingDegrees: Float? = null,
    val compassAvailable: Boolean = false,
    val sampleIntervalMs: Long = 5_000L
)

class TodayViewModel(
    private val container: AppContainer
) : ViewModel() {
    private val dayStart = DayTime.startOfDayMillis()

    private data class CoreState(
        val points: List<TrackPoint>,
        val enabled: Boolean,
        val status: TrackingStatus,
        val speed: Float?,
        val interval: Long
    )

    private val coreState = combine(
        container.trackRepository.observePointsForDay(dayStart),
        container.trackingController.isTrackingEnabled,
        container.trackingController.status,
        container.locationSource.speedMps,
        container.settingsStore.sampleIntervalMs
    ) { points, enabled, status, speed, interval ->
        CoreState(points, enabled, status, speed, interval)
    }

    val uiState: StateFlow<TodayUiState> = combine(
        coreState,
        container.compassSensor.headingDegrees,
        container.compassSensor.available,
        container.settingsStore.trackingMode
    ) { core, heading, compassAvailable, mode ->
        val pathSegments = PathGeometry.drawableSegments(core.points)
        val path = pathSegments.flatten()
        val lastRaw = core.points.lastOrNull()
        TodayUiState(
            points = core.points,
            path = path,
            pathSegments = pathSegments,
            currentPosition = lastRaw?.let { LatLng(it.latitude, it.longitude) }
                ?: path.lastOrNull(),
            distanceMeters = PathGeometry.totalDistanceMeters(core.points),
            firstPointAt = core.points.firstOrNull()?.recordedAt,
            lastPointAt = lastRaw?.recordedAt,
            lastAccuracyMeters = lastRaw?.accuracyMeters,
            trackingEnabled = core.enabled,
            trackingMode = mode,
            status = if (core.enabled) core.status else TrackingStatus.Paused,
            speedMps = if (core.enabled) core.speed else null,
            headingDegrees = heading,
            compassAvailable = compassAvailable,
            sampleIntervalMs = core.interval
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun setTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.trackingController.setTrackingEnabled(enabled)
        }
    }

    fun setTrackingMode(mode: TrackingMode) {
        viewModelScope.launch {
            container.trackingController.setTrackingMode(mode)
        }
    }

    fun setSampleIntervalMs(intervalMs: Long) {
        container.settingsStore.setSampleIntervalMs(intervalMs)
    }

    fun deleteAll() {
        viewModelScope.launch {
            container.trackRepository.deleteAll()
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TodayViewModel(container) as T
                }
            }
    }
}
