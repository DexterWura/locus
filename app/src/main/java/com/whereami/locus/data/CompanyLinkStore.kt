package com.whereami.locus.data

import android.content.Context
import com.whereami.locus.data.security.EncryptedPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CompanyLink(
    val serverUrl: String,
    val token: String,
    val assetName: String,
    val pairedAtMillis: Long
)

class CompanyLinkStore(context: Context) {
    private val prefs = EncryptedPrefs.create(context.applicationContext, PREFS_NAME)
    private val _link = MutableStateFlow(read())
    val link: StateFlow<CompanyLink?> = _link.asStateFlow()

    fun current(): CompanyLink? = _link.value

    fun save(link: CompanyLink) {
        prefs.edit()
            .putString(KEY_SERVER, link.serverUrl.trimEnd('/'))
            .putString(KEY_TOKEN, link.token)
            .putString(KEY_ASSET_NAME, link.assetName)
            .putLong(KEY_PAIRED_AT, link.pairedAtMillis)
            .apply()
        _link.value = link.copy(serverUrl = link.serverUrl.trimEnd('/'))
    }

    fun clear() {
        prefs.edit().clear().apply()
        _link.value = null
    }

    private fun read(): CompanyLink? {
        val server = prefs.getString(KEY_SERVER, null)?.trimEnd('/')
        val token = prefs.getString(KEY_TOKEN, null)
        val assetName = prefs.getString(KEY_ASSET_NAME, null)
        val pairedAt = prefs.getLong(KEY_PAIRED_AT, 0L)
        if (server.isNullOrBlank() || token.isNullOrBlank() || assetName.isNullOrBlank()) {
            return null
        }
        return CompanyLink(
            serverUrl = server,
            token = token,
            assetName = assetName,
            pairedAtMillis = if (pairedAt > 0L) pairedAt else System.currentTimeMillis()
        )
    }

    companion object {
        private const val PREFS_NAME = "locus_company_link"
        private const val KEY_SERVER = "server_url"
        private const val KEY_TOKEN = "device_token"
        private const val KEY_ASSET_NAME = "asset_name"
        private const val KEY_PAIRED_AT = "paired_at"
    }
}
