package com.reshare.converter

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Converts PDF files to HTML using Poppler's pdftohtml binary.
 * The resulting HTML can then be fed to Pandoc for conversion to other formats.
 */
object PdfInputConverter {

    /**
     * Converts a PDF file to HTML string using pdftohtml.
     *
     * @param pdfFile The input PDF file
     * @param nativeLibDir Directory containing the pdftohtml binary
     * @param libSymlinkDir Directory containing versioned library symlinks
     * @return Result containing HTML string on success, or a ConversionError on failure
     */
    fun convertToHtml(pdfFile: File, nativeLibDir: String, libSymlinkDir: File): Result<String> {
        val command = buildCommand("$nativeLibDir/libpdftohtml.so", pdfFile)

        return try {
            val process = ProcessBuilder(command)
                .directory(pdfFile.parentFile)
                .apply {
                    environment()["LD_LIBRARY_PATH"] = "$libSymlinkDir:$nativeLibDir"
                }
                .start()

            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()

            val completed = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)

            if (!completed) {
                process.destroyForcibly()
                return Result.failure(ConversionError.Timeout())
            }

            val exitCode = process.exitValue()
            if (exitCode != 0) {
                return Result.failure(mapPdfToHtmlError(exitCode, stderr))
            }

            if (stdout.isBlank()) {
                return Result.failure(ConversionError.InputError("PDF produced no text output (may be scanned/image-only)"))
            }

            Result.success(stdout)
        } catch (e: Exception) {
            Result.failure(ConversionError.ProcessFailed(-1, e.message ?: "Unknown error"))
        }
    }

    /**
     * Builds the pdftohtml command line arguments.
     * Flags: -s (single page), -i (ignore images), -noframes (single HTML), -stdout (pipe to stdout).
     */
    internal fun buildCommand(pdfToHtmlPath: String, inputFile: File): List<String> {
        return listOf(
            pdfToHtmlPath,
            "-s",
            "-i",
            "-noframes",
            "-stdout",
            inputFile.absolutePath
        )
    }

    /**
     * Maps pdftohtml exit codes and stderr to ConversionError.
     * Exit code 3 or "encrypted" in stderr indicates a password-protected PDF.
     */
    internal fun mapPdfToHtmlError(exitCode: Int, stderr: String): ConversionError {
        if (exitCode == 3 || stderr.contains("encrypted", ignoreCase = true)) {
            return ConversionError.InputError("PDF is password-protected and cannot be converted")
        }
        return ConversionError.ProcessFailed(exitCode, "pdftohtml failed: $stderr")
    }
}
