package com.whereami.locus.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * One-shot copy from pre-encryption DataStore files into EncryptedSharedPreferences
 * so upgrades keep onboarding, auth hashes, and tracking settings.
 */
object LegacyDataStoreMigrator {
    private const val MIGRATED_FLAG = "legacy_datastore_migrated_v1"

    fun migrateSettings(context: Context, prefs: SharedPreferences) {
        if (prefs.getBoolean(MIGRATED_FLAG, false)) return
        val legacyFile = datastoreFile(context, "locus_settings")
        if (legacyFile.exists()) {
            runBlocking {
                val legacy = openLegacy(context, "locus_settings").data.first()
                val editor = prefs.edit()
                putBooleanIfAbsent(
                    editor,
                    prefs,
                    "tracking_enabled",
                    legacy[booleanPreferencesKey("tracking_enabled")]
                )
                putBooleanIfAbsent(
                    editor,
                    prefs,
                    "onboarding_complete",
                    legacy[booleanPreferencesKey("onboarding_complete")]
                )
                putLongIfAbsent(
                    editor,
                    prefs,
                    "sample_interval_ms",
                    legacy[longPreferencesKey("sample_interval_ms")]
                )
                editor.putBoolean(MIGRATED_FLAG, true).commit()
            }
            deleteDatastoreFiles(legacyFile)
        } else {
            prefs.edit().putBoolean(MIGRATED_FLAG, true).apply()
        }
    }

    fun migrateAuth(context: Context, prefs: SharedPreferences) {
        if (prefs.getBoolean(MIGRATED_FLAG, false)) return
        val legacyFile = datastoreFile(context, "locus_auth")
        if (legacyFile.exists()) {
            runBlocking {
                val legacy = openLegacy(context, "locus_auth").data.first()
                val editor = prefs.edit()
                val keys = listOf(
                    "password_salt",
                    "password_hash",
                    "answer_salt",
                    "question_1_id",
                    "question_2_id",
                    "answer_1_hash",
                    "answer_2_hash",
                    "recovery_salt",
                    "recovery_phrase_hash"
                )
                keys.forEach { key ->
                    putStringIfAbsent(editor, prefs, key, legacy[stringPreferencesKey(key)])
                }
                editor.putBoolean(MIGRATED_FLAG, true).commit()
            }
            deleteDatastoreFiles(legacyFile)
        } else {
            prefs.edit().putBoolean(MIGRATED_FLAG, true).apply()
        }
    }

    private fun putBooleanIfAbsent(
        editor: SharedPreferences.Editor,
        prefs: SharedPreferences,
        key: String,
        value: Boolean?
    ) {
        if (value == null || prefs.contains(key)) return
        editor.putBoolean(key, value)
    }

    private fun putLongIfAbsent(
        editor: SharedPreferences.Editor,
        prefs: SharedPreferences,
        key: String,
        value: Long?
    ) {
        if (value == null || prefs.contains(key)) return
        editor.putLong(key, value)
    }

    private fun putStringIfAbsent(
        editor: SharedPreferences.Editor,
        prefs: SharedPreferences,
        key: String,
        value: String?
    ) {
        if (value.isNullOrBlank() || prefs.contains(key)) return
        editor.putString(key, value)
    }

    private fun openLegacy(context: Context, name: String) =
        PreferenceDataStoreFactory.create(
            produceFile = { datastoreFile(context, name) }
        )

    private fun datastoreFile(context: Context, name: String): File =
        File(context.applicationContext.filesDir, "datastore/$name.preferences_pb")

    private fun deleteDatastoreFiles(primary: File) {
        primary.delete()
        File(primary.path + ".bak").delete()
        File(primary.path + ".tmp").delete()
    }
}
