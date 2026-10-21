package com.whereami.locus.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.whereami.locus.AppContainer
import com.whereami.locus.domain.DayDetail
import com.whereami.locus.domain.LatLng
import com.whereami.locus.domain.PathGeometry
import com.whereami.locus.domain.PlaceLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DayDetailViewModel(
    private val container: AppContainer,
    private val dayStartMillis: Long
) : ViewModel() {
    private val _detail = MutableStateFlow<DayDetail?>(null)
    val detail: StateFlow<DayDetail?> = _detail.asStateFlow()

    val places: StateFlow<List<PlaceLabel>> = container.trackRepository
        .observePlaceLabels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refresh()
        viewModelScope.launch {
            container.trackRepository.observePlaceLabels().collect {
                refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _detail.value = container.trackRepository.getDayDetail(dayStartMillis)
        }
    }

    fun nameStop(center: LatLng, name: String, note: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val trimmedNote = note?.trim()?.ifEmpty { null }
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
                        latitude = center.latitude,
                        longitude = center.longitude,
                        note = trimmedNote,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
            refresh()
        }
    }

    fun savePlace(latitude: Double, longitude: Double, name: String, note: String?) {
        nameStop(LatLng(latitude, longitude), name, note)
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
            refresh()
        }
    }

    fun deletePlace(id: Long) {
        if (id == 0L) return
        viewModelScope.launch {
            container.trackRepository.deletePlaceLabel(id)
            refresh()
        }
    }

    companion object {
        fun factory(container: AppContainer, dayStartMillis: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DayDetailViewModel(container, dayStartMillis) as T
                }
            }
    }
}
