package com.whereami.locus

import android.app.Application

class LocusApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.trackingController.resumeIfEnabled()
    }
}
