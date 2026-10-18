package com.whereami.locus.auth

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * In-memory unlock for the current process. After the app leaves the foreground,
 * stays unlocked for [UNLOCK_GRACE_MS] so brief app switches do not demand the password.
 */
class AuthSession {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lockJob: Job? = null
    private var observing = false

    fun unlock() {
        lockJob?.cancel()
        lockJob = null
        _isUnlocked.value = true
    }

    fun lock() {
        lockJob?.cancel()
        lockJob = null
        _isUnlocked.value = false
    }

    fun observeProcessLifecycle() {
        if (observing) return
        observing = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                lockJob?.cancel()
                lockJob = null
            }

            override fun onStop(owner: LifecycleOwner) {
                if (!_isUnlocked.value) return
                lockJob?.cancel()
                lockJob = scope.launch {
                    delay(UNLOCK_GRACE_MS)
                    lock()
                }
            }
        })
    }

    companion object {
        /** Stay unlocked this long after leaving the app (recents, another app, etc.). */
        const val UNLOCK_GRACE_MS = 10 * 60 * 1000L
    }
}
