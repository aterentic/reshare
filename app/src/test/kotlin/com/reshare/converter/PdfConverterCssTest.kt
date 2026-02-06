package com.reshare.converter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for PdfConverter CSS injection.
 * These run on JVM without Android dependencies.
 */
class PdfConverterCssTest {

    @Test
    fun `injectCss inserts style before closing head tag`() {
        val html = "<html><head><title>Test</title></head><body><p>Hello</p></body></html>"
        val css = "body { color: red; }"

        val result = PdfConverter.injectCss(html, css)

        assertTrue(result.contains("<style>\nbody { color: red; }\n</style>\n</head>"))
        assertTrue(result.contains("<title>Test</title>"))
    }

    @Test
    fun `injectCss handles uppercase HEAD tag`() {
        val html = "<html><HEAD></HEAD><body></body></html>"
        val css = "p { margin: 0; }"

        val result = PdfConverter.injectCss(html, css)

        assertTrue("CSS should be injected before closing head tag",
            result.contains("<style>\np { margin: 0; }\n</style>") && result.contains("</head>", ignoreCase = true))
    }

    @Test
    fun `injectCss prepends style when no head tag`() {
        val html = "<p>No head tag here</p>"
        val css = "p { font-size: 14px; }"

        val result = PdfConverter.injectCss(html, css)

        assertTrue(result.startsWith("<style>\np { font-size: 14px; }\n</style>"))
        assertTrue(result.endsWith("<p>No head tag here</p>"))
    }

    @Test
    fun `injectCss preserves original content`() {
        val html = "<html><head></head><body><h1>Title</h1><p>Content</p></body></html>"
        val css = "h1 { color: blue; }"

        val result = PdfConverter.injectCss(html, css)

        assertTrue(result.contains("<h1>Title</h1><p>Content</p>"))
    }
}
