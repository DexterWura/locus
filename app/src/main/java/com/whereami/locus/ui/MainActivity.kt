package com.whereami.locus.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whereami.locus.AppContainer
import com.whereami.locus.LocusApp
import com.whereami.locus.ui.navigation.LocusNavHost
import com.whereami.locus.ui.theme.LocusTheme

class MainActivity : ComponentActivity() {
    private lateinit var container: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        container = (application as LocusApp).container
        setContent {
            val unlocked by container.authSession.isUnlocked.collectAsStateWithLifecycle()
            val onboardingDone by container.settingsStore.onboardingComplete.collectAsStateWithLifecycle()
            val passwordSet by container.authStore.isPasswordSet.collectAsStateWithLifecycle()
            val needsSecure = !onboardingDone || (passwordSet && !unlocked)

            SideEffect {
                if (needsSecure) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            LocusTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LocusNavHost(container = container)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        container.compassSensor.start()
        container.trackingController.onAppForeground()
    }

    override fun onStop() {
        container.trackingController.onAppBackground()
        container.compassSensor.stop()
        super.onStop()
    }
}
