package com.reshare.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.reshare.R
import com.reshare.converter.InputFormat
import com.reshare.converter.OutputFormat
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
    private lateinit var formatPreferences: FormatPreferences

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
        formatPreferences = FormatPreferences(this)

        setupNotificationToggle()
        setupTextSharingToggle()
        setupFormatMatrix()
        setupHistoryButton()
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

    private fun setupHistoryButton() {
        findViewById<LinearLayout>(R.id.history_setting).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun setupFormatMatrix() {
        val container = findViewById<LinearLayout>(R.id.format_matrix_container)
        val table = TableLayout(this)

        val inputFormats = InputFormat.entries
        val outputFormats = OutputFormat.entries

        // Header row: empty corner cell + one column header per output format
        val headerRow = TableRow(this)
        headerRow.addView(TextView(this)) // empty corner
        for (output in outputFormats) {
            val header = TextView(this).apply {
                text = outputFormatLabel(output)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                gravity = Gravity.CENTER
                setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            }
            headerRow.addView(header)
        }
        table.addView(headerRow)

        // One row per input format
        for (input in inputFormats) {
            val row = TableRow(this)

            val label = TextView(this).apply {
                text = inputFormatLabel(input)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dpToPx(4), dpToPx(8), dpToPx(4))
            }
            row.addView(label)

            for (output in outputFormats) {
                val checkBox = CheckBox(this).apply {
                    isChecked = formatPreferences.isEnabled(input, output)
                    gravity = Gravity.CENTER
                    setOnCheckedChangeListener { _, checked ->
                        formatPreferences.setEnabled(input, output, checked)
                    }
                }
                row.addView(checkBox)
            }
            table.addView(row)
        }

        container.addView(table)
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()
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
    }

    private fun outputFormatLabel(format: OutputFormat): String = when (format) {
        OutputFormat.PDF -> "PDF"
        OutputFormat.DOCX -> "DOCX"
        OutputFormat.HTML -> "HTML"
        OutputFormat.MARKDOWN -> "MD"
        OutputFormat.PLAIN -> "TXT"
        OutputFormat.LATEX -> "LaTeX"
    }
}
