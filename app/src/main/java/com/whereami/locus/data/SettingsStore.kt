package com.whereami.locus.data

import android.content.Context
import android.content.SharedPreferences
import com.whereami.locus.data.security.EncryptedPrefs
import com.whereami.locus.data.security.LegacyDataStoreMigrator
import com.whereami.locus.domain.TrackingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        EncryptedPrefs.create(appContext, PREFS_NAME).also { secure ->
            LegacyDataStoreMigrator.migrateSettings(appContext, secure)
        }

    private val _trackingEnabled = MutableStateFlow(prefs.getBoolean(KEY_TRACKING_ENABLED, false))
    val trackingEnabled: StateFlow<Boolean> = _trackingEnabled.asStateFlow()

    private val _onboardingComplete =
        MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false))
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete.asStateFlow()

    private val _sampleIntervalMs =
        MutableStateFlow(prefs.getLong(KEY_SAMPLE_INTERVAL_MS, DEFAULT_SAMPLE_INTERVAL_MS))
    val sampleIntervalMs: StateFlow<Long> = _sampleIntervalMs.asStateFlow()

    private val _trackingMode = MutableStateFlow(readMode())
    val trackingMode: StateFlow<TrackingMode> = _trackingMode.asStateFlow()

    fun setTrackingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TRACKING_ENABLED, enabled).apply()
        _trackingEnabled.value = enabled
    }

    fun setOnboardingComplete(complete: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, complete).apply()
        _onboardingComplete.value = complete
    }

    fun setSampleIntervalMs(intervalMs: Long) {
        prefs.edit().putLong(KEY_SAMPLE_INTERVAL_MS, intervalMs).apply()
        _sampleIntervalMs.value = intervalMs
    }

    fun setTrackingMode(mode: TrackingMode) {
        prefs.edit().putString(KEY_TRACKING_MODE, mode.name).apply()
        _trackingMode.value = mode
    }

    private fun readMode(): TrackingMode {
        val raw = prefs.getString(KEY_TRACKING_MODE, TrackingMode.Foreground.name)
        return runCatching { TrackingMode.valueOf(raw!!) }.getOrDefault(TrackingMode.Foreground)
    }

    companion object {
        const val DEFAULT_SAMPLE_INTERVAL_MS = 5_000L
        const val STILL_SAMPLE_INTERVAL_MS = 30_000L
        const val MIN_DISPLACEMENT_METERS = 8f
        const val STILL_MIN_DISPLACEMENT_METERS = 20f
        const val STILL_AFTER_MS = 25_000L
        const val MOVING_MIN_DISTANCE_METERS = 8f

        private const val PREFS_NAME = "locus_settings_secure"
        private const val KEY_TRACKING_ENABLED = "tracking_enabled"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_SAMPLE_INTERVAL_MS = "sample_interval_ms"
        private const val KEY_TRACKING_MODE = "tracking_mode"
    }
}
