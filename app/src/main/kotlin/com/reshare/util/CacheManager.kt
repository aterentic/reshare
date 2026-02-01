package com.reshare.util

import android.content.Context
import java.io.File

/**
 * Manages cache cleanup for converted files.
 */
class CacheManager(private val context: Context) {

    private val convertedDir = File(context.cacheDir, "converted")

    /**
     * Removes files older than [maxAgeMillis] from the converted files cache.
     */
    fun cleanupOldFiles(maxAgeMillis: Long = ONE_HOUR_MILLIS) {
        if (!convertedDir.exists()) return

        val cutoffTime = System.currentTimeMillis() - maxAgeMillis

        convertedDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoffTime) {
                file.delete()
            }
        }
    }

    /**
     * Removes all files from the converted files cache.
     */
    fun clearAllCache() {
        convertedDir.deleteRecursively()
    }

    companion object {
        private const val ONE_HOUR_MILLIS = 60 * 60 * 1000L
    }
}
