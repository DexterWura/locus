package com.whereami.locus.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.whereami.locus.AppContainer
import com.whereami.locus.domain.DayDetail
import com.whereami.locus.domain.LatLng
import com.whereami.locus.domain.PlaceLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DayDetailViewModel(
    private val container: AppContainer,
    private val dayStartMillis: Long
) : ViewModel() {
    private val _detail = MutableStateFlow<DayDetail?>(null)
    val detail: StateFlow<DayDetail?> = _detail.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _detail.value = container.trackRepository.getDayDetail(dayStartMillis)
        }
    }

    fun nameStop(center: LatLng, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            container.trackRepository.upsertPlaceLabel(
                PlaceLabel(
                    name = trimmed,
                    latitude = center.latitude,
                    longitude = center.longitude
                )
            )
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
