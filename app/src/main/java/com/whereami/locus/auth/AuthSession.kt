package com.whereami.locus.auth

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory unlock for the current process. Locks again when the app
 * leaves the foreground so the trail is not left open on the recents screen.
 */
class AuthSession {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var observing = false

    fun unlock() {
        _isUnlocked.value = true
    }

    fun lock() {
        _isUnlocked.value = false
    }

    fun observeProcessLifecycle() {
        if (observing) return
        observing = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                lock()
            }
        })
    }
}
