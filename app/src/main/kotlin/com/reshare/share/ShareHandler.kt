package com.reshare.share

import android.content.Context
import android.content.Intent
import android.net.Uri
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

    /**
     * Shares multiple converted files using Android's share sheet with ACTION_SEND_MULTIPLE.
     *
     * @param files The converted files to share
     * @param mimeType The MIME type for the shared files
     */
    fun shareFiles(files: List<File>, mimeType: String) {
        val authority = "${context.packageName}.fileprovider"
        val uris = ArrayList<Uri>(files.map { FileProvider.getUriForFile(context, authority, it) })

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share converted documents")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Shares plain text content directly via EXTRA_TEXT.
     * Used for sharing document content as a chat message instead of a file.
     *
     * @param text The text content to share
     */
    fun shareText(text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }

        val chooser = Intent.createChooser(shareIntent, "Share as text")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
