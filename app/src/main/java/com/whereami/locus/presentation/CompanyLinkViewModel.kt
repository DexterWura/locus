package com.whereami.locus.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.whereami.locus.AppContainer
import com.whereami.locus.data.CompanyLink
import com.whereami.locus.data.sync.LocusApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CompanyLinkViewModel(
    private val container: AppContainer
) : ViewModel() {
    val link: StateFlow<CompanyLink?> = container.companyLinkStore.link
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), container.companyLinkStore.current())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    fun pairFromQr(raw: String) {
        viewModelScope.launch {
            _busy.value = true
            _message.value = null
            try {
                val payload = LocusApiClient.parsePairingQr(raw)
                if (payload == null) {
                    _message.value = "That QR is not a Locus company link."
                    return@launch
                }
                val result = withContext(Dispatchers.IO) {
                    LocusApiClient.pair(payload.serverUrl, payload.code)
                }
                result.fold(
                    onSuccess = { paired ->
                        container.companyLinkStore.save(
                            CompanyLink(
                                serverUrl = payload.serverUrl,
                                token = paired.token,
                                assetName = paired.assetName,
                                pairedAtMillis = System.currentTimeMillis()
                            )
                        )
                        container.locationUploader.flushPending()
                        _message.value = "Linked to ${paired.assetName}."
                    },
                    onFailure = { error ->
                        _message.value = error.message ?: "Could not link to the company."
                    }
                )
            } finally {
                _busy.value = false
            }
        }
    }

    fun unlink() {
        viewModelScope.launch {
            container.companyLinkStore.clear()
            _message.value = "Company link removed. Trails stay on this phone."
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CompanyLinkViewModel(container) as T
                }
            }
    }
}
