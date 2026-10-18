package com.whereami.locus.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "locus_settings")

class SettingsStore(private val context: Context) {
    private val trackingEnabledKey = booleanPreferencesKey("tracking_enabled")
    private val onboardingCompleteKey = booleanPreferencesKey("onboarding_complete")
    private val sampleIntervalMsKey = longPreferencesKey("sample_interval_ms")

    val trackingEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[trackingEnabledKey] ?: false
    }

    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[onboardingCompleteKey] ?: false
    }

    val sampleIntervalMs: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[sampleIntervalMsKey] ?: DEFAULT_SAMPLE_INTERVAL_MS
    }

    suspend fun setTrackingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[trackingEnabledKey] = enabled }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[onboardingCompleteKey] = complete }
    }

    suspend fun setSampleIntervalMs(intervalMs: Long) {
        context.dataStore.edit { it[sampleIntervalMsKey] = intervalMs }
    }

    companion object {
        const val DEFAULT_SAMPLE_INTERVAL_MS = 5_000L
        const val STILL_SAMPLE_INTERVAL_MS = 30_000L
        const val MIN_DISPLACEMENT_METERS = 8f
        /** How long without meaningful movement before UI shows Still. */
        const val STILL_AFTER_MS = 45_000L
    }
}
