package com.reshare

import android.app.Application
import com.reshare.util.CacheManager

class ReShareApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Clean up old cached files in background
        Thread {
            CacheManager(this).cleanupOldFiles()
        }.start()
    }
}
