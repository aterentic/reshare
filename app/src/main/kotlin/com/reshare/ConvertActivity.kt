package com.reshare

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.reshare.converter.ConversionError
import com.reshare.converter.FormatDetector
import com.reshare.converter.InputFormat
import com.reshare.converter.MAX_FILE_SIZE
import com.reshare.converter.OutputFormat
import com.reshare.converter.PandocConverter
import com.reshare.converter.PdfConverter
import kotlinx.coroutines.launch

/**
 * Headless activity that exposes document conversion via an intent-based API
 * for automation tools (Tasker, Automate, MacroDroid).
 *
 * Action: com.reshare.ACTION_CONVERT
 * Extras:
 *   INPUT_URI: Uri           (required) content:// or file:// URI
 *   INPUT_FORMAT: String     (optional) e.g., "markdown", auto-detect if absent
 *   OUTPUT_FORMAT: String    (required) e.g., "pdf", "docx", "html", "markdown", "plain", "latex"
 *
 * Result (RESULT_OK):
 *   OUTPUT_URI: Uri          content:// URI to converted file (via FileProvider)
 *
 * Result (RESULT_CANCELED):
 *   ERROR: String            error message if conversion failed
 */
class ConvertActivity : AppCompatActivity() {

    companion object {
        const val ACTION_CONVERT = "com.reshare.ACTION_CONVERT"
        const val EXTRA_INPUT_URI = "INPUT_URI"
        const val EXTRA_INPUT_FORMAT = "INPUT_FORMAT"
        const val EXTRA_OUTPUT_FORMAT = "OUTPUT_FORMAT"
        const val EXTRA_OUTPUT_URI = "OUTPUT_URI"
        const val EXTRA_ERROR = "ERROR"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action != ACTION_CONVERT) {
            finishWithError("Invalid action")
            return
        }

        val inputUri = getInputUri()
        if (inputUri == null) {
            finishWithError("Missing or invalid INPUT_URI")
            return
        }

        val outputFormatName = intent.getStringExtra(EXTRA_OUTPUT_FORMAT)
        if (outputFormatName == null) {
            finishWithError("Missing OUTPUT_FORMAT")
            return
        }

        val outputFormat = try {
            OutputFormat.valueOf(outputFormatName.uppercase())
        } catch (_: IllegalArgumentException) {
            finishWithError("Unknown OUTPUT_FORMAT: $outputFormatName")
            return
        }

        val inputFormat = resolveInputFormat(inputUri)
        if (inputFormat == null) {
            finishWithError("Unknown INPUT_FORMAT: ${intent.getStringExtra(EXTRA_INPUT_FORMAT)}")
            return
        }

        val content = readContent(inputUri)
        if (content == null) {
            finishWithError("Cannot read input URI")
            return
        }

        if (content.size > MAX_FILE_SIZE) {
            finishWithError("File size ${content.size} exceeds limit of $MAX_FILE_SIZE bytes")
            return
        }

        val input = PandocConverter.ConversionInput(
            content = content,
            contentUri = null,
            inputFormat = inputFormat
        )

        lifecycleScope.launch {
            convert(input, outputFormat)
        }
    }

    private fun getInputUri(): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_INPUT_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_INPUT_URI)
        }
    }

    private fun resolveInputFormat(inputUri: Uri): InputFormat? {
        val formatName = intent.getStringExtra(EXTRA_INPUT_FORMAT)
        if (formatName != null) {
            return try {
                InputFormat.valueOf(formatName.uppercase())
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        val detector = FormatDetector(contentResolver)
        return detector.detectFormat(inputUri)
    }

    private fun readContent(uri: Uri): ByteArray? {
        return try {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun convert(input: PandocConverter.ConversionInput, outputFormat: OutputFormat) {
        try {
            val result = if (outputFormat == OutputFormat.PDF) {
                PdfConverter(this@ConvertActivity).convertToPdf(input)
            } else {
                PandocConverter(this@ConvertActivity).convert(input, outputFormat)
            }

            result.onSuccess { file ->
                val authority = "${packageName}.fileprovider"
                val outputUri = FileProvider.getUriForFile(this@ConvertActivity, authority, file)

                val resultIntent = Intent().apply {
                    putExtra(EXTRA_OUTPUT_URI, outputUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }.onFailure { error ->
                val message = (error as? ConversionError)?.message ?: error.message ?: "Unknown error"
                finishWithError(message)
            }
        } catch (e: Exception) {
            finishWithError(e.message ?: "Unknown error")
        }
    }

    private fun finishWithError(message: String) {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_ERROR, message)
        }
        setResult(RESULT_CANCELED, resultIntent)
        finish()
    }
}
