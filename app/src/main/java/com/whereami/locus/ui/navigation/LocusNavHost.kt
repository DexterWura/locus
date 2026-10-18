package com.whereami.locus.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
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
import com.whereami.locus.presentation.DayDetailViewModel
import com.whereami.locus.presentation.HistoryViewModel
import com.whereami.locus.presentation.OnboardingViewModel
import com.whereami.locus.presentation.TodayViewModel
import com.whereami.locus.ui.history.DayDetailScreen
import com.whereami.locus.ui.history.HistoryScreen
import com.whereami.locus.ui.onboarding.OnboardingScreen
import com.whereami.locus.ui.settings.SettingsScreen
import com.whereami.locus.ui.today.TodayScreen

object Routes {
    const val Onboarding = "onboarding"
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
    val onboardingComplete by onboardingViewModel.onboardingComplete.collectAsStateWithLifecycle()

    if (onboardingComplete == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val startDestination = if (onboardingComplete == true) Routes.Today else Routes.Onboarding

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.Onboarding) {
            OnboardingScreen(
                onFinished = {
                    onboardingViewModel.completeOnboarding()
                    navController.navigate(Routes.Today) {
                        popUpTo(Routes.Onboarding) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Today) {
            val todayViewModel: TodayViewModel = viewModel(
                factory = TodayViewModel.factory(container)
            )
            val state by todayViewModel.uiState.collectAsStateWithLifecycle()
            TodayScreen(
                state = state,
                onToggleTracking = todayViewModel::setTrackingEnabled,
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
                sampleIntervalMs = state.sampleIntervalMs,
                onSampleIntervalChange = todayViewModel::setSampleIntervalMs,
                onDeleteAll = todayViewModel::deleteAll,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
