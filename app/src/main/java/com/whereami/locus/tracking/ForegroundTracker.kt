package com.whereami.locus.tracking

import com.whereami.locus.data.SettingsStore
import com.whereami.locus.domain.TrackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * In-process location collection used when tracking mode is Foreground
 * (no sticky notification).
 */
class ForegroundTracker(
    private val locationSource: LocationSource,
    private val trackRepository: TrackRepository,
    private val settingsStore: SettingsStore,
    private val scope: CoroutineScope
) {
    private var job: Job? = null

    val isRunning: Boolean get() = job?.isActive == true

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            val interval = settingsStore.sampleIntervalMs.value
            locationSource.observeLocations(interval).collect { point ->
                trackRepository.insertPoint(point)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        locationSource.markPaused()
    }
}
