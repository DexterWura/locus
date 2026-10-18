package com.whereami.locus.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.whereami.locus.LocusApp
import com.whereami.locus.ui.navigation.LocusNavHost
import com.whereami.locus.ui.theme.LocusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LocusApp).container
        setContent {
            LocusTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LocusNavHost(container = container)
                }
            }
        }
    }
}
