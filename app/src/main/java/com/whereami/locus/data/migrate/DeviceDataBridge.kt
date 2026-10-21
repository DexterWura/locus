package com.whereami.locus.data.migrate

import android.content.Context
import android.util.Base64
import android.util.Log
import com.whereami.locus.auth.AuthStore
import com.whereami.locus.data.CompanyLink
import com.whereami.locus.data.CompanyLinkStore
import com.whereami.locus.data.SettingsStore
import com.whereami.locus.data.db.LocusDatabase
import com.whereami.locus.data.db.PlaceLabelEntity
import com.whereami.locus.data.db.TrackPointEntity
import com.whereami.locus.domain.TrackingMode
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Exports/imports trail + link + settings + auth hashes as JSON so a debug
 * signature change can be survived (Android Keystore keys do not transfer).
 */
object DeviceDataBridge {
    const val EXPORT_FILE_NAME = "locus_migrate_export.json"
    private const val TAG = "DeviceDataBridge"
    private const val FORMAT_VERSION = 1

    fun exportFileCandidates(context: Context): List<File> = listOf(
        File(context.getExternalFilesDir(null), EXPORT_FILE_NAME),
        File("/storage/emulated/0/Download", EXPORT_FILE_NAME),
        File(context.filesDir, EXPORT_FILE_NAME)
    )

    fun exportToExternalFiles(
        context: Context,
        database: LocusDatabase,
        settingsStore: SettingsStore,
        authStore: AuthStore,
        companyLinkStore: CompanyLinkStore
    ): File {
        val json = buildExportJson(database, settingsStore, authStore, companyLinkStore)
        val file = File(context.getExternalFilesDir(null), EXPORT_FILE_NAME)
        file.parentFile?.mkdirs()
        file.writeText(json)
        Log.i(TAG, "Exported migration payload to ${file.absolutePath} (${file.length()} bytes)")
        return file
    }

    fun importIfPresent(
        context: Context,
        database: LocusDatabase,
        settingsStore: SettingsStore,
        authStore: AuthStore,
        companyLinkStore: CompanyLinkStore
    ): Boolean {
        val source = exportFileCandidates(context).firstOrNull { it.exists() && it.length() > 0L }
            ?: return false
        return try {
            applyImport(source.readText(), database, settingsStore, authStore, companyLinkStore)
            source.delete()
            exportFileCandidates(context).forEach { candidate ->
                if (candidate.exists() && candidate != source) candidate.delete()
            }
            Log.i(TAG, "Imported migration payload from ${source.absolutePath}")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Migration import failed", t)
            false
        }
    }

    private fun buildExportJson(
        database: LocusDatabase,
        settingsStore: SettingsStore,
        authStore: AuthStore,
        companyLinkStore: CompanyLinkStore
    ): String {
        val points = runBlocking { database.trackPointDao().getAll() }
        val labels = runBlocking { database.placeLabelDao().getAll() }
        val root = JSONObject()
        root.put("version", FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put(
            "settings",
            JSONObject()
                .put("trackingEnabled", settingsStore.trackingEnabled.value)
                .put("onboardingComplete", settingsStore.onboardingComplete.value)
                .put("sampleIntervalMs", settingsStore.sampleIntervalMs.value)
                .put("trackingMode", settingsStore.trackingMode.value.name)
        )
        companyLinkStore.current()?.let { link ->
            root.put(
                "companyLink",
                JSONObject()
                    .put("serverUrl", link.serverUrl)
                    .put("token", link.token)
                    .put("assetName", link.assetName)
                    .put("pairedAtMillis", link.pairedAtMillis)
            )
        }
        authStore.getStoredAuth()?.let { auth ->
            root.put(
                "auth",
                JSONObject()
                    .put("passwordSalt", b64(auth.passwordSalt))
                    .put("passwordHash", b64(auth.passwordHash))
                    .put("answerSalt", b64(auth.answerSalt))
                    .put("question1Id", auth.question1Id)
                    .put("question2Id", auth.question2Id)
                    .put("answer1Hash", b64(auth.answer1Hash))
                    .put("answer2Hash", b64(auth.answer2Hash))
                    .put("recoverySalt", b64(auth.recoverySalt))
                    .put("recoveryPhraseHash", b64(auth.recoveryPhraseHash))
            )
        }
        val pointArray = JSONArray()
        points.forEach { point ->
            pointArray.put(
                JSONObject()
                    .put("latitude", point.latitude)
                    .put("longitude", point.longitude)
                    .put("accuracyMeters", point.accuracyMeters.toDouble())
                    .put("altitudeMeters", point.altitudeMeters)
                    .put("speedMetersPerSecond", point.speedMetersPerSecond?.toDouble())
                    .put("bearingDegrees", point.bearingDegrees?.toDouble())
                    .put("recordedAt", point.recordedAt)
                    .put("uploadedAt", point.uploadedAt)
            )
        }
        root.put("points", pointArray)
        val labelArray = JSONArray()
        labels.forEach { label ->
            labelArray.put(
                JSONObject()
                    .put("name", label.name)
                    .put("latitude", label.latitude)
                    .put("longitude", label.longitude)
                    .put("radiusMeters", label.radiusMeters.toDouble())
                    .put("note", label.note)
                    .put("createdAt", label.createdAt)
            )
        }
        root.put("labels", labelArray)
        return root.toString()
    }

    private fun applyImport(
        raw: String,
        database: LocusDatabase,
        settingsStore: SettingsStore,
        authStore: AuthStore,
        companyLinkStore: CompanyLinkStore
    ) {
        val root = JSONObject(raw)
        root.optJSONObject("settings")?.let { settings ->
            settingsStore.setOnboardingComplete(settings.optBoolean("onboardingComplete", true))
            settingsStore.setTrackingEnabled(settings.optBoolean("trackingEnabled", false))
            settingsStore.setSampleIntervalMs(
                settings.optLong("sampleIntervalMs", SettingsStore.DEFAULT_SAMPLE_INTERVAL_MS)
            )
            val mode = settings.optString("trackingMode", TrackingMode.Foreground.name)
            settingsStore.setTrackingMode(
                runCatching { TrackingMode.valueOf(mode) }.getOrDefault(TrackingMode.Foreground)
            )
        }
        root.optJSONObject("companyLink")?.let { link ->
            companyLinkStore.save(
                CompanyLink(
                    serverUrl = link.getString("serverUrl"),
                    token = link.getString("token"),
                    assetName = link.getString("assetName"),
                    pairedAtMillis = link.optLong("pairedAtMillis", System.currentTimeMillis())
                )
            )
        }
        root.optJSONObject("auth")?.let { auth ->
            authStore.restoreFromExport(
                passwordSalt = b64Decode(auth.getString("passwordSalt")),
                passwordHash = b64Decode(auth.getString("passwordHash")),
                answerSalt = b64Decode(auth.getString("answerSalt")),
                question1Id = auth.getString("question1Id"),
                question2Id = auth.getString("question2Id"),
                answer1Hash = b64Decode(auth.getString("answer1Hash")),
                answer2Hash = b64Decode(auth.getString("answer2Hash")),
                recoverySalt = b64Decode(auth.getString("recoverySalt")),
                recoveryPhraseHash = b64Decode(auth.getString("recoveryPhraseHash"))
            )
        }
        runBlocking {
            val existing = database.trackPointDao().getAll()
            if (existing.isEmpty()) {
                val points = root.optJSONArray("points") ?: JSONArray()
                for (i in 0 until points.length()) {
                    val p = points.getJSONObject(i)
                    database.trackPointDao().insert(
                        TrackPointEntity(
                            latitude = p.getDouble("latitude"),
                            longitude = p.getDouble("longitude"),
                            accuracyMeters = p.getDouble("accuracyMeters").toFloat(),
                            altitudeMeters = if (p.isNull("altitudeMeters")) null else p.getDouble("altitudeMeters"),
                            speedMetersPerSecond = if (p.isNull("speedMetersPerSecond")) {
                                null
                            } else {
                                p.getDouble("speedMetersPerSecond").toFloat()
                            },
                            bearingDegrees = if (p.isNull("bearingDegrees")) {
                                null
                            } else {
                                p.getDouble("bearingDegrees").toFloat()
                            },
                            recordedAt = p.getLong("recordedAt"),
                            uploadedAt = if (p.isNull("uploadedAt")) null else p.getLong("uploadedAt")
                        )
                    )
                }
            }
            val labels = root.optJSONArray("labels") ?: JSONArray()
            if (database.placeLabelDao().getAll().isEmpty()) {
                for (i in 0 until labels.length()) {
                    val label = labels.getJSONObject(i)
                    database.placeLabelDao().upsert(
                        PlaceLabelEntity(
                            name = label.getString("name"),
                            latitude = label.getDouble("latitude"),
                            longitude = label.getDouble("longitude"),
                            radiusMeters = label.getDouble("radiusMeters").toFloat(),
                            note = if (!label.has("note") || label.isNull("note")) {
                                null
                            } else {
                                label.getString("note")
                            },
                            createdAt = if (!label.has("createdAt") || label.isNull("createdAt")) {
                                null
                            } else {
                                label.getLong("createdAt")
                            }
                        )
                    )
                }
            }
        }
    }

    private fun b64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun b64Decode(value: String): ByteArray =
        Base64.decode(value, Base64.NO_WRAP)
}
