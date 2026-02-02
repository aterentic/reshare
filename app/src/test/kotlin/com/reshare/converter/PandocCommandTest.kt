package com.reshare.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Unit tests for Pandoc command building and error mapping.
 * These run on JVM without Android dependencies.
 */
class PandocCommandTest {

    private val pandocPath = "/data/app/lib/libpandoc.so"

    @Test
    fun `builds correct command for markdown to html`() {
        val inputFile = File("/tmp/input.md")
        val outputFile = File("/tmp/output.html")

        val command = PandocConverter.buildCommand(
            pandocPath = pandocPath,
            inputFormat = InputFormat.MARKDOWN,
            outputFormat = OutputFormat.HTML,
            inputFile = inputFile,
            outputFile = outputFile
        )

        assertEquals(
            listOf(pandocPath, "-f", "markdown", "-t", "html", "/tmp/input.md", "-o", "/tmp/output.html"),
            command
        )
    }

    @Test
    fun `builds correct command for plain to docx`() {
        val inputFile = File("/tmp/input.txt")
        val outputFile = File("/tmp/output.docx")

        val command = PandocConverter.buildCommand(
            pandocPath = pandocPath,
            inputFormat = InputFormat.PLAIN,
            outputFormat = OutputFormat.DOCX,
            inputFile = inputFile,
            outputFile = outputFile
        )

        // PLAIN uses "markdown" reader as a passthrough
        assertEquals(
            listOf(pandocPath, "-f", "markdown", "-t", "docx", "/tmp/input.txt", "-o", "/tmp/output.docx"),
            command
        )
    }

    @Test
    fun `builds command without input file for stdin mode`() {
        val outputFile = File("/tmp/output.html")

        val command = PandocConverter.buildCommand(
            pandocPath = pandocPath,
            inputFormat = InputFormat.MARKDOWN,
            outputFormat = OutputFormat.HTML,
            inputFile = null,
            outputFile = outputFile
        )

        assertEquals(
            listOf(pandocPath, "-f", "markdown", "-t", "html", "-o", "/tmp/output.html"),
            command
        )
    }

    @Test
    fun `builds correct command for docx to markdown`() {
        val inputFile = File("/tmp/document.docx")
        val outputFile = File("/tmp/output.md")

        val command = PandocConverter.buildCommand(
            pandocPath = pandocPath,
            inputFormat = InputFormat.DOCX,
            outputFormat = OutputFormat.MARKDOWN,
            inputFile = inputFile,
            outputFile = outputFile
        )

        assertEquals(
            listOf(pandocPath, "-f", "docx", "-t", "markdown", "/tmp/document.docx", "-o", "/tmp/output.md"),
            command
        )
    }

    @Test
    fun `builds correct command for html to plain`() {
        val inputFile = File("/tmp/page.html")
        val outputFile = File("/tmp/output.txt")

        val command = PandocConverter.buildCommand(
            pandocPath = pandocPath,
            inputFormat = InputFormat.HTML,
            outputFormat = OutputFormat.PLAIN,
            inputFile = inputFile,
            outputFile = outputFile
        )

        assertEquals(
            listOf(pandocPath, "-f", "html", "-t", "plain", "/tmp/page.html", "-o", "/tmp/output.txt"),
            command
        )
    }

    @Test
    fun `builds correct command for epub to html`() {
        val inputFile = File("/tmp/book.epub")
        val outputFile = File("/tmp/output.html")

        val command = PandocConverter.buildCommand(
            pandocPath = pandocPath,
            inputFormat = InputFormat.EPUB,
            outputFormat = OutputFormat.HTML,
            inputFile = inputFile,
            outputFile = outputFile
        )

        assertEquals(
            listOf(pandocPath, "-f", "epub", "-t", "html", "/tmp/book.epub", "-o", "/tmp/output.html"),
            command
        )
    }

    @Test
    fun `builds correct command for odt to docx`() {
        val inputFile = File("/tmp/document.odt")
        val outputFile = File("/tmp/output.docx")

        val command = PandocConverter.buildCommand(
            pandocPath = pandocPath,
            inputFormat = InputFormat.ODT,
            outputFormat = OutputFormat.DOCX,
            inputFile = inputFile,
            outputFile = outputFile
        )

        assertEquals(
            listOf(pandocPath, "-f", "odt", "-t", "docx", "/tmp/document.odt", "-o", "/tmp/output.docx"),
            command
        )
    }

    @Test
    fun `handles paths with spaces`() {
        val inputFile = File("/tmp/my documents/input file.md")
        val outputFile = File("/tmp/output folder/result.html")

        val command = PandocConverter.buildCommand(
            pandocPath = pandocPath,
            inputFormat = InputFormat.MARKDOWN,
            outputFormat = OutputFormat.HTML,
            inputFile = inputFile,
            outputFile = outputFile
        )

        assertEquals(
            listOf(
                pandocPath,
                "-f", "markdown",
                "-t", "html",
                "/tmp/my documents/input file.md",
                "-o", "/tmp/output folder/result.html"
            ),
            command
        )
    }

    // Error mapping tests

    @Test
    fun `maps exit code 0 to Success`() {
        val description = PandocConverter.mapExitCodeToDescription(0)
        assertEquals("Success", description)
    }

    @Test
    fun `maps exit code 1 to PandocIOError`() {
        val description = PandocConverter.mapExitCodeToDescription(1)
        assertEquals("PandocIOError", description)
    }

    @Test
    fun `maps exit code 21 to PandocUnknownReaderError`() {
        val description = PandocConverter.mapExitCodeToDescription(21)
        assertEquals("PandocUnknownReaderError", description)
    }

    @Test
    fun `maps exit code 22 to PandocUnknownWriterError`() {
        val description = PandocConverter.mapExitCodeToDescription(22)
        assertEquals("PandocUnknownWriterError", description)
    }

    @Test
    fun `maps exit code 64 to PandocParseError`() {
        val description = PandocConverter.mapExitCodeToDescription(64)
        assertEquals("PandocParseError", description)
    }

    @Test
    fun `maps unknown exit code to Unknown Pandoc error`() {
        val description = PandocConverter.mapExitCodeToDescription(999)
        assertEquals("Unknown Pandoc error", description)
    }

    // InputFormat tests

    @Test
    fun `InputFormat fromMimeType returns null for text plain to allow content sniffing`() {
        val format = InputFormat.fromMimeType("text/plain")
        assertNull(format)
    }

    @Test
    fun `InputFormat fromMimeType returns correct format for text markdown`() {
        val format = InputFormat.fromMimeType("text/markdown")
        assertEquals(InputFormat.MARKDOWN, format)
    }

    @Test
    fun `InputFormat fromMimeType returns correct format for text x-markdown`() {
        val format = InputFormat.fromMimeType("text/x-markdown")
        assertEquals(InputFormat.MARKDOWN, format)
    }

    @Test
    fun `InputFormat fromMimeType returns correct format for text html`() {
        val format = InputFormat.fromMimeType("text/html")
        assertEquals(InputFormat.HTML, format)
    }

    @Test
    fun `InputFormat fromMimeType returns correct format for docx`() {
        val format = InputFormat.fromMimeType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
        assertEquals(InputFormat.DOCX, format)
    }

    @Test
    fun `InputFormat fromMimeType returns correct format for odt`() {
        val format = InputFormat.fromMimeType("application/vnd.oasis.opendocument.text")
        assertEquals(InputFormat.ODT, format)
    }

    @Test
    fun `InputFormat fromMimeType returns correct format for epub`() {
        val format = InputFormat.fromMimeType("application/epub+zip")
        assertEquals(InputFormat.EPUB, format)
    }

    @Test
    fun `InputFormat fromMimeType returns null for unknown mime type`() {
        val format = InputFormat.fromMimeType("application/octet-stream")
        assertEquals(null, format)
    }

    // OutputFormat tests

    @Test
    fun `OutputFormat DOCX has correct properties`() {
        assertEquals("docx", OutputFormat.DOCX.pandocFlag)
        assertEquals("docx", OutputFormat.DOCX.extension)
        assertEquals(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            OutputFormat.DOCX.mimeType
        )
    }

    @Test
    fun `OutputFormat HTML has correct properties`() {
        assertEquals("html", OutputFormat.HTML.pandocFlag)
        assertEquals("html", OutputFormat.HTML.extension)
        assertEquals("text/html", OutputFormat.HTML.mimeType)
    }

    @Test
    fun `OutputFormat MARKDOWN has correct properties`() {
        assertEquals("markdown", OutputFormat.MARKDOWN.pandocFlag)
        assertEquals("md", OutputFormat.MARKDOWN.extension)
        assertEquals("text/markdown", OutputFormat.MARKDOWN.mimeType)
    }

    @Test
    fun `OutputFormat PLAIN has correct properties`() {
        assertEquals("plain", OutputFormat.PLAIN.pandocFlag)
        assertEquals("txt", OutputFormat.PLAIN.extension)
        assertEquals("text/plain", OutputFormat.PLAIN.mimeType)
    }

    // ConversionError tests

    @Test
    fun `ConversionError Timeout has correct message`() {
        val error = ConversionError.Timeout()
        assertEquals("Conversion timed out", error.message)
    }

    @Test
    fun `ConversionError ProcessFailed has correct message`() {
        val error = ConversionError.ProcessFailed(1, "file not found")
        assertTrue(error.message.contains("1"))
        assertTrue(error.message.contains("file not found"))
    }

    @Test
    fun `ConversionError FileTooLarge has correct message`() {
        val error = ConversionError.FileTooLarge(2_000_000, 1_000_000)
        assertTrue(error.message.contains("2000000"))
        assertTrue(error.message.contains("1000000"))
    }

    @Test
    fun `ConversionError UnsupportedFormat has correct message`() {
        val error = ConversionError.UnsupportedFormat("audio/mp3")
        assertTrue(error.message.contains("audio/mp3"))
    }
}
