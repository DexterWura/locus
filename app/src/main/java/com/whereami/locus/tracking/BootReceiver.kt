package com.whereami.locus.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.whereami.locus.LocusApp
import com.whereami.locus.domain.TrackingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as LocusApp
                val settings = app.container.settingsStore
                val enabled = settings.trackingEnabled.value
                val background = settings.trackingMode.value == TrackingMode.Background
                if (enabled && background && LocationPermissions.hasFine(context)) {
                    TrackingService.start(context.applicationContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
