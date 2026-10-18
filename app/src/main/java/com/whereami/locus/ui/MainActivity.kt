package com.whereami.locus.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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
    }

    override fun onStop() {
        container.compassSensor.stop()
        super.onStop()
    }
}
