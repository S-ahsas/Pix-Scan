package com.ahsas.pixqr

import android.app.Application
import com.google.android.material.color.DynamicColors

class App : Application() {
    val database: ScanDatabase by lazy {
        ScanDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}