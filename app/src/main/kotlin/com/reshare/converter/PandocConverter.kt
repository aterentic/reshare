package com.reshare.converter

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Input formats supported by Pandoc.
 */
enum class InputFormat(val pandocFlag: String, val mimeTypes: List<String>) {
    PLAIN("markdown", emptyList()),  // No MIME type - detected via content sniffing fallback
    MARKDOWN("markdown", listOf("text/markdown", "text/x-markdown")),
    ORG("org", listOf("text/org", "text/x-org")),
    HTML("html", listOf("text/html")),
    DOCX("docx", listOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
    ODT("odt", listOf("application/vnd.oasis.opendocument.text")),
    EPUB("epub", listOf("application/epub+zip")),
    LATEX("latex", listOf("application/x-latex", "application/x-tex"));

    companion object {
        fun fromMimeType(mimeType: String): InputFormat? =
            entries.find { mimeType in it.mimeTypes }
    }
}

/**
 * Output formats supported by Pandoc.
 * Note: PDF is handled separately via HTML -> WebView -> PrintManager.
 */
enum class OutputFormat(val pandocFlag: String, val extension: String, val mimeType: String) {
    PDF("html", "pdf", "application/pdf"),
    DOCX("docx", "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    HTML("html", "html", "text/html"),
    MARKDOWN("markdown", "md", "text/markdown"),
    PLAIN("plain", "txt", "text/plain"),
    LATEX("latex", "tex", "application/x-latex")
}

/**
 * Errors that can occur during document conversion.
 */
sealed class ConversionError : Exception() {
    data class Timeout(override val message: String = "Conversion timed out") : ConversionError()
    data class ProcessFailed(val exitCode: Int, val stderr: String) : ConversionError() {
        override val message: String = "Pandoc failed with exit code $exitCode: $stderr"
    }
    data class FileTooLarge(val sizeBytes: Long, val maxBytes: Long = MAX_FILE_SIZE) : ConversionError() {
        override val message: String = "File size $sizeBytes exceeds limit of $maxBytes bytes"
    }
    data class UnsupportedFormat(val format: String) : ConversionError() {
        override val message: String = "Unsupported format: $format"
    }
    data class InputError(override val message: String) : ConversionError()
}

/**
 * Maximum file size for conversion (10 MB).
 */
const val MAX_FILE_SIZE: Long = 10_000_000

/**
 * Timeout for Pandoc process execution in seconds.
 */
const val PROCESS_TIMEOUT_SECONDS: Long = 30

/**
 * Converts documents using the bundled Pandoc binary.
 */
class PandocConverter(private val context: Context) {

    /**
     * Input for document conversion.
     * Either [content] or [contentUri] must be provided, but not both.
     */
    data class ConversionInput(
        val content: ByteArray?,
        val contentUri: Uri?,
        val inputFormat: InputFormat
    ) {
        init {
            require(content != null || contentUri != null) {
                "Either content or contentUri must be provided"
            }
            require(content == null || contentUri == null) {
                "Cannot provide both content and contentUri"
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ConversionInput
            if (content != null) {
                if (other.content == null) return false
                if (!content.contentEquals(other.content)) return false
            } else if (other.content != null) return false
            if (contentUri != other.contentUri) return false
            if (inputFormat != other.inputFormat) return false
            return true
        }

        override fun hashCode(): Int {
            var result = content?.contentHashCode() ?: 0
            result = 31 * result + (contentUri?.hashCode() ?: 0)
            result = 31 * result + inputFormat.hashCode()
            return result
        }
    }

    private val outputDir: File by lazy {
        File(context.cacheDir, "converted").also { it.mkdirs() }
    }

    private val libSymlinkDir: File by lazy {
        File(context.cacheDir, "lib").also { it.mkdirs() }
    }

    /**
     * Creates symlinks for versioned library names that Pandoc expects.
     * Pandoc is linked against libz.so.1 but Android requires lib*.so naming.
     * Returns the directory containing the symlinks.
     */
    private fun setupLibrarySymlinks(nativeLibDir: String): File? {
        return try {
            // Map of expected soname -> actual file in nativeLibDir
            val symlinkMap = mapOf(
                "libz.so.1" to "libz.so",
                "liblua5.4.so.5.4" to "liblua5.4.so"
            )

            for ((linkName, targetName) in symlinkMap) {
                val linkFile = File(libSymlinkDir, linkName)
                val targetFile = File(nativeLibDir, targetName)

                if (!linkFile.exists() && targetFile.exists()) {
                    // Create symlink using ln -sf
                    val process = ProcessBuilder("ln", "-sf", targetFile.absolutePath, linkFile.absolutePath)
                        .redirectErrorStream(true)
                        .start()
                    process.waitFor()
                }
            }

            libSymlinkDir
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts input to the specified output format.
     *
     * @param input The input data and format
     * @param outputFormat The desired output format
     * @return Result containing the output file on success, or a ConversionError on failure
     */
    fun convert(input: ConversionInput, outputFormat: OutputFormat): Result<File> {
        val outputFile = File(outputDir, "${UUID.randomUUID()}.${outputFormat.extension}")

        return when {
            input.content != null -> convertFromBytes(input.content, input.inputFormat, outputFormat, outputFile)
            input.contentUri != null -> convertFromUri(input.contentUri, input.inputFormat, outputFormat, outputFile)
            else -> Result.failure(ConversionError.InputError("No input provided"))
        }
    }

    private fun convertFromBytes(
        content: ByteArray,
        inputFormat: InputFormat,
        outputFormat: OutputFormat,
        outputFile: File
    ): Result<File> {
        if (content.size > MAX_FILE_SIZE) {
            return Result.failure(ConversionError.FileTooLarge(content.size.toLong()))
        }
        return runPandoc(
            inputFile = null,
            inputBytes = content,
            inputFormat = inputFormat,
            outputFormat = outputFormat,
            outputFile = outputFile
        )
    }

    private fun convertFromUri(
        contentUri: Uri,
        inputFormat: InputFormat,
        outputFormat: OutputFormat,
        outputFile: File
    ): Result<File> {
        val inputFile = copyUriToTempFile(contentUri)
            ?: return Result.failure(ConversionError.InputError("Failed to read input file"))

        return try {
            if (inputFile.length() > MAX_FILE_SIZE) {
                Result.failure(ConversionError.FileTooLarge(inputFile.length()))
            } else {
                runPandoc(
                    inputFile = inputFile,
                    inputBytes = null,
                    inputFormat = inputFormat,
                    outputFormat = outputFormat,
                    outputFile = outputFile
                )
            }
        } finally {
            inputFile.delete()
        }
    }

    private fun copyUriToTempFile(uri: Uri): File? {
        return try {
            val tempFile = File(context.cacheDir, "input_${UUID.randomUUID()}")
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private fun runPandoc(
        inputFile: File?,
        inputBytes: ByteArray?,
        inputFormat: InputFormat,
        outputFormat: OutputFormat,
        outputFile: File
    ): Result<File> {
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val pandocPath = "$nativeLibDir/libpandoc.so"

        // Create symlinks for versioned library names that Pandoc expects
        val libDir = setupLibrarySymlinks(nativeLibDir)
            ?: return Result.failure(ConversionError.ProcessFailed(-1, "Failed to setup library symlinks"))

        val command = buildCommand(pandocPath, inputFormat, outputFormat, inputFile, outputFile)

        return try {
            val process = ProcessBuilder(command)
                .directory(context.cacheDir)
                .apply {
                    environment()["LD_LIBRARY_PATH"] = "$libDir:$nativeLibDir"
                }
                .redirectErrorStream(true)
                .start()

            if (inputBytes != null) {
                process.outputStream.use { it.write(inputBytes) }
            }

            val completed = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)

            if (!completed) {
                process.destroyForcibly()
                return Result.failure(ConversionError.Timeout())
            }

            val exitCode = process.exitValue()
            if (exitCode != 0) {
                val stderr = process.inputStream.bufferedReader().readText()
                return Result.failure(ConversionError.ProcessFailed(exitCode, stderr))
            }

            if (!outputFile.exists()) {
                return Result.failure(ConversionError.ProcessFailed(exitCode, "Output file not created"))
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(ConversionError.ProcessFailed(-1, e.message ?: "Unknown error"))
        }
    }

    companion object {
        /**
         * Builds the Pandoc command line arguments.
         * Exposed for testing.
         */
        internal fun buildCommand(
            pandocPath: String,
            inputFormat: InputFormat,
            outputFormat: OutputFormat,
            inputFile: File?,
            outputFile: File
        ): List<String> {
            return buildList {
                add(pandocPath)
                add("-f")
                add(inputFormat.pandocFlag)
                add("-t")
                add(outputFormat.pandocFlag)
                if (inputFile != null) {
                    add(inputFile.absolutePath)
                }
                add("-o")
                add(outputFile.absolutePath)
            }
        }

        /**
         * Maps Pandoc exit codes to user-friendly error descriptions.
         * Exposed for testing.
         */
        internal fun mapExitCodeToDescription(exitCode: Int): String {
            return when (exitCode) {
                0 -> "Success"
                1 -> "PandocIOError"
                3 -> "PandocFailOnWarningError"
                4 -> "PandocAppError"
                5 -> "PandocTemplateError"
                6 -> "PandocOptionError"
                21 -> "PandocUnknownReaderError"
                22 -> "PandocUnknownWriterError"
                23 -> "PandocUnsupportedExtensionError"
                24 -> "PandocCiteprocError"
                25 -> "PandocBibliographyError"
                31 -> "PandocEpubSubdirectoryError"
                43 -> "PandocPDFError"
                44 -> "PandocXMLError"
                47 -> "PandocPDFProgramNotFoundError"
                61 -> "PandocHttpError"
                62 -> "PandocShouldNeverHappenError"
                63 -> "PandocSomeError"
                64 -> "PandocParseError"
                65 -> "PandocParsecError"
                66 -> "PandocMakePDFError"
                67 -> "PandocSyntaxMapError"
                83 -> "PandocFilterError"
                84 -> "PandocLuaError"
                91 -> "PandocNoScriptingEngine"
                92 -> "PandocMacroLoop"
                97 -> "PandocCouldNotFindDataFileError"
                98 -> "PandocCouldNotFindMetadataFileError"
                99 -> "PandocResourceNotFound"
                else -> "Unknown Pandoc error"
            }
        }
    }
}
