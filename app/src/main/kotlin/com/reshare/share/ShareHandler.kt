package com.reshare.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.reshare.converter.OutputFormat
import java.io.File

/**
 * Handles sharing converted files to other apps via FileProvider.
 */
class ShareHandler(private val context: Context) {

    /**
     * Shares a converted file using Android's share sheet.
     *
     * @param file The converted file to share
     * @param outputFormat The output format (used to determine MIME type)
     */
    fun shareFile(file: File, outputFormat: OutputFormat) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = outputFormat.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share converted document")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
