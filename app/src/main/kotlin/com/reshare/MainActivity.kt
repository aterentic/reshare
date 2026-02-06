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
import com.reshare.converter.Template
import com.reshare.notification.NotificationPermissionManager
import com.reshare.notification.ProgressNotifier
import com.reshare.share.ShareHandler
import com.reshare.share.SharePreferences
import com.reshare.share.StorageSaver
import com.reshare.history.ConversionHistoryDb
import com.reshare.history.ConversionRecord
import com.reshare.ui.FormatPickerDialog
import com.reshare.ui.FormatPreferences
import com.reshare.ui.PostConversionDialog
import com.reshare.ui.TemplatePickerDialog
import com.reshare.ui.TextPreviewActivity
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var formatDetector: FormatDetector
    private lateinit var formatPreferences: FormatPreferences
    private lateinit var permissionManager: NotificationPermissionManager
    private lateinit var safLauncher: ActivityResultLauncher<Intent>
    private lateinit var previewLauncher: ActivityResultLauncher<Intent>

    private lateinit var historyDb: ConversionHistoryDb

    /** File pending SAF save. */
    private var pendingSaveFile: File? = null

    /** Input name for the current conversion (used for history recording). */
    private var pendingInputName: String = "Shared text"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        formatDetector = FormatDetector(contentResolver)
        formatPreferences = FormatPreferences(this)
        historyDb = ConversionHistoryDb(this)

        safLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleSafResult(result.resultCode, result.data)
        }

        previewLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            finish()
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

                pendingInputName = "Shared text"

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

                pendingInputName = getFileName(uri) ?: "Shared file"

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

    private fun getFileName(uri: Uri): String? {
        return contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)
            } else null
        }
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
                showTemplatePickerOrConvert(input, outputFormat)
            },
            onCancelled = {
                finish()
            }
        ).show(supportFragmentManager, "format_picker")
    }

    private fun showTemplatePickerOrConvert(input: PandocConverter.ConversionInput, outputFormat: OutputFormat) {
        if (!TemplatePickerDialog.supportsTemplates(outputFormat)) {
            startConversion(input, outputFormat, Template.DEFAULT)
            return
        }

        TemplatePickerDialog.show(
            context = this,
            outputFormat = outputFormat,
            formatPreferences = formatPreferences,
            onTemplateSelected = { template ->
                startConversion(input, outputFormat, template)
            },
            onCancelled = {
                finish()
            }
        )
    }

    private fun startConversion(input: PandocConverter.ConversionInput, outputFormat: OutputFormat, template: Template) {
        val progressNotifier = ProgressNotifier(this)
        progressNotifier.showProgress()

        val cssContent = loadTemplateCss(template)

        lifecycleScope.launch {
            try {
                val result = if (outputFormat == OutputFormat.PDF) {
                    PdfConverter(this@MainActivity).convertToPdf(input, cssContent)
                } else {
                    val cssFile = writeCssToCache(cssContent)
                    PandocConverter(this@MainActivity).convert(input, outputFormat, cssFile)
                }

                progressNotifier.hideProgress()

                result.onSuccess { file ->
                    recordHistory(file, pendingInputName, input.inputFormat, outputFormat)
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
                showBatchTemplatePickerOrConvert(uris, outputFormat)
            },
            onCancelled = {
                finish()
            }
        ).show(supportFragmentManager, "format_picker")
    }

    private fun showBatchTemplatePickerOrConvert(uris: List<Uri>, outputFormat: OutputFormat) {
        if (!TemplatePickerDialog.supportsTemplates(outputFormat)) {
            startBatchConversion(uris, outputFormat, Template.DEFAULT)
            return
        }

        TemplatePickerDialog.show(
            context = this,
            outputFormat = outputFormat,
            formatPreferences = formatPreferences,
            onTemplateSelected = { template ->
                startBatchConversion(uris, outputFormat, template)
            },
            onCancelled = {
                finish()
            }
        )
    }

    private fun startBatchConversion(uris: List<Uri>, outputFormat: OutputFormat, template: Template) {
        val progressNotifier = ProgressNotifier(this)
        val total = uris.size
        val cssContent = loadTemplateCss(template)

        lifecycleScope.launch {
            val convertedFiles = mutableListOf<File>()
            val failures = mutableListOf<Int>()
            val cssFile = writeCssToCache(cssContent)

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

                val inputName = getFileName(uri) ?: "Shared file"

                try {
                    val result = if (outputFormat == OutputFormat.PDF) {
                        PdfConverter(this@MainActivity).convertToPdf(input, cssContent)
                    } else {
                        PandocConverter(this@MainActivity).convert(input, outputFormat, cssFile)
                    }

                    result.onSuccess { file ->
                        recordHistory(file, inputName, inputFormat, outputFormat)
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

        if (shareAsText) {
            val intent = TextPreviewActivity.newIntent(
                context = this,
                file = file,
                mimeType = outputFormat.mimeType,
                extension = outputFormat.extension
            )
            previewLauncher.launch(intent)
            return
        }

        PostConversionDialog.show(
            context = this,
            onShare = {
                ShareHandler(this).shareFile(file, outputFormat)
                finish()
            },
            onSaveTo = {
                pendingSaveFile = file
                launchSafPicker(outputFormat.mimeType, "converted.${outputFormat.extension}")
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

    private fun recordHistory(
        file: File,
        inputName: String,
        inputFormat: InputFormat,
        outputFormat: OutputFormat
    ) {
        try {
            val historyFile = ConversionHistoryDb.copyToHistory(this, file, outputFormat)
            val record = ConversionRecord(
                id = 0,
                inputName = inputName,
                inputFormat = inputFormat,
                outputFormat = outputFormat,
                timestamp = System.currentTimeMillis(),
                outputPath = historyFile.absolutePath,
                sizeBytes = historyFile.length()
            )
            historyDb.insert(record)
        } catch (_: Exception) {
            // History recording is best-effort; don't interrupt conversion flow
        }
    }

    private fun handleSafResult(resultCode: Int, data: Intent?) {
        val uri = data?.data
        if (resultCode != RESULT_OK || uri == null) {
            pendingSaveFile?.delete()
            pendingSaveFile = null
            finish()
            return
        }

        val file = pendingSaveFile
        pendingSaveFile = null

        val saved = if (file != null) {
            val ok = StorageSaver(contentResolver).saveToUri(file, uri)
            file.delete()
            ok
        } else {
            false
        }

        if (saved) {
            Toast.makeText(this, R.string.save_success, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    /**
     * Reads the CSS content for [template] from assets.
     * Returns null for the Default template (no styling).
     */
    private fun loadTemplateCss(template: Template): String? {
        val path = template.cssAssetPath ?: return null
        return try {
            assets.open(path).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Writes [css] to a temporary file in cache, suitable for Pandoc's --css flag.
     * Returns null when [css] is null (Default template).
     */
    private fun writeCssToCache(css: String?): File? {
        css ?: return null
        val file = File(cacheDir, "template_style.css")
        file.writeText(css)
        return file
    }
}
