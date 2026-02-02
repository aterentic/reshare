package com.reshare.notification

import android.content.Context
import android.widget.Toast
import com.reshare.converter.ConversionError

/**
 * Shows error messages via Toast as primary/fallback error reporting.
 * Works without notification permission and provides immediate user feedback.
 */
object ToastErrorReporter {

    /**
     * Shows a toast message for the given conversion error.
     * Uses Toast.LENGTH_LONG for better visibility.
     */
    fun showError(context: Context, error: ConversionError) {
        val message = formatErrorMessage(error)
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Shows a custom error message via toast.
     */
    fun showError(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun formatErrorMessage(error: ConversionError): String = when (error) {
        is ConversionError.FileTooLarge ->
            "File too large (max ${error.maxBytes / 1_000_000} MB)"
        is ConversionError.Timeout ->
            "Conversion timed out"
        is ConversionError.UnsupportedFormat ->
            "Unsupported file format"
        is ConversionError.ProcessFailed ->
            if (error.stderr.isNotEmpty() && error.stderr.length < 50) {
                "Conversion failed: ${error.stderr}"
            } else {
                "Conversion failed"
            }
        is ConversionError.InputError ->
            error.message
    }
}
