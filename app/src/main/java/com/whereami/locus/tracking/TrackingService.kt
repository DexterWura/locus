package com.whereami.locus.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.whereami.locus.LocusApp
import com.whereami.locus.R
import com.whereami.locus.domain.TrackingStatus
import com.whereami.locus.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TrackingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var collectionJob: Job? = null
    private var statusJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                scope.launch {
                    val app = application as LocusApp
                    app.container.trackingController.setTrackingEnabled(false)
                    stopSelfSafely()
                }
                return START_NOT_STICKY
            }

            else -> {
                startAsForeground()
                beginCollecting()
                observeStatusForNotification()
            }
        }
        return START_STICKY
    }

    private fun beginCollecting() {
        if (collectionJob?.isActive == true) return
        val app = application as LocusApp
        val repository = app.container.trackRepository
        val settings = app.container.settingsStore
        val locationSource = app.container.locationSource

        collectionJob = scope.launch {
            val interval = settings.sampleIntervalMs.first()
            locationSource.observeLocations(interval).collectLatest { point ->
                repository.insertPoint(point)
            }
        }
    }

    private fun observeStatusForNotification() {
        if (statusJob?.isActive == true) return
        val locationSource = (application as LocusApp).container.locationSource
        statusJob = scope.launch {
            locationSource.status.collectLatest { status ->
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, buildNotification(status))
            }
        }
    }

    private fun startAsForeground() {
        createChannel()
        val notification = buildNotification(TrackingStatus.Moving)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(status: TrackingStatus): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, TrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val statusText = when (status) {
            TrackingStatus.Moving -> "Moving · keeping today’s path"
            TrackingStatus.Still -> "Still · waiting for movement"
            TrackingStatus.Paused -> getString(R.string.tracking_notification_text)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, getString(R.string.tracking_notification_stop), stopIntent)
            .build()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.tracking_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.tracking_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    private fun stopSelfSafely() {
        collectionJob?.cancel()
        collectionJob = null
        statusJob?.cancel()
        statusJob = null
        (application as LocusApp).container.locationSource.markPaused()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        collectionJob?.cancel()
        statusJob?.cancel()
        scope.cancel()
        (application as? LocusApp)?.container?.locationSource?.markPaused()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "locus_tracking"
        const val NOTIFICATION_ID = 42
        const val ACTION_STOP = "com.whereami.locus.action.STOP_TRACKING"

        fun start(context: Context) {
            val intent = Intent(context, TrackingService::class.java)
            ContextCompatStartForeground(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TrackingService::class.java))
        }

        private fun ContextCompatStartForeground(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
