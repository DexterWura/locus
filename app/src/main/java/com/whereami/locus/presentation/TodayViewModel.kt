package com.whereami.locus.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.whereami.locus.AppContainer
import com.whereami.locus.domain.DayTime
import com.whereami.locus.domain.LatLng
import com.whereami.locus.domain.PathGeometry
import com.whereami.locus.domain.PlaceLabel
import com.whereami.locus.domain.TrackPoint
import com.whereami.locus.domain.TrackingMode
import com.whereami.locus.domain.TrackingStatus
import kotlinx.coroutines.flow.MutableStateFlow
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
    val sampleIntervalMs: Long = 5_000L,
    val places: List<PlaceLabel> = emptyList(),
    val currentPlace: PlaceLabel? = null,
    val focusTarget: LatLng? = null,
    val focusToken: Int = 0
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

    private val _focusTarget = MutableStateFlow<LatLng?>(null)
    private val _focusToken = MutableStateFlow(0)

    private val sensorState = combine(
        container.compassSensor.headingDegrees,
        container.compassSensor.available,
        container.settingsStore.trackingMode,
        container.trackRepository.observePlaceLabels()
    ) { heading, compassAvailable, mode, places ->
        SensorPlaceState(heading, compassAvailable, mode, places)
    }

    private data class SensorPlaceState(
        val heading: Float?,
        val compassAvailable: Boolean,
        val mode: TrackingMode,
        val places: List<PlaceLabel>
    )

    val uiState: StateFlow<TodayUiState> = combine(
        coreState,
        sensorState,
        _focusTarget,
        _focusToken
    ) { core, sensors, focusTarget, focusToken ->
        val pathSegments = PathGeometry.drawableSegments(core.points)
        val path = pathSegments.flatten()
        val lastRaw = core.points.lastOrNull()
        val currentPosition = lastRaw?.let { LatLng(it.latitude, it.longitude) }
            ?: path.lastOrNull()
        val currentPlace = currentPosition?.let {
            PathGeometry.nearestPlaceWithinRadius(it, sensors.places)
        }
        TodayUiState(
            points = core.points,
            path = path,
            pathSegments = pathSegments,
            currentPosition = currentPosition,
            distanceMeters = PathGeometry.totalDistanceMeters(core.points),
            firstPointAt = core.points.firstOrNull()?.recordedAt,
            lastPointAt = lastRaw?.recordedAt,
            lastAccuracyMeters = lastRaw?.accuracyMeters,
            trackingEnabled = core.enabled,
            trackingMode = sensors.mode,
            status = if (core.enabled) core.status else TrackingStatus.Paused,
            speedMps = if (core.enabled) core.speed else null,
            headingDegrees = sensors.heading,
            compassAvailable = sensors.compassAvailable,
            sampleIntervalMs = core.interval,
            places = sensors.places,
            currentPlace = currentPlace,
            focusTarget = focusTarget,
            focusToken = focusToken
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

    fun savePlace(latitude: Double, longitude: Double, name: String, note: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val trimmedNote = note?.trim()?.ifEmpty { null }
            val center = LatLng(latitude, longitude)
            val nearby = PathGeometry.nearestPlaceWithinRadius(
                center,
                container.trackRepository.getPlaceLabels()
            )
            if (nearby != null) {
                container.trackRepository.upsertPlaceLabel(
                    nearby.copy(name = trimmed, note = trimmedNote ?: nearby.note)
                )
            } else {
                container.trackRepository.upsertPlaceLabel(
                    PlaceLabel(
                        name = trimmed,
                        latitude = latitude,
                        longitude = longitude,
                        note = trimmedNote,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun updatePlace(place: PlaceLabel, name: String, note: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || place.id == 0L) return
        viewModelScope.launch {
            container.trackRepository.upsertPlaceLabel(
                place.copy(
                    name = trimmed,
                    note = note?.trim()?.ifEmpty { null }
                )
            )
        }
    }

    fun deletePlace(id: Long) {
        if (id == 0L) return
        viewModelScope.launch {
            container.trackRepository.deletePlaceLabel(id)
        }
    }

    fun focusPlace(place: PlaceLabel) {
        focusAt(place.latitude, place.longitude)
    }

    fun focusAt(latitude: Double, longitude: Double) {
        _focusTarget.value = LatLng(latitude, longitude)
        _focusToken.value = _focusToken.value + 1
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
