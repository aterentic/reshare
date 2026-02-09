package com.reshare.fetch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlContentFetcherTest {

    // --- extractJsonString ---

    @Test
    fun `extractJsonString finds simple value`() {
        val json = """{"html": "hello world"}"""
        assertEquals("hello world", UrlContentFetcher.extractJsonString(json, "html"))
    }

    @Test
    fun `extractJsonString handles escaped quotes`() {
        val json = """{"html": "say \"hello\""}"""
        assertEquals("say \"hello\"", UrlContentFetcher.extractJsonString(json, "html"))
    }

    @Test
    fun `extractJsonString handles escaped backslash`() {
        val json = """{"html": "back\\slash"}"""
        assertEquals("back\\slash", UrlContentFetcher.extractJsonString(json, "html"))
    }

    @Test
    fun `extractJsonString handles newlines and tabs`() {
        val json = """{"html": "line1\nline2\ttab"}"""
        assertEquals("line1\nline2\ttab", UrlContentFetcher.extractJsonString(json, "html"))
    }

    @Test
    fun `extractJsonString handles unicode escapes`() {
        val json = """{"html": "\u0041\u0042"}"""
        assertEquals("AB", UrlContentFetcher.extractJsonString(json, "html"))
    }

    @Test
    fun `extractJsonString returns null for missing key`() {
        val json = """{"other": "value"}"""
        assertNull(UrlContentFetcher.extractJsonString(json, "html"))
    }

    @Test
    fun `extractJsonString handles multiple keys`() {
        val json = """{"author_name": "John", "html": "<blockquote>tweet</blockquote>"}"""
        assertEquals("John", UrlContentFetcher.extractJsonString(json, "author_name"))
        assertEquals("<blockquote>tweet</blockquote>", UrlContentFetcher.extractJsonString(json, "html"))
    }

    @Test
    fun `extractJsonString handles whitespace around colon`() {
        val json = """{"html" : "value"}"""
        assertEquals("value", UrlContentFetcher.extractJsonString(json, "html"))
    }

    // --- extractTweetId ---

    @Test
    fun `extractTweetId from twitter dot com URL`() {
        assertEquals("123456", UrlContentFetcher.extractTweetId("https://twitter.com/user/status/123456"))
    }

    @Test
    fun `extractTweetId from x dot com URL`() {
        assertEquals("789", UrlContentFetcher.extractTweetId("https://x.com/someone/status/789"))
    }

    @Test
    fun `extractTweetId with trailing slash`() {
        assertEquals("123", UrlContentFetcher.extractTweetId("https://x.com/user/status/123/"))
    }

    @Test
    fun `extractTweetId with query params`() {
        assertEquals("123", UrlContentFetcher.extractTweetId("https://x.com/user/status/123?s=20"))
    }

    @Test
    fun `extractTweetId returns null for non-status URL`() {
        assertNull(UrlContentFetcher.extractTweetId("https://x.com/user"))
    }

    @Test
    fun `extractTweetId returns null for non-numeric ID`() {
        assertNull(UrlContentFetcher.extractTweetId("https://x.com/user/status/abc"))
    }

    // --- extractMetaContent ---

    @Test
    fun `extractMetaContent finds og property`() {
        val html = """<meta property="og:description" content="Hello world" />"""
        assertEquals("Hello world", UrlContentFetcher.extractMetaContent(html, "og:description"))
    }

    @Test
    fun `extractMetaContent finds content-first order`() {
        val html = """<meta content="Hello world" property="og:description" />"""
        assertEquals("Hello world", UrlContentFetcher.extractMetaContent(html, "og:description"))
    }

    @Test
    fun `extractMetaContent returns null for missing property`() {
        val html = """<meta property="og:title" content="Title" />"""
        assertNull(UrlContentFetcher.extractMetaContent(html, "og:description"))
    }

    @Test
    fun `extractMetaContent handles extra attributes`() {
        val html = """<meta name="x" property="og:image" id="y" content="https://img.com/pic.jpg" />"""
        assertEquals("https://img.com/pic.jpg", UrlContentFetcher.extractMetaContent(html, "og:image"))
    }

    @Test
    fun `extractMetaContent is case insensitive for tag`() {
        val html = """<META property="og:title" content="Title" />"""
        assertEquals("Title", UrlContentFetcher.extractMetaContent(html, "og:title"))
    }

    @Test
    fun `extractMetaContent handles self-closing and non-self-closing`() {
        val html1 = """<meta property="og:title" content="A">"""
        val html2 = """<meta property="og:title" content="B"/>"""
        assertEquals("A", UrlContentFetcher.extractMetaContent(html1, "og:title"))
        assertEquals("B", UrlContentFetcher.extractMetaContent(html2, "og:title"))
    }
}
