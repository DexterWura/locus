package com.whereami.locus.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.whereami.locus.AppContainer
import com.whereami.locus.domain.DayTime
import com.whereami.locus.domain.LatLng
import com.whereami.locus.domain.PathGeometry
import com.whereami.locus.domain.PlaceLabel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PlaceListItem(
    val place: PlaceLabel,
    val distanceMeters: Double?
)

class PlacesViewModel(
    private val container: AppContainer
) : ViewModel() {
    private val dayStart = DayTime.startOfDayMillis()

    val places: StateFlow<List<PlaceListItem>> = combine(
        container.trackRepository.observePlaceLabels(),
        container.trackRepository.observePointsForDay(dayStart)
    ) { labels, points ->
        val current = points.lastOrNull()?.let { LatLng(it.latitude, it.longitude) }
        labels
            .map { label ->
                val distance = current?.let {
                    PathGeometry.haversineMeters(
                        it.latitude,
                        it.longitude,
                        label.latitude,
                        label.longitude
                    )
                }
                PlaceListItem(place = label, distanceMeters = distance)
            }
            .sortedWith(
                compareBy<PlaceListItem> { it.distanceMeters == null }
                    .thenBy { it.distanceMeters ?: Double.MAX_VALUE }
                    .thenBy { it.place.name.lowercase() }
            )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlacesViewModel(container) as T
                }
            }
    }
}
