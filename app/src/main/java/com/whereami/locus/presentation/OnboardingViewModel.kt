package com.whereami.locus.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.whereami.locus.AppContainer
import kotlinx.coroutines.flow.StateFlow

class OnboardingViewModel(
    private val container: AppContainer
) : ViewModel() {
    val onboardingComplete: StateFlow<Boolean> = container.settingsStore.onboardingComplete

    fun completeOnboarding() {
        container.settingsStore.setOnboardingComplete(true)
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OnboardingViewModel(container) as T
                }
            }
    }
}
