package com.reshare

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.reshare.converter.ConversionError
import com.reshare.converter.FormatDetector
import com.reshare.converter.InputFormat
import com.reshare.converter.MAX_FILE_SIZE
import com.reshare.converter.OutputFormat
import com.reshare.converter.PandocConverter
import com.reshare.converter.PdfConverter
import com.reshare.notification.NotificationPermissionManager
import com.reshare.notification.ProgressNotifier
import com.reshare.share.ShareHandler
import com.reshare.share.SharePreferences
import com.reshare.share.StorageSaver
import com.reshare.ui.FormatPickerDialog
import com.reshare.ui.FormatPreferences
import com.reshare.ui.PostConversionDialog
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var formatDetector: FormatDetector
    private lateinit var formatPreferences: FormatPreferences
    private lateinit var permissionManager: NotificationPermissionManager
    private lateinit var safLauncher: ActivityResultLauncher<Intent>

    /** File pending SAF save (null when saving text). */
    private var pendingSaveFile: File? = null
    /** Text pending SAF save (null when saving a file). */
    private var pendingSaveText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        formatDetector = FormatDetector(contentResolver)
        formatPreferences = FormatPreferences(this)

        safLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleSafResult(result.resultCode, result.data)
        }

        // Register permission manager before activity starts
        permissionManager = NotificationPermissionManager(this)
        permissionManager.register()

        // Request notification permission on first launch or share
        permissionManager.requestIfNeeded()

        when (intent?.action) {
            Intent.ACTION_SEND -> handleShareIntent(intent)
            Intent.ACTION_SEND_MULTIPLE -> handleBatchShareIntent(intent)
            else -> finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when (intent.action) {
            Intent.ACTION_SEND -> handleShareIntent(intent)
            Intent.ACTION_SEND_MULTIPLE -> handleBatchShareIntent(intent)
            else -> finish()
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
        val enabledFormats = formatPreferences.enabledOutputFormats(input.inputFormat)

        if (enabledFormats.isEmpty()) {
            Toast.makeText(this, R.string.no_formats_configured, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        FormatPickerDialog.newInstance(
            inputFormat = input.inputFormat,
            enabledFormats = enabledFormats,
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
                    showPostConversionDialog(file, outputFormat)
                }.onFailure { error ->
                    showError(error as? ConversionError ?: ConversionError.ProcessFailed(-1, error.message ?: "Unknown error"))
                }
            } catch (e: Exception) {
                progressNotifier.hideProgress()
                showError(ConversionError.ProcessFailed(-1, e.message ?: "Unknown error"))
            }
        }
    }

    private fun handleBatchShareIntent(intent: Intent) {
        val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        }

        if (uris.isNullOrEmpty()) {
            showError(ConversionError.InputError("No files provided"))
            return
        }

        // Detect format from the first file for the format picker
        val firstFormat = formatDetector.detectFormat(uris.first())

        showBatchFormatPicker(uris, firstFormat)
    }

    private fun showBatchFormatPicker(uris: List<Uri>, firstInputFormat: InputFormat) {
        val enabledFormats = formatPreferences.enabledOutputFormats(firstInputFormat)

        if (enabledFormats.isEmpty()) {
            Toast.makeText(this, R.string.no_formats_configured, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        FormatPickerDialog.newInstance(
            inputFormat = firstInputFormat,
            enabledFormats = enabledFormats,
            onFormatSelected = { outputFormat ->
                startBatchConversion(uris, outputFormat)
            },
            onCancelled = {
                finish()
            }
        ).show(supportFragmentManager, "format_picker")
    }

    private fun startBatchConversion(uris: List<Uri>, outputFormat: OutputFormat) {
        val progressNotifier = ProgressNotifier(this)
        val total = uris.size

        lifecycleScope.launch {
            val convertedFiles = mutableListOf<File>()
            val failures = mutableListOf<Int>()

            for ((index, uri) in uris.withIndex()) {
                progressNotifier.showBatchProgress(index + 1, total)

                val inputFormat = formatDetector.detectFormat(uri)

                val size = getFileSize(uri)
                if (size > MAX_FILE_SIZE) {
                    failures.add(index + 1)
                    continue
                }

                val content = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (content == null) {
                    failures.add(index + 1)
                    continue
                }

                val input = PandocConverter.ConversionInput(
                    content = content,
                    contentUri = null,
                    inputFormat = inputFormat
                )

                try {
                    val result = if (outputFormat == OutputFormat.PDF) {
                        PdfConverter(this@MainActivity).convertToPdf(input)
                    } else {
                        PandocConverter(this@MainActivity).convert(input, outputFormat)
                    }

                    result.onSuccess { file ->
                        convertedFiles.add(file)
                    }.onFailure {
                        failures.add(index + 1)
                    }
                } catch (e: Exception) {
                    failures.add(index + 1)
                }
            }

            progressNotifier.hideProgress()

            if (failures.isNotEmpty() && convertedFiles.isNotEmpty()) {
                val message = getString(R.string.batch_partial_failure, failures.size, total)
                ProgressNotifier(this@MainActivity).showError(
                    ConversionError.InputError(message)
                )
            }

            if (convertedFiles.isEmpty()) {
                showError(ConversionError.ProcessFailed(-1, "All conversions failed"))
                return@launch
            }

            if (convertedFiles.size == 1) {
                showPostConversionDialog(convertedFiles.first(), outputFormat)
            } else {
                // Multiple files - always share as files (no SAF save for batch)
                ShareHandler(this@MainActivity).shareFiles(convertedFiles, outputFormat.mimeType)
                finish()
            }
        }
    }

    private fun showPostConversionDialog(file: File, outputFormat: OutputFormat) {
        val shareAsText = outputFormat.isTextBased &&
            SharePreferences(this).shareTextFormatsAsText

        PostConversionDialog.show(
            context = this,
            onShare = {
                if (shareAsText) {
                    val text = file.readText(Charsets.UTF_8)
                    file.delete()
                    ShareHandler(this).shareText(text)
                } else {
                    ShareHandler(this).shareFile(file, outputFormat)
                }
                finish()
            },
            onSaveTo = {
                val suggestedName = "converted.${outputFormat.extension}"
                if (shareAsText) {
                    pendingSaveText = file.readText(Charsets.UTF_8)
                    pendingSaveFile = null
                    file.delete()
                } else {
                    pendingSaveFile = file
                    pendingSaveText = null
                }
                launchSafPicker(outputFormat.mimeType, suggestedName)
            },
            onCancelled = {
                file.delete()
                finish()
            }
        )
    }

    private fun launchSafPicker(mimeType: String, suggestedName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, suggestedName)
        }
        safLauncher.launch(intent)
    }

    private fun handleSafResult(resultCode: Int, data: Intent?) {
        val uri = data?.data
        if (resultCode != RESULT_OK || uri == null) {
            pendingSaveFile?.delete()
            pendingSaveFile = null
            pendingSaveText = null
            finish()
            return
        }

        val saver = StorageSaver(contentResolver)
        val saved = when {
            pendingSaveFile != null -> {
                val ok = saver.saveToUri(pendingSaveFile!!, uri)
                pendingSaveFile!!.delete()
                pendingSaveFile = null
                ok
            }
            pendingSaveText != null -> {
                val ok = saver.saveTextToUri(pendingSaveText!!, uri)
                pendingSaveText = null
                ok
            }
            else -> false
        }

        if (saved) {
            Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
