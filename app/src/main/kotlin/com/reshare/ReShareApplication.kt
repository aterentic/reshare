package com.reshare

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.reshare.util.CacheManager

class ReShareApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)

        // Clean up old cached files in background
        Thread {
            CacheManager(this).cleanupOldFiles()
        }.start()
    }
}
