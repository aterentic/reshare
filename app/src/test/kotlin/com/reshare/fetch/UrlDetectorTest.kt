package com.reshare.fetch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlDetectorTest {

    @Test
    fun `extractUrl finds https URL in text`() {
        val text = "Check out https://example.com/page for details"
        assertEquals("https://example.com/page", UrlDetector.extractUrl(text))
    }

    @Test
    fun `extractUrl finds http URL in text`() {
        val text = "Visit http://example.com"
        assertEquals("http://example.com", UrlDetector.extractUrl(text))
    }

    @Test
    fun `extractUrl returns null for plain text`() {
        assertNull(UrlDetector.extractUrl("no links here"))
    }

    @Test
    fun `extractUrl returns first URL when multiple present`() {
        val text = "https://first.com and https://second.com"
        assertEquals("https://first.com", UrlDetector.extractUrl(text))
    }

    @Test
    fun `extractUrl handles URL with path and query`() {
        val text = "https://example.com/path?q=test&page=1#section"
        assertEquals(text, UrlDetector.extractUrl(text))
    }

    @Test
    fun `isUrl returns true for text containing URL`() {
        assertTrue(UrlDetector.isUrl("see https://example.com"))
    }

    @Test
    fun `isUrl returns false for plain text`() {
        assertFalse(UrlDetector.isUrl("just some text"))
    }

    @Test
    fun `isTwitterUrl matches twitter dot com`() {
        assertTrue(UrlDetector.isTwitterUrl("https://twitter.com/user/status/123"))
    }

    @Test
    fun `isTwitterUrl matches x dot com`() {
        assertTrue(UrlDetector.isTwitterUrl("https://x.com/user/status/123"))
    }

    @Test
    fun `isTwitterUrl matches www prefix`() {
        assertTrue(UrlDetector.isTwitterUrl("https://www.twitter.com/user"))
        assertTrue(UrlDetector.isTwitterUrl("https://www.x.com/user"))
    }

    @Test
    fun `isTwitterUrl matches mobile prefix`() {
        assertTrue(UrlDetector.isTwitterUrl("https://mobile.twitter.com/user"))
    }

    @Test
    fun `isTwitterUrl rejects non-twitter URLs`() {
        assertFalse(UrlDetector.isTwitterUrl("https://example.com"))
        assertFalse(UrlDetector.isTwitterUrl("https://nottwitter.com"))
    }

    @Test
    fun `isInstagramUrl matches instagram dot com`() {
        assertTrue(UrlDetector.isInstagramUrl("https://instagram.com/p/abc123"))
    }

    @Test
    fun `isInstagramUrl matches www prefix`() {
        assertTrue(UrlDetector.isInstagramUrl("https://www.instagram.com/p/abc123"))
    }

    @Test
    fun `isInstagramUrl matches mobile prefix`() {
        assertTrue(UrlDetector.isInstagramUrl("https://m.instagram.com/p/abc123"))
    }

    @Test
    fun `isInstagramUrl rejects non-instagram URLs`() {
        assertFalse(UrlDetector.isInstagramUrl("https://example.com"))
    }

    @Test
    fun `extractUrl handles empty string`() {
        assertNull(UrlDetector.extractUrl(""))
    }

    @Test
    fun `extractUrl does not match ftp URLs`() {
        assertNull(UrlDetector.extractUrl("ftp://files.example.com/file.txt"))
    }
}
