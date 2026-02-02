package com.reshare

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.reshare.converter.ConversionError
import com.reshare.converter.FormatDetector
import com.reshare.converter.MAX_FILE_SIZE
import com.reshare.converter.OutputFormat
import com.reshare.converter.PandocConverter
import com.reshare.converter.PdfConverter
import com.reshare.notification.NotificationPermissionManager
import com.reshare.notification.ProgressNotifier
import com.reshare.share.ShareHandler
import com.reshare.ui.FormatPickerDialog
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var formatDetector: FormatDetector
    private lateinit var permissionManager: NotificationPermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        formatDetector = FormatDetector(contentResolver)

        // Register permission manager before activity starts
        permissionManager = NotificationPermissionManager(this)
        permissionManager.register()

        // Request notification permission on first launch or share
        permissionManager.requestIfNeeded()

        if (intent?.action == Intent.ACTION_SEND) {
            handleShareIntent(intent)
        } else {
            // Not a share intent - close silently
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_SEND) {
            handleShareIntent(intent)
        } else {
            finish()
        }
    }

    private fun handleShareIntent(intent: Intent) {
        val inputFormat = formatDetector.detectFormat(intent)

        val conversionInput = when {
            intent.hasExtra(Intent.EXTRA_TEXT) -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                val bytes = text.toByteArray(Charsets.UTF_8)

                if (bytes.size > MAX_FILE_SIZE) {
                    showError(ConversionError.FileTooLarge(bytes.size.toLong(), MAX_FILE_SIZE))
                    return
                }

                PandocConverter.ConversionInput(
                    content = bytes,
                    contentUri = null,
                    inputFormat = inputFormat
                )
            }

            intent.hasExtra(Intent.EXTRA_STREAM) -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }

                if (uri == null) {
                    showError(ConversionError.InputError("No file provided"))
                    return
                }

                val size = getFileSize(uri)
                if (size > MAX_FILE_SIZE) {
                    showError(ConversionError.FileTooLarge(size, MAX_FILE_SIZE))
                    return
                }

                // Read content from URI - use content bytes, not URI
                val content = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: run {
                        showError(ConversionError.InputError("Cannot read file"))
                        return
                    }

                PandocConverter.ConversionInput(
                    content = content,
                    contentUri = null,
                    inputFormat = inputFormat
                )
            }

            else -> {
                showError(ConversionError.InputError("No content to convert"))
                return
            }
        }

        showFormatPicker(conversionInput)
    }

    private fun getFileSize(uri: Uri): Long {
        return contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getLong(0)
            } else 0L
        } ?: 0L
    }

    private fun showError(error: ConversionError) {
        ProgressNotifier(this).showError(error)
        finish()
    }

    private fun showFormatPicker(input: PandocConverter.ConversionInput) {
        FormatPickerDialog.newInstance(
            inputFormat = input.inputFormat,
            onFormatSelected = { outputFormat ->
                startConversion(input, outputFormat)
            },
            onCancelled = {
                finish()
            }
        ).show(supportFragmentManager, "format_picker")
    }

    private fun startConversion(input: PandocConverter.ConversionInput, outputFormat: OutputFormat) {
        val progressNotifier = ProgressNotifier(this)
        progressNotifier.showProgress()

        lifecycleScope.launch {
            try {
                val result = if (outputFormat == OutputFormat.PDF) {
                    PdfConverter(this@MainActivity).convertToPdf(input)
                } else {
                    PandocConverter(this@MainActivity).convert(input, outputFormat)
                }

                progressNotifier.hideProgress()

                result.onSuccess { file ->
                    ShareHandler(this@MainActivity).shareFile(file, outputFormat)
                    finish()
                }.onFailure { error ->
                    showError(error as? ConversionError ?: ConversionError.ProcessFailed(-1, error.message ?: "Unknown error"))
                }
            } catch (e: Exception) {
                progressNotifier.hideProgress()
                showError(ConversionError.ProcessFailed(-1, e.message ?: "Unknown error"))
            }
        }
    }
}
