package com.reshare.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Unit tests for FormatDetector.
 * These run on JVM without Android dependencies by using companion object methods.
 */
class FormatDetectorTest {

    // Extension detection tests

    @Test
    fun `detectFromExtension returns PLAIN for txt files`() {
        assertEquals(InputFormat.PLAIN, FormatDetector.detectFromExtension("document.txt"))
    }

    @Test
    fun `detectFromExtension returns MARKDOWN for md files`() {
        assertEquals(InputFormat.MARKDOWN, FormatDetector.detectFromExtension("readme.md"))
    }

    @Test
    fun `detectFromExtension returns MARKDOWN for markdown files`() {
        assertEquals(InputFormat.MARKDOWN, FormatDetector.detectFromExtension("notes.markdown"))
    }

    @Test
    fun `detectFromExtension returns HTML for html files`() {
        assertEquals(InputFormat.HTML, FormatDetector.detectFromExtension("page.html"))
    }

    @Test
    fun `detectFromExtension returns HTML for htm files`() {
        assertEquals(InputFormat.HTML, FormatDetector.detectFromExtension("page.htm"))
    }

    @Test
    fun `detectFromExtension returns DOCX for docx files`() {
        assertEquals(InputFormat.DOCX, FormatDetector.detectFromExtension("document.docx"))
    }

    @Test
    fun `detectFromExtension returns ODT for odt files`() {
        assertEquals(InputFormat.ODT, FormatDetector.detectFromExtension("document.odt"))
    }

    @Test
    fun `detectFromExtension returns EPUB for epub files`() {
        assertEquals(InputFormat.EPUB, FormatDetector.detectFromExtension("book.epub"))
    }

    @Test
    fun `detectFromExtension is case insensitive`() {
        assertEquals(InputFormat.MARKDOWN, FormatDetector.detectFromExtension("README.MD"))
        assertEquals(InputFormat.HTML, FormatDetector.detectFromExtension("Page.HTML"))
        assertEquals(InputFormat.DOCX, FormatDetector.detectFromExtension("Document.DOCX"))
    }

    @Test
    fun `detectFromExtension returns null for unknown extensions`() {
        assertNull(FormatDetector.detectFromExtension("file.pdf"))
        assertNull(FormatDetector.detectFromExtension("file.xyz"))
        assertNull(FormatDetector.detectFromExtension("file.doc"))
    }

    @Test
    fun `detectFromExtension returns null for files without extension`() {
        assertNull(FormatDetector.detectFromExtension("README"))
        assertNull(FormatDetector.detectFromExtension("Makefile"))
    }

    @Test
    fun `detectFromExtension handles multiple dots in filename`() {
        assertEquals(InputFormat.MARKDOWN, FormatDetector.detectFromExtension("my.file.name.md"))
        assertEquals(InputFormat.HTML, FormatDetector.detectFromExtension("page.backup.html"))
    }

    // HTML content sniffing tests

    @Test
    fun `sniffContent detects HTML with DOCTYPE`() {
        val content = "<!DOCTYPE html><html><body>Hello</body></html>".toByteArray()
        assertEquals(InputFormat.HTML, FormatDetector.sniffContent(content))
    }

    @Test
    fun `sniffContent detects HTML with html tag`() {
        val content = "<html><head></head><body>Content</body></html>".toByteArray()
        assertEquals(InputFormat.HTML, FormatDetector.sniffContent(content))
    }

    @Test
    fun `sniffContent detects HTML case insensitive`() {
        val content = "<!doctype HTML><HTML><body>Hello</body></HTML>".toByteArray()
        assertEquals(InputFormat.HTML, FormatDetector.sniffContent(content))
    }

    @Test
    fun `sniffContent detects HTML with leading whitespace`() {
        val content = "   \n  <!DOCTYPE html><html></html>".toByteArray()
        assertEquals(InputFormat.HTML, FormatDetector.sniffContent(content))
    }

    // Markdown content sniffing tests

    @Test
    fun `sniffContent detects Markdown with h1 header`() {
        val content = "# Title\n\nSome content here.".toByteArray()
        assertEquals(InputFormat.MARKDOWN, FormatDetector.sniffContent(content))
    }

    @Test
    fun `sniffContent detects Markdown with h2 header`() {
        val content = "## Section\n\nMore content.".toByteArray()
        assertEquals(InputFormat.MARKDOWN, FormatDetector.sniffContent(content))
    }

    @Test
    fun `sniffContent detects Markdown with h6 header`() {
        val content = "###### Tiny header\n\nText.".toByteArray()
        assertEquals(InputFormat.MARKDOWN, FormatDetector.sniffContent(content))
    }

    @Test
    fun `sniffContent detects Markdown with link`() {
        val content = "Check out [this link](https://example.com) for more info.".toByteArray()
        assertEquals(InputFormat.MARKDOWN, FormatDetector.sniffContent(content))
    }

    @Test
    fun `sniffContent detects Markdown with code block`() {
        val content = "Here is some code:\n\n```\nprint('hello')\n```\n".toByteArray()
        assertEquals(InputFormat.MARKDOWN, FormatDetector.sniffContent(content))
    }

    @Test
    fun `sniffContent detects Markdown with fenced code block with language`() {
        val content = "Example:\n```kotlin\nfun main() = println(\"Hello\")\n```".toByteArray()
        assertEquals(InputFormat.MARKDOWN, FormatDetector.sniffContent(content))
    }

    @Test
    fun `sniffContent requires space after header hashes`() {
        // This should NOT be detected as markdown - no space after #
        val content = "#hashtag and #another without space".toByteArray()
        assertNull(FormatDetector.sniffContent(content))
    }

    // ZIP-based format sniffing tests

    @Test
    fun `sniffContent detects DOCX from ZIP with word directory`() {
        val content = createZipWithEntries("word/document.xml", "word/styles.xml")
        assertEquals(InputFormat.DOCX, FormatDetector.sniffContent(content))
    }

    @Test
    fun `sniffContent detects EPUB from ZIP with container xml`() {
        val content = createZipWithEntries("META-INF/container.xml", "OEBPS/content.opf")
        assertEquals(InputFormat.EPUB, FormatDetector.sniffContent(content))
    }

    @Test
    fun `sniffContent detects ODT from ZIP with content xml at root`() {
        val content = createZipWithEntries("content.xml", "styles.xml", "meta.xml")
        assertEquals(InputFormat.ODT, FormatDetector.sniffContent(content))
    }

    @Test
    fun `sniffContent returns null for unknown ZIP format`() {
        val content = createZipWithEntries("data/file.txt", "images/photo.jpg")
        assertNull(FormatDetector.sniffContent(content))
    }

    // Edge cases

    @Test
    fun `sniffContent returns null for empty content`() {
        assertNull(FormatDetector.sniffContent(ByteArray(0)))
    }

    @Test
    fun `sniffContent returns null for plain text without markers`() {
        val content = "This is just plain text without any special formatting.".toByteArray()
        assertNull(FormatDetector.sniffContent(content))
    }

    @Test
    fun `sniffContent returns null for binary data`() {
        val content = byteArrayOf(0x00, 0x01, 0x02, 0x03, 0xFF.toByte(), 0xFE.toByte())
        assertNull(FormatDetector.sniffContent(content))
    }

    @Test
    fun `sniffContent handles corrupted ZIP gracefully`() {
        // PK signature but invalid ZIP content
        val content = byteArrayOf(0x50, 0x4B, 0x00, 0x00, 0x00, 0x00)
        assertNull(FormatDetector.sniffContent(content))
    }

    // MIME type tests (using InputFormat.fromMimeType directly)

    @Test
    fun `fromMimeType returns PLAIN for text plain`() {
        assertEquals(InputFormat.PLAIN, InputFormat.fromMimeType("text/plain"))
    }

    @Test
    fun `fromMimeType returns MARKDOWN for text markdown`() {
        assertEquals(InputFormat.MARKDOWN, InputFormat.fromMimeType("text/markdown"))
    }

    @Test
    fun `fromMimeType returns MARKDOWN for text x-markdown`() {
        assertEquals(InputFormat.MARKDOWN, InputFormat.fromMimeType("text/x-markdown"))
    }

    @Test
    fun `fromMimeType returns HTML for text html`() {
        assertEquals(InputFormat.HTML, InputFormat.fromMimeType("text/html"))
    }

    @Test
    fun `fromMimeType returns DOCX for word document mime type`() {
        assertEquals(
            InputFormat.DOCX,
            InputFormat.fromMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
        )
    }

    @Test
    fun `fromMimeType returns ODT for odt mime type`() {
        assertEquals(
            InputFormat.ODT,
            InputFormat.fromMimeType("application/vnd.oasis.opendocument.text")
        )
    }

    @Test
    fun `fromMimeType returns EPUB for epub mime type`() {
        assertEquals(InputFormat.EPUB, InputFormat.fromMimeType("application/epub+zip"))
    }

    @Test
    fun `fromMimeType returns null for unknown mime type`() {
        assertNull(InputFormat.fromMimeType("application/octet-stream"))
        assertNull(InputFormat.fromMimeType("image/png"))
        assertNull(InputFormat.fromMimeType("application/pdf"))
    }

    // Extension map completeness test

    @Test
    fun `extension map contains all expected extensions`() {
        val expected = setOf("txt", "md", "markdown", "html", "htm", "docx", "odt", "epub")
        assertEquals(expected, FormatDetector.EXTENSION_MAP.keys)
    }

    // Helper function to create ZIP files for testing

    private fun createZipWithEntries(vararg entryNames: String): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            for (name in entryNames) {
                zos.putNextEntry(ZipEntry(name))
                zos.write("content".toByteArray())
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }
}
