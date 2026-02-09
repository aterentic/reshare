package com.reshare.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.materialswitch.MaterialSwitch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.reshare.R
import com.reshare.converter.InputFormat
import com.reshare.converter.OutputFormat

/**
 * Main launcher activity providing settings and usage instructions.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var notificationSwitch: MaterialSwitch
    private lateinit var notificationSetting: LinearLayout
    private lateinit var formatPreferences: FormatPreferences

    private val settingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
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
        formatPreferences = FormatPreferences(this)

        setupNotificationToggle()
        setupInputFormatsChips()
        setupOutputFormatsChips()
        setupHistoryButton()
    }

    override fun onResume() {
        super.onResume()
        updateNotificationSwitchState()
    }

    private fun setupNotificationToggle() {
        updateNotificationSwitchState()
        notificationSwitch.isClickable = false

        notificationSetting.setOnClickListener {
            toggleNotifications()
        }
    }

    private fun toggleNotifications() {
        if (isNotificationPermissionGranted()) {
            openNotificationSettings()
        } else {
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

    private fun setupInputFormatsChips() {
        val chipGroup = findViewById<ChipGroup>(R.id.input_formats_chips)
        val enabledInputs = formatPreferences.enabledInputFormats()

        for (format in InputFormat.entries) {
            val chip = Chip(this).apply {
                text = inputFormatLabel(format)
                isCheckable = true
                isChecked = format in enabledInputs
                setOnCheckedChangeListener { _, checked ->
                    formatPreferences.setInputEnabled(format, checked)
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun setupOutputFormatsChips() {
        val chipGroup = findViewById<ChipGroup>(R.id.output_formats_chips)
        val enabledOutputs = formatPreferences.enabledOutputFormatsAll()

        for (format in OutputFormat.entries) {
            val chip = Chip(this).apply {
                text = outputFormatLabel(format)
                isCheckable = true
                isChecked = format in enabledOutputs
                setOnCheckedChangeListener { _, checked ->
                    formatPreferences.setOutputEnabled(format, checked)
                }
            }
            chipGroup.addView(chip)
        }
    }

    private fun setupHistoryButton() {
        findViewById<LinearLayout>(R.id.history_setting).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun inputFormatLabel(format: InputFormat): String = when (format) {
        InputFormat.PLAIN -> "Plain"
        InputFormat.MARKDOWN -> "Markdown"
        InputFormat.ORG -> "Org"
        InputFormat.HTML -> "HTML"
        InputFormat.DOCX -> "DOCX"
        InputFormat.ODT -> "ODT"
        InputFormat.EPUB -> "EPUB"
        InputFormat.LATEX -> "LaTeX"
        InputFormat.PDF -> "PDF"
        InputFormat.IMAGE -> "Image"
    }

    private fun outputFormatLabel(format: OutputFormat): String = when (format) {
        OutputFormat.PDF -> "PDF"
        OutputFormat.DOCX -> "DOCX"
        OutputFormat.HTML -> "HTML"
        OutputFormat.MARKDOWN -> "Markdown"
        OutputFormat.PLAIN -> "Plain Text"
        OutputFormat.LATEX -> "LaTeX"
    }
}
