package com.whereami.locus.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.whereami.locus.AppContainer
import com.whereami.locus.auth.PasswordCrypto
import com.whereami.locus.auth.SecurityQuestion
import com.whereami.locus.auth.SecurityQuestions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthGateState(
    val loading: Boolean = true,
    val passwordSet: Boolean = false,
    val unlocked: Boolean = false
)

class AuthViewModel(
    private val container: AppContainer
) : ViewModel() {
    val gateState: StateFlow<AuthGateState> = combine(
        container.authStore.isPasswordSet,
        container.authSession.isUnlocked
    ) { passwordSet, unlocked ->
        AuthGateState(loading = false, passwordSet = passwordSet, unlocked = unlocked)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AuthGateState()
    )

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _recoveryQuestions = MutableStateFlow<Pair<SecurityQuestion, SecurityQuestion>?>(null)
    val recoveryQuestions: StateFlow<Pair<SecurityQuestion, SecurityQuestion>?> =
        _recoveryQuestions.asStateFlow()

    private val _recoveryAuthorized = MutableStateFlow(false)
    val recoveryAuthorized: StateFlow<Boolean> = _recoveryAuthorized.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    fun unlockWithPassword(password: String) {
        viewModelScope.launch {
            _error.value = null
            if (password.isBlank()) {
                _error.value = "Enter your password"
                return@launch
            }
            if (container.authStore.verifyPassword(password)) {
                container.authSession.unlock()
            } else {
                _error.value = "Incorrect password"
            }
        }
    }

    fun loadRecoveryQuestions() {
        viewModelScope.launch {
            val ids = container.authStore.questionIds()
            if (ids == null) {
                _error.value = "No recovery questions found"
                return@launch
            }
            val q1 = SecurityQuestions.byId(ids.first)
            val q2 = SecurityQuestions.byId(ids.second)
            if (q1 == null || q2 == null) {
                _error.value = "Recovery questions unavailable"
                return@launch
            }
            _recoveryQuestions.value = q1 to q2
        }
    }

    fun recoverWithAnswers(answer1: String, answer2: String) {
        viewModelScope.launch {
            _error.value = null
            if (answer1.isBlank() || answer2.isBlank()) {
                _error.value = "Answer both questions"
                return@launch
            }
            if (container.authStore.verifySecurityAnswers(answer1, answer2)) {
                _recoveryAuthorized.value = true
            } else {
                _error.value = "Answers do not match"
            }
        }
    }

    fun recoverWithPhrase(phrase: String) {
        viewModelScope.launch {
            _error.value = null
            if (phrase.isBlank()) {
                _error.value = "Enter your recovery phrase"
                return@launch
            }
            if (container.authStore.verifyRecoveryPhrase(phrase)) {
                _recoveryAuthorized.value = true
            } else {
                _error.value = "Recovery phrase does not match"
            }
        }
    }

    fun resetPassword(newPassword: String, confirm: String) {
        viewModelScope.launch {
            _error.value = null
            val validation = validatePassword(newPassword, confirm)
            if (validation != null) {
                _error.value = validation
                return@launch
            }
            if (!_recoveryAuthorized.value) {
                _error.value = "Verify recovery first"
                return@launch
            }
            container.authStore.updatePassword(newPassword)
            _recoveryAuthorized.value = false
            container.authSession.unlock()
        }
    }

    fun changePassword(
        current: String,
        newPassword: String,
        confirm: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _error.value = null
            if (!container.authStore.verifyPassword(current)) {
                _error.value = "Current password is incorrect"
                return@launch
            }
            val validation = validatePassword(newPassword, confirm)
            if (validation != null) {
                _error.value = validation
                return@launch
            }
            container.authStore.updatePassword(newPassword)
            onSuccess()
        }
    }

    fun setupAuth(
        password: String,
        confirm: String,
        question1Id: String,
        question2Id: String,
        answer1: String,
        answer2: String,
        recoveryPhrase: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _error.value = null
            val validation = validatePassword(password, confirm)
            if (validation != null) {
                _error.value = validation
                return@launch
            }
            if (question1Id == question2Id) {
                _error.value = "Pick two different questions"
                return@launch
            }
            if (answer1.isBlank() || answer2.isBlank()) {
                _error.value = "Answer both security questions"
                return@launch
            }
            container.authStore.setup(
                password = password,
                question1Id = question1Id,
                question2Id = question2Id,
                answer1 = answer1,
                answer2 = answer2,
                recoveryPhrase = recoveryPhrase
            )
            container.authSession.unlock()
            onDone()
        }
    }

    fun newRecoveryPhrase(): String = PasswordCrypto.generateRecoveryPhrase()

    companion object {
        fun validatePassword(password: String, confirm: String): String? {
            if (password.length < 6) return "Password must be at least 6 characters"
            if (password != confirm) return "Passwords do not match"
            return null
        }

        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(container) as T
                }
            }
    }
}
