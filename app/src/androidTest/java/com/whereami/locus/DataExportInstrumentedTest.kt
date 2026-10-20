package com.whereami.locus

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.whereami.locus.data.db.LocusDatabase
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Runs inside the already-installed app process so Keystore-backed data can be
 * exported before a debug-signing replacement install.
 */
@RunWith(AndroidJUnit4::class)
class DataExportInstrumentedTest {
    @Test
    fun exportAllUserData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val app = context as LocusApp
        val database = LocusDatabase.get(app)
        val points = runBlocking { database.trackPointDao().getAll() }
        val labels = runBlocking { database.placeLabelDao().getAll() }
        val settings = app.container.settingsStore
        val auth = app.container.authStore.getStoredAuth()
        val link = app.container.companyLinkStore.current()

        val root = JSONObject()
            .put("version", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put(
                "settings",
                JSONObject()
                    .put("trackingEnabled", settings.trackingEnabled.value)
                    .put("onboardingComplete", settings.onboardingComplete.value)
                    .put("sampleIntervalMs", settings.sampleIntervalMs.value)
                    .put("trackingMode", settings.trackingMode.value.name)
            )

        if (link != null) {
            root.put(
                "companyLink",
                JSONObject()
                    .put("serverUrl", link.serverUrl)
                    .put("token", link.token)
                    .put("assetName", link.assetName)
                    .put("pairedAtMillis", link.pairedAtMillis)
            )
        }
        if (auth != null) {
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
            )
        }
        root.put("labels", labelArray)

        val outDir = context.getExternalFilesDir(null) ?: context.filesDir
        val out = File(outDir, "locus_migrate_export.json")
        out.writeText(root.toString())
        assertTrue("export missing", out.exists() && out.length() > 2L)

        // Also copy where adb can always pull without run-as after process exit.
        val downloadCopy = File("/storage/emulated/0/Download/locus_migrate_export.json")
        runCatching {
            downloadCopy.parentFile?.mkdirs()
            downloadCopy.writeText(root.toString())
        }
    }

    private fun b64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)
}
