package com.reshare.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Manages notification permission requests on Android 13+.
 * Stores user denial preference to avoid repeated permission requests.
 */
class NotificationPermissionManager(private val activity: ComponentActivity) {

    private val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var permissionLauncher: ActivityResultLauncher<String>? = null
    private var onPermissionResult: ((Boolean) -> Unit)? = null

    /**
     * Registers the permission launcher. Must be called in Activity onCreate,
     * before the activity is started.
     */
    fun register(onResult: (Boolean) -> Unit = {}) {
        onPermissionResult = onResult
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                markPermissionDenied()
            }
            onPermissionResult?.invoke(granted)
        }
    }

    /**
     * Requests notification permission if needed on Android 13+.
     * Returns true if permission is already granted or not required.
     * Returns false if permission request was shown (result comes via callback).
     */
    fun requestIfNeeded(): Boolean {
        // Permission not required on Android < 13
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        // Already granted
        if (isPermissionGranted()) {
            return true
        }

        // User already denied - don't ask again
        if (wasPermissionDenied()) {
            return true
        }

        // Request permission
        permissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
        return false
    }

    /**
     * Checks if notification permission is currently granted.
     */
    fun isPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required on older Android versions
        }
    }

    private fun wasPermissionDenied(): Boolean {
        return prefs.getBoolean(KEY_PERMISSION_DENIED, false)
    }

    private fun markPermissionDenied() {
        prefs.edit().putBoolean(KEY_PERMISSION_DENIED, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "notification_prefs"
        private const val KEY_PERMISSION_DENIED = "permission_denied"
    }
}
