package com.whereami.locus

import android.content.Context
import com.whereami.locus.auth.AuthSession
import com.whereami.locus.auth.AuthStore
import com.whereami.locus.data.CompanyLinkStore
import com.whereami.locus.data.SettingsStore
import com.whereami.locus.data.TrackRepositoryImpl
import com.whereami.locus.data.db.LocusDatabase
import com.whereami.locus.data.migrate.DeviceDataBridge
import com.whereami.locus.data.sync.LocationUploader
import com.whereami.locus.domain.TrackRepository
import com.whereami.locus.domain.TrackingController
import com.whereami.locus.tracking.CompassSensor
import com.whereami.locus.tracking.LocationSource
import com.whereami.locus.tracking.TrackingControllerImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = LocusDatabase.get(appContext)
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val settingsStore = SettingsStore(appContext)
    val authStore = AuthStore(appContext)
    val authSession = AuthSession().also { it.observeProcessLifecycle() }
    val companyLinkStore = CompanyLinkStore(appContext)

    init {
        DeviceDataBridge.importIfPresent(
            context = appContext,
            database = database,
            settingsStore = settingsStore,
            authStore = authStore,
            companyLinkStore = companyLinkStore
        )
    }

    val trackRepository: TrackRepository = TrackRepositoryImpl(database)
    val locationUploader = LocationUploader(companyLinkStore, trackRepository)
    val locationSource = LocationSource(appContext)
    val compassSensor = CompassSensor(appContext)
    val trackingController: TrackingController = TrackingControllerImpl(
        context = appContext,
        settingsStore = settingsStore,
        locationSource = locationSource,
        trackRepository = trackRepository,
        locationUploader = locationUploader,
        appScope = appScope
    )

    init {
        appScope.launch {
            locationUploader.flushPending()
        }
    }
}
