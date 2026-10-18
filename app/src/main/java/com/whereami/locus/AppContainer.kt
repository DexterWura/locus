package com.whereami.locus

import android.content.Context
import com.whereami.locus.auth.AuthSession
import com.whereami.locus.auth.AuthStore
import com.whereami.locus.data.SettingsStore
import com.whereami.locus.data.TrackRepositoryImpl
import com.whereami.locus.data.db.LocusDatabase
import com.whereami.locus.domain.TrackRepository
import com.whereami.locus.domain.TrackingController
import com.whereami.locus.tracking.CompassSensor
import com.whereami.locus.tracking.LocationSource
import com.whereami.locus.tracking.TrackingControllerImpl

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = LocusDatabase.get(appContext)

    val settingsStore = SettingsStore(appContext)
    val authStore = AuthStore(appContext)
    val authSession = AuthSession().also { it.observeProcessLifecycle() }
    val trackRepository: TrackRepository = TrackRepositoryImpl(database)
    val locationSource = LocationSource(appContext)
    val compassSensor = CompassSensor(appContext)
    val trackingController: TrackingController = TrackingControllerImpl(
        context = appContext,
        settingsStore = settingsStore,
        locationSource = locationSource,
        trackRepository = trackRepository
    )
}
