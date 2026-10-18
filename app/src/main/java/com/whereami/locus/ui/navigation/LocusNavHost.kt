package com.whereami.locus.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.whereami.locus.AppContainer
import com.whereami.locus.presentation.AuthViewModel
import com.whereami.locus.presentation.DayDetailViewModel
import com.whereami.locus.presentation.HistoryViewModel
import com.whereami.locus.presentation.OnboardingViewModel
import com.whereami.locus.presentation.TodayViewModel
import com.whereami.locus.ui.auth.LockScreen
import com.whereami.locus.ui.auth.RecoveryScreen
import com.whereami.locus.ui.history.DayDetailScreen
import com.whereami.locus.ui.history.HistoryScreen
import com.whereami.locus.ui.onboarding.OnboardingScreen
import com.whereami.locus.ui.settings.SettingsScreen
import com.whereami.locus.ui.today.TodayScreen

object Routes {
    const val Onboarding = "onboarding"
    const val Lock = "lock"
    const val Recover = "recover"
    const val Today = "today"
    const val History = "history"
    const val DayDetail = "day/{dayStart}"
    const val Settings = "settings"

    fun dayDetail(dayStart: Long) = "day/$dayStart"
}

@Composable
fun LocusNavHost(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val onboardingViewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.factory(container)
    )
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.factory(container)
    )
    val onboardingComplete by onboardingViewModel.onboardingComplete.collectAsStateWithLifecycle()
    val gate by authViewModel.gateState.collectAsStateWithLifecycle()
    val authError by authViewModel.error.collectAsStateWithLifecycle()
    val recoveryQuestions by authViewModel.recoveryQuestions.collectAsStateWithLifecycle()
    val recoveryAuthorized by authViewModel.recoveryAuthorized.collectAsStateWithLifecycle()

    if (gate.loading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val startDestination = when {
        !onboardingComplete -> Routes.Onboarding
        gate.passwordSet && !gate.unlocked -> Routes.Lock
        else -> Routes.Today
    }

    LaunchedEffect(gate.unlocked, gate.passwordSet, onboardingComplete) {
        if (onboardingComplete && gate.passwordSet && !gate.unlocked) {
            val current = navController.currentDestination?.route
            if (current != Routes.Lock && current != Routes.Recover && current != Routes.Onboarding) {
                navController.navigate(Routes.Lock) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.Onboarding) {
            OnboardingScreen(
                authError = authError,
                onClearAuthError = authViewModel::clearError,
                onGenerateRecoveryPhrase = authViewModel::newRecoveryPhrase,
                onSetupAuth = { password, confirm, q1, q2, a1, a2, phrase, onDone ->
                    authViewModel.setupAuth(
                        password = password,
                        confirm = confirm,
                        question1Id = q1,
                        question2Id = q2,
                        answer1 = a1,
                        answer2 = a2,
                        recoveryPhrase = phrase,
                        onDone = onDone
                    )
                },
                onFinished = {
                    onboardingViewModel.completeOnboarding()
                    navController.navigate(Routes.Today) {
                        popUpTo(Routes.Onboarding) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Lock) {
            LockScreen(
                error = authError,
                onClearError = authViewModel::clearError,
                onUnlock = authViewModel::unlockWithPassword,
                onForgotPassword = {
                    authViewModel.clearError()
                    navController.navigate(Routes.Recover)
                }
            )
            LaunchedEffect(gate.unlocked) {
                if (gate.unlocked) {
                    navController.navigate(Routes.Today) {
                        popUpTo(Routes.Lock) { inclusive = true }
                    }
                }
            }
        }
        composable(Routes.Recover) {
            RecoveryScreen(
                questions = recoveryQuestions,
                recoveryAuthorized = recoveryAuthorized,
                error = authError,
                onClearError = authViewModel::clearError,
                onLoadQuestions = authViewModel::loadRecoveryQuestions,
                onRecoverWithAnswers = authViewModel::recoverWithAnswers,
                onRecoverWithPhrase = authViewModel::recoverWithPhrase,
                onResetPassword = authViewModel::resetPassword,
                onBack = { navController.popBackStack() }
            )
            LaunchedEffect(gate.unlocked) {
                if (gate.unlocked) {
                    navController.navigate(Routes.Today) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
        composable(Routes.Today) {
            val todayViewModel: TodayViewModel = viewModel(
                factory = TodayViewModel.factory(container)
            )
            val state by todayViewModel.uiState.collectAsStateWithLifecycle()
            TodayScreen(
                state = state,
                onOpenHistory = { navController.navigate(Routes.History) },
                onOpenSettings = { navController.navigate(Routes.Settings) }
            )
        }
        composable(Routes.History) {
            val historyViewModel: HistoryViewModel = viewModel(
                factory = HistoryViewModel.factory(container)
            )
            val days by historyViewModel.days.collectAsStateWithLifecycle()
            HistoryScreen(
                days = days,
                onBack = { navController.popBackStack() },
                onOpenDay = { dayStart ->
                    navController.navigate(Routes.dayDetail(dayStart))
                }
            )
        }
        composable(
            route = Routes.DayDetail,
            arguments = listOf(navArgument("dayStart") { type = NavType.LongType })
        ) { entry ->
            val dayStart = entry.arguments?.getLong("dayStart") ?: return@composable
            val dayViewModel: DayDetailViewModel = viewModel(
                factory = DayDetailViewModel.factory(container, dayStart)
            )
            val detail by dayViewModel.detail.collectAsStateWithLifecycle()
            DayDetailScreen(
                detail = detail,
                onBack = { navController.popBackStack() },
                onNameStop = dayViewModel::nameStop
            )
        }
        composable(Routes.Settings) {
            val todayViewModel: TodayViewModel = viewModel(
                factory = TodayViewModel.factory(container)
            )
            val state by todayViewModel.uiState.collectAsStateWithLifecycle()
            SettingsScreen(
                trackingEnabled = state.trackingEnabled,
                onToggleTracking = todayViewModel::setTrackingEnabled,
                trackingMode = state.trackingMode,
                onTrackingModeChange = todayViewModel::setTrackingMode,
                sampleIntervalMs = state.sampleIntervalMs,
                onSampleIntervalChange = todayViewModel::setSampleIntervalMs,
                onDeleteAll = todayViewModel::deleteAll,
                authError = authError,
                onClearAuthError = authViewModel::clearError,
                onChangePassword = { current, newPassword, confirm, onSuccess ->
                    authViewModel.changePassword(current, newPassword, confirm, onSuccess)
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
