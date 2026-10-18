package com.whereami.locus.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.whereami.locus.AppContainer
import com.whereami.locus.domain.DayTime
import com.whereami.locus.domain.LatLng
import com.whereami.locus.domain.PathGeometry
import com.whereami.locus.domain.TrackPoint
import com.whereami.locus.domain.TrackingStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val points: List<TrackPoint> = emptyList(),
    val path: List<LatLng> = emptyList(),
    val currentPosition: LatLng? = null,
    val distanceMeters: Double = 0.0,
    val firstPointAt: Long? = null,
    val lastPointAt: Long? = null,
    val lastAccuracyMeters: Float? = null,
    val trackingEnabled: Boolean = false,
    val status: TrackingStatus = TrackingStatus.Paused,
    val sampleIntervalMs: Long = 5_000L
)

class TodayViewModel(
    private val container: AppContainer
) : ViewModel() {
    private val dayStart = DayTime.startOfDayMillis()

    val uiState: StateFlow<TodayUiState> = combine(
        container.trackRepository.observePointsForDay(dayStart),
        container.trackingController.isTrackingEnabled,
        container.trackingController.status,
        container.settingsStore.sampleIntervalMs
    ) { points, enabled, status, interval ->
        val path = PathGeometry.drawablePoints(points)
        TodayUiState(
            points = points,
            path = path,
            currentPosition = path.lastOrNull(),
            distanceMeters = PathGeometry.totalDistanceMeters(points),
            firstPointAt = points.firstOrNull()?.recordedAt,
            lastPointAt = points.lastOrNull()?.recordedAt,
            lastAccuracyMeters = points.lastOrNull()?.accuracyMeters,
            trackingEnabled = enabled,
            status = if (enabled) status else TrackingStatus.Paused,
            sampleIntervalMs = interval
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun setTrackingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.trackingController.setTrackingEnabled(enabled)
        }
    }

    fun setSampleIntervalMs(intervalMs: Long) {
        viewModelScope.launch {
            container.settingsStore.setSampleIntervalMs(intervalMs)
        }
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
