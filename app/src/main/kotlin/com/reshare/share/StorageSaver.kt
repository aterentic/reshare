package com.reshare.share

import android.content.ContentResolver
import android.net.Uri
import java.io.File

/**
 * Saves files to user-selected storage locations via SAF (Storage Access Framework).
 */
class StorageSaver(private val contentResolver: ContentResolver) {

    /**
     * Copies the contents of [file] to the given SAF [uri].
     *
     * @return true on success, false on failure
     */
    fun saveToUri(file: File, uri: Uri): Boolean {
        return try {
            contentResolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Writes [text] encoded as UTF-8 to the given SAF [uri].
     *
     * @return true on success, false on failure
     */
    fun saveTextToUri(text: String, uri: Uri): Boolean {
        return try {
            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
