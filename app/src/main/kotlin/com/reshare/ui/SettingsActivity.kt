package com.reshare.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Switch
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.reshare.R
import com.reshare.share.SharePreferences

/**
 * Main launcher activity providing settings and usage instructions.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var notificationSwitch: Switch
    private lateinit var notificationSetting: LinearLayout
    private lateinit var textSharingSwitch: Switch
    private lateinit var textSharingSetting: LinearLayout
    private lateinit var sharePreferences: SharePreferences

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Update switch state when returning from system settings
        updateNotificationSwitchState()
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationSwitch.isChecked = granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        title = getString(R.string.settings_title)

        notificationSwitch = findViewById(R.id.switch_notifications)
        notificationSetting = findViewById(R.id.notification_setting)
        textSharingSwitch = findViewById(R.id.switch_text_sharing)
        textSharingSetting = findViewById(R.id.text_sharing_setting)
        sharePreferences = SharePreferences(this)

        setupNotificationToggle()
        setupTextSharingToggle()
    }

    override fun onResume() {
        super.onResume()
        updateNotificationSwitchState()
    }

    private fun setupNotificationToggle() {
        updateNotificationSwitchState()

        // Handle clicks on the entire row
        notificationSetting.setOnClickListener {
            toggleNotifications()
        }

        // Handle direct switch changes
        notificationSwitch.setOnClickListener {
            toggleNotifications()
        }
    }

    private fun setupTextSharingToggle() {
        textSharingSwitch.isChecked = sharePreferences.shareTextFormatsAsText

        textSharingSetting.setOnClickListener {
            toggleTextSharing()
        }

        textSharingSwitch.setOnClickListener {
            toggleTextSharing()
        }
    }

    private fun toggleTextSharing() {
        val newValue = !sharePreferences.shareTextFormatsAsText
        sharePreferences.shareTextFormatsAsText = newValue
        textSharingSwitch.isChecked = newValue
    }

    private fun toggleNotifications() {
        if (notificationSwitch.isChecked) {
            // User wants to disable - open system settings
            openNotificationSettings()
        } else {
            // User wants to enable
            requestNotificationPermission()
        }
    }

    private fun updateNotificationSwitchState() {
        notificationSwitch.isChecked = isNotificationPermissionGranted()
    }

    private fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // User previously denied, send to settings
                openNotificationSettings()
            } else {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun openNotificationSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        settingsLauncher.launch(intent)
    }
}
