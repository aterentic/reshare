package com.reshare.converter

import org.junit.Assert.assertTrue
import org.junit.Test

class ImageConverterTest {

    @Test
    fun `buildImageHtml contains data URI with correct mime type`() {
        val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47) // PNG magic bytes
        val html = ImageConverter.buildImageHtml(bytes, "image/png")
        assertTrue(html.contains("data:image/png;base64,"))
    }

    @Test
    fun `buildImageHtml contains img tag`() {
        val bytes = byteArrayOf(1, 2, 3)
        val html = ImageConverter.buildImageHtml(bytes, "image/jpeg")
        assertTrue(html.contains("<img src=\"data:image/jpeg;base64,"))
        assertTrue(html.contains("\" alt=\"Image\">"))
    }

    @Test
    fun `buildImageHtml is valid HTML document`() {
        val bytes = byteArrayOf(1, 2, 3)
        val html = ImageConverter.buildImageHtml(bytes, "image/webp")
        assertTrue(html.startsWith("<!DOCTYPE html><html>"))
        assertTrue(html.contains("<head>"))
        assertTrue(html.contains("</body></html>"))
    }

    @Test
    fun `buildImageHtml includes responsive styling`() {
        val bytes = byteArrayOf(1)
        val html = ImageConverter.buildImageHtml(bytes, "image/png")
        assertTrue(html.contains("max-width: 100%"))
    }

    @Test
    fun `buildImageHtml base64 encodes content`() {
        // "hello" -> "aGVsbG8=" in base64
        val bytes = "hello".toByteArray()
        val html = ImageConverter.buildImageHtml(bytes, "image/png")
        assertTrue(html.contains("aGVsbG8="))
    }
}
