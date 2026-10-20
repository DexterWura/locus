package com.whereami.locus.data.sync

import com.whereami.locus.domain.TrackPoint
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter

sealed class UploadResult {
    data class Success(val accepted: Int) : UploadResult()
    data object Unauthorized : UploadResult()
    data class Failed(val message: String) : UploadResult()
}

data class PairResult(
    val token: String,
    val assetName: String
)

data class PairingQrPayload(
    val serverUrl: String,
    val code: String
)

object LocusApiClient {
    fun parsePairingQr(raw: String): PairingQrPayload? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return try {
            val json = JSONObject(trimmed)
            if (json.optString("app") != "locus") return null
            val server = json.optString("server").trim().trimEnd('/')
            val code = json.optString("code").trim()
            if (server.isEmpty() || code.isEmpty()) return null
            if (!server.startsWith("https://") && !server.startsWith("http://")) return null
            PairingQrPayload(serverUrl = server, code = code)
        } catch (_: Exception) {
            null
        }
    }

    fun pair(serverUrl: String, code: String): Result<PairResult> {
        return try {
            val body = JSONObject().put("code", code).toString()
            val response = postJson(
                url = "${serverUrl.trimEnd('/')}/api/devices/locus/pair",
                body = body,
                bearerToken = null
            )
            when (response.code) {
                in 200..299 -> {
                    val root = JSONObject(response.body)
                    val data = root.optJSONObject("data")
                        ?: return Result.failure(IllegalStateException("Missing data in pair response"))
                    val token = data.optString("token")
                    val assetName = data.optString("asset_name")
                    if (token.isBlank() || assetName.isBlank()) {
                        Result.failure(IllegalStateException("Incomplete pair response"))
                    } else {
                        Result.success(PairResult(token = token, assetName = assetName))
                    }
                }
                else -> {
                    val message = runCatching {
                        JSONObject(response.body).optString("message")
                    }.getOrNull()?.takeIf { it.isNotBlank() }
                        ?: "Pairing failed (${response.code})"
                    Result.failure(IllegalStateException(message))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun uploadPositions(
        serverUrl: String,
        token: String,
        points: List<TrackPoint>
    ): UploadResult {
        if (points.isEmpty()) return UploadResult.Success(0)
        return try {
            val array = JSONArray()
            points.forEach { point ->
                val item = JSONObject()
                    .put("latitude", point.latitude)
                    .put("longitude", point.longitude)
                    .put("accuracy_meters", point.accuracyMeters.toDouble())
                    .put(
                        "recorded_at",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(point.recordedAt))
                    )
                if (point.altitudeMeters != null) {
                    item.put("altitude_meters", point.altitudeMeters)
                }
                if (point.speedMetersPerSecond != null) {
                    item.put("speed_meters_per_second", point.speedMetersPerSecond.toDouble())
                }
                if (point.bearingDegrees != null) {
                    item.put("bearing_degrees", point.bearingDegrees.toDouble())
                }
                array.put(item)
            }
            val body = JSONObject().put("points", array).toString()
            val response = postJson(
                url = "${serverUrl.trimEnd('/')}/api/devices/locus/positions",
                body = body,
                bearerToken = token
            )
            when (response.code) {
                401 -> UploadResult.Unauthorized
                in 200..299 -> {
                    val accepted = runCatching {
                        JSONObject(response.body)
                            .optJSONObject("data")
                            ?.optInt("accepted", points.size)
                    }.getOrNull() ?: points.size
                    UploadResult.Success(accepted)
                }
                else -> {
                    val message = runCatching {
                        JSONObject(response.body).optString("message")
                    }.getOrNull()?.takeIf { it.isNotBlank() }
                        ?: "Upload failed (${response.code})"
                    UploadResult.Failed(message)
                }
            }
        } catch (e: Exception) {
            UploadResult.Failed(e.message ?: "Network error")
        }
    }

    private data class HttpResponse(val code: Int, val body: String)

    private fun postJson(url: String, body: String, bearerToken: String?): HttpResponse {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 30_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            if (!bearerToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
        }
        try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body)
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            return HttpResponse(code, responseBody)
        } finally {
            connection.disconnect()
        }
    }
}
