package com.reshare.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PdfInputConverterTest {

    @Test
    fun `buildCommand includes all required flags`() {
        val command = PdfInputConverter.buildCommand("/lib/libpdftohtml.so", File("/tmp/input.pdf"))
        assertEquals(
            listOf("/lib/libpdftohtml.so", "-s", "-i", "-noframes", "-stdout", "/tmp/input.pdf"),
            command
        )
    }

    @Test
    fun `buildCommand handles paths with spaces`() {
        val command = PdfInputConverter.buildCommand("/lib/libpdftohtml.so", File("/tmp/my docs/input file.pdf"))
        assertEquals("/tmp/my docs/input file.pdf", command.last())
    }

    @Test
    fun `mapPdfToHtmlError returns InputError for exit code 3`() {
        val error = PdfInputConverter.mapPdfToHtmlError(3, "some error")
        assertTrue(error is ConversionError.InputError)
        assertTrue(error.message!!.contains("password-protected"))
    }

    @Test
    fun `mapPdfToHtmlError returns InputError when stderr contains encrypted`() {
        val error = PdfInputConverter.mapPdfToHtmlError(1, "Error: Document is Encrypted")
        assertTrue(error is ConversionError.InputError)
        assertTrue(error.message!!.contains("password-protected"))
    }

    @Test
    fun `mapPdfToHtmlError returns InputError for encrypted case insensitive`() {
        val error = PdfInputConverter.mapPdfToHtmlError(1, "error: encrypted pdf")
        assertTrue(error is ConversionError.InputError)
    }

    @Test
    fun `mapPdfToHtmlError returns ProcessFailed for generic error`() {
        val error = PdfInputConverter.mapPdfToHtmlError(1, "unknown error occurred")
        assertTrue(error is ConversionError.ProcessFailed)
        val processFailed = error as ConversionError.ProcessFailed
        assertEquals(1, processFailed.exitCode)
        assertTrue(processFailed.stderr.contains("unknown error occurred"))
    }

    @Test
    fun `mapPdfToHtmlError returns ProcessFailed for exit code 2`() {
        val error = PdfInputConverter.mapPdfToHtmlError(2, "file not found")
        assertTrue(error is ConversionError.ProcessFailed)
    }
}
