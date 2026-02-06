package com.reshare.converter

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * Detects input formats from MIME types, file extensions, and content sniffing.
 *
 * Detection priority:
 * 1. Check MIME type from intent/ContentResolver
 * 2. Check file extension if content URI
 * 3. Sniff content (magic bytes, patterns)
 * 4. Default to plain text
 */
class FormatDetector(private val contentResolver: ContentResolver) {

    private val extensionMap = mapOf(
        "txt" to InputFormat.PLAIN,
        "md" to InputFormat.MARKDOWN,
        "markdown" to InputFormat.MARKDOWN,
        "org" to InputFormat.ORG,
        "html" to InputFormat.HTML,
        "htm" to InputFormat.HTML,
        "docx" to InputFormat.DOCX,
        "odt" to InputFormat.ODT,
        "epub" to InputFormat.EPUB,
        "tex" to InputFormat.LATEX
    )

    /**
     * Detects format from an intent (typically ACTION_SEND or ACTION_VIEW).
     */
    fun detectFormat(intent: Intent): InputFormat {
        val mimeType = intent.type
        val uri = intent.data ?: getUriFromIntent(intent)

        val fileName = uri?.let { getFileName(it) }

        // Get content from URI or EXTRA_TEXT for sniffing
        val content = uri?.let { readContent(it) }
            ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.toByteArray(Charsets.UTF_8)

        return detectFormat(mimeType, fileName, content)
    }

    @Suppress("DEPRECATION")
    private fun getUriFromIntent(intent: Intent): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    /**
     * Detects format from a content URI.
     */
    fun detectFormat(uri: Uri): InputFormat {
        val mimeType = contentResolver.getType(uri)
        val fileName = getFileName(uri)
        val content = readContent(uri)

        return detectFormat(mimeType, fileName, content)
    }

    /**
     * Detects format from available metadata and content.
     *
     * @param mimeType MIME type if known
     * @param fileName File name if known (for extension detection)
     * @param content File content for magic byte detection (first few KB is sufficient)
     * @return Detected format, defaults to PLAIN if unknown
     */
    fun detectFormat(mimeType: String?, fileName: String?, content: ByteArray?): InputFormat {
        // 1. Check MIME type
        mimeType?.let { mime ->
            InputFormat.fromMimeType(mime)?.let { return it }
        }

        // 2. Check file extension
        fileName?.let { name ->
            detectFromExtension(name)?.let { return it }
        }

        // 3. Sniff content
        content?.let { bytes ->
            sniffContent(bytes)?.let { return it }
        }

        // 4. Default to plain text
        return InputFormat.PLAIN
    }

    private fun detectFromExtension(fileName: String): InputFormat? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return extensionMap[extension]
    }

    private fun sniffContent(content: ByteArray): InputFormat? {
        if (content.isEmpty()) return null

        // DOCX/EPUB/ODT are ZIP files - check for PK signature
        if (content.size >= 4 &&
            content[0] == 0x50.toByte() &&
            content[1] == 0x4B.toByte()
        ) {
            return sniffZipFormat(content)
        }

        // HTML detection
        val text = content.take(1000).toByteArray().toString(Charsets.UTF_8)
        if (text.contains("<!DOCTYPE html", ignoreCase = true) ||
            text.contains("<html", ignoreCase = true)
        ) {
            return InputFormat.HTML
        }

        // LaTeX detection
        if (text.contains("\\documentclass") ||
            text.contains("\\begin{document}")
        ) {
            return InputFormat.LATEX
        }

        // Org mode detection (headlines, metadata, links)
        if (text.contains(Regex("^\\*+\\s", RegexOption.MULTILINE)) ||  // * Headline
            text.contains(Regex("^#\\+[A-Z]+:", RegexOption.MULTILINE)) ||  // #+TITLE:
            text.contains(Regex("\\[\\[.+\\]\\]"))  // [[link]] or [[link][desc]]
        ) {
            return InputFormat.ORG
        }

        // Markdown heuristics (headers, links, code blocks)
        if (text.contains(Regex("^#{1,6}\\s", RegexOption.MULTILINE)) ||
            text.contains(Regex("\\[.+\\]\\(.+\\)")) ||
            text.contains("```")
        ) {
            return InputFormat.MARKDOWN
        }

        return null
    }

    private fun sniffZipFormat(content: ByteArray): InputFormat? {
        return try {
            val entries = mutableListOf<String>()
            ZipInputStream(ByteArrayInputStream(content)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    entries.add(entry.name)
                    entry = zip.nextEntry
                }
            }

            when {
                // DOCX: contains word/document.xml
                entries.any { it.startsWith("word/") && it.endsWith(".xml") } -> InputFormat.DOCX
                // EPUB: contains META-INF/container.xml
                entries.any { it == "META-INF/container.xml" } -> InputFormat.EPUB
                // ODT: contains content.xml at root
                entries.any { it == "content.xml" } -> InputFormat.ODT
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        return contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)
            } else null
        }
    }

    private fun readContent(uri: Uri): ByteArray? {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                // Read first 8KB for sniffing - enough for magic bytes and text heuristics
                val buffer = ByteArray(8192)
                val bytesRead = input.read(buffer)
                if (bytesRead > 0) buffer.copyOf(bytesRead) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        /**
         * Extension map exposed for testing.
         */
        internal val EXTENSION_MAP = mapOf(
            "txt" to InputFormat.PLAIN,
            "md" to InputFormat.MARKDOWN,
            "markdown" to InputFormat.MARKDOWN,
            "org" to InputFormat.ORG,
            "html" to InputFormat.HTML,
            "htm" to InputFormat.HTML,
            "docx" to InputFormat.DOCX,
            "odt" to InputFormat.ODT,
            "epub" to InputFormat.EPUB,
            "tex" to InputFormat.LATEX
        )

        /**
         * Detects format from extension without needing a ContentResolver.
         * Useful for testing and simple file-based detection.
         */
        fun detectFromExtension(fileName: String): InputFormat? {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            return EXTENSION_MAP[extension]
        }

        /**
         * Sniffs content to detect format without needing a ContentResolver.
         * Useful for testing and direct content-based detection.
         */
        fun sniffContent(content: ByteArray): InputFormat? {
            if (content.isEmpty()) return null

            // DOCX/EPUB/ODT are ZIP files - check for PK signature
            if (content.size >= 4 &&
                content[0] == 0x50.toByte() &&
                content[1] == 0x4B.toByte()
            ) {
                return sniffZipFormat(content)
            }

            // HTML detection
            val text = content.take(1000).toByteArray().toString(Charsets.UTF_8)
            if (text.contains("<!DOCTYPE html", ignoreCase = true) ||
                text.contains("<html", ignoreCase = true)
            ) {
                return InputFormat.HTML
            }

            // LaTeX detection
            if (text.contains("\\documentclass") ||
                text.contains("\\begin{document}")
            ) {
                return InputFormat.LATEX
            }

            // Org mode detection (headlines, metadata, links)
            if (text.contains(Regex("^\\*+\\s", RegexOption.MULTILINE)) ||  // * Headline
                text.contains(Regex("^#\\+[A-Z]+:", RegexOption.MULTILINE)) ||  // #+TITLE:
                text.contains(Regex("\\[\\[.+\\]\\]"))  // [[link]] or [[link][desc]]
            ) {
                return InputFormat.ORG
            }

            // Markdown heuristics (headers, links, code blocks)
            if (text.contains(Regex("^#{1,6}\\s", RegexOption.MULTILINE)) ||
                text.contains(Regex("\\[.+\\]\\(.+\\)")) ||
                text.contains("```")
            ) {
                return InputFormat.MARKDOWN
            }

            return null
        }

        private fun sniffZipFormat(content: ByteArray): InputFormat? {
            return try {
                val entries = mutableListOf<String>()
                ZipInputStream(ByteArrayInputStream(content)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        entries.add(entry.name)
                        entry = zip.nextEntry
                    }
                }

                when {
                    entries.any { it.startsWith("word/") && it.endsWith(".xml") } -> InputFormat.DOCX
                    entries.any { it == "META-INF/container.xml" } -> InputFormat.EPUB
                    entries.any { it == "content.xml" } -> InputFormat.ODT
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
