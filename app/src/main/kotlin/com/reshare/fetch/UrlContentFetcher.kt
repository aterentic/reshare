package com.reshare.fetch

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

/**
 * Result of fetching content from a URL.
 */
sealed class FetchResult {
    data class Document(val content: ByteArray, val contentType: String, val sourceUrl: String) : FetchResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Document) return false
            return content.contentEquals(other.content) && contentType == other.contentType && sourceUrl == other.sourceUrl
        }
        override fun hashCode(): Int {
            var result = content.contentHashCode()
            result = 31 * result + contentType.hashCode()
            result = 31 * result + sourceUrl.hashCode()
            return result
        }
    }

    data class Image(val content: ByteArray, val contentType: String, val sourceUrl: String) : FetchResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Image) return false
            return content.contentEquals(other.content) && contentType == other.contentType && sourceUrl == other.sourceUrl
        }
        override fun hashCode(): Int {
            var result = content.contentHashCode()
            result = 31 * result + contentType.hashCode()
            result = 31 * result + sourceUrl.hashCode()
            return result
        }
    }

    data class Error(val message: String) : FetchResult()
}

/**
 * Fetches content from URLs with special handling for Twitter/X and Instagram.
 * Uses java.net.HttpURLConnection — no external dependencies.
 */
object UrlContentFetcher {

    private const val TIMEOUT_MS = 15_000
    private const val MAX_DOWNLOAD_BYTES = 10_000_000L
    private const val SYNDICATION_URL = "https://cdn.syndication.twimg.com/tweet-result"

    /**
     * Fetches content from [url], dispatching to platform-specific handlers.
     * Must be called from a background thread.
     */
    fun fetch(url: String): FetchResult = try {
        when {
            UrlDetector.isTwitterUrl(url) -> fetchTwitter(url)
            UrlDetector.isInstagramUrl(url) -> fetchInstagram(url)
            else -> fetchGeneric(url)
        }
    } catch (e: IOException) {
        FetchResult.Error("Network error: ${e.message}")
    } catch (e: Exception) {
        FetchResult.Error("Failed to fetch: ${e.message}")
    }

    /**
     * Extracts the numeric tweet ID from a Twitter/X URL path like /user/status/123456.
     */
    internal fun extractTweetId(url: String): String? {
        val path = try { URI(url).path } catch (_: Exception) { return null }
        val segments = path.trimEnd('/').split('/')
        val statusIdx = segments.indexOf("status")
        if (statusIdx < 0 || statusIdx + 1 >= segments.size) return null
        val id = segments[statusIdx + 1]
        return if (id.all { it.isDigit() } && id.isNotEmpty()) id else null
    }

    private fun fetchTwitter(url: String): FetchResult {
        val tweetId = extractTweetId(url)
            ?: return FetchResult.Error("Could not extract tweet ID from URL")

        // Try syndication JSON API (works for many tweets, no auth required)
        val syndicationResult = trySyndicationApi(tweetId, url)
        if (syndicationResult != null) return syndicationResult

        // Fallback: return a minimal HTML document with the link
        val html = buildFallbackHtml("Tweet", url)
        return FetchResult.Document(html.toByteArray(Charsets.UTF_8), "text/html", url)
    }

    private fun trySyndicationApi(tweetId: String, sourceUrl: String): FetchResult? {
        return try {
            val apiUrl = "$SYNDICATION_URL?id=$tweetId&token=0"
            val conn = openConnection(apiUrl)
            try {
                conn.connect()
                if (conn.responseCode !in 200..299) return null
                val json = conn.inputStream.use { it.readNBytes(MAX_DOWNLOAD_BYTES.toInt()) }.decodeToString()

                val text = extractJsonString(json, "text") ?: return null
                val userName = extractJsonString(json, "name") ?: ""
                val screenName = extractJsonString(json, "screen_name") ?: ""
                val createdAt = extractJsonString(json, "created_at") ?: ""

                val html = buildTweetHtml(text, userName, screenName, createdAt, sourceUrl)
                FetchResult.Document(html.toByteArray(Charsets.UTF_8), "text/html", sourceUrl)
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchInstagram(url: String): FetchResult {
        val pageHtml = httpGet(url).decodeToString()
        val description = extractMetaContent(pageHtml, "og:description") ?: ""
        val imageUrl = extractMetaContent(pageHtml, "og:image")
        val title = extractMetaContent(pageHtml, "og:title") ?: "Instagram Post"
        val fullHtml = buildInstagramHtml(title, description, imageUrl, url)
        return FetchResult.Document(fullHtml.toByteArray(Charsets.UTF_8), "text/html", url)
    }

    private fun fetchGeneric(url: String): FetchResult {
        val conn = openConnection(url)
        try {
            conn.connect()
            val code = conn.responseCode
            if (code !in 200..299) {
                return FetchResult.Error("HTTP $code")
            }
            val contentType = conn.contentType?.lowercase() ?: "application/octet-stream"
            val bytes = conn.inputStream.use { it.readNBytes(MAX_DOWNLOAD_BYTES.toInt()) }

            return if (contentType.startsWith("image/")) {
                FetchResult.Image(bytes, contentType.substringBefore(';').trim(), url)
            } else {
                FetchResult.Document(bytes, contentType.substringBefore(';').trim(), url)
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun httpGet(url: String): ByteArray {
        val conn = openConnection(url)
        try {
            conn.connect()
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IOException("HTTP $code for $url")
            }
            return conn.inputStream.use { it.readNBytes(MAX_DOWNLOAD_BYTES.toInt()) }
        } finally {
            conn.disconnect()
        }
    }

    private fun openConnection(url: String): HttpURLConnection {
        val conn = URI(url).toURL().openConnection() as HttpURLConnection
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; ReShare/1.0)")
        return conn
    }

    // --- Parsing helpers (internal for testing) ---

    internal fun extractJsonString(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"".toRegex()
        val match = pattern.find(json) ?: return null
        val start = match.range.last + 1
        val sb = StringBuilder()
        var i = start
        while (i < json.length) {
            val c = json[i]
            if (c == '"') break
            if (c == '\\' && i + 1 < json.length) {
                i++
                when (json[i]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> sb.append('\r')
                    'u' -> {
                        if (i + 4 < json.length) {
                            val hex = json.substring(i + 1, i + 5)
                            sb.append(hex.toInt(16).toChar())
                            i += 4
                        }
                    }
                    else -> { sb.append('\\'); sb.append(json[i]) }
                }
            } else {
                sb.append(c)
            }
            i++
        }
        return sb.toString()
    }

    internal fun extractMetaContent(html: String, property: String): String? {
        // Match <meta property="og:..." content="..."> or <meta content="..." property="og:...">
        val patterns = listOf(
            """<meta[^>]*property\s*=\s*"${Regex.escape(property)}"[^>]*content\s*=\s*"([^"]*)"[^>]*/?>""",
            """<meta[^>]*content\s*=\s*"([^"]*)"[^>]*property\s*=\s*"${Regex.escape(property)}"[^>]*/?>"""
        )
        for (p in patterns) {
            val match = Regex(p, RegexOption.IGNORE_CASE).find(html)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    private fun buildTweetHtml(text: String, userName: String, screenName: String, createdAt: String, sourceUrl: String): String = buildString {
        val displayName = if (userName.isNotBlank()) userName else screenName
        append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
        append("<title>Tweet by $displayName</title></head><body>")
        if (displayName.isNotBlank()) append("<p><strong>$displayName</strong>")
        if (screenName.isNotBlank()) append(" <em>@$screenName</em>")
        if (displayName.isNotBlank()) append("</p>")
        append("<blockquote><p>$text</p></blockquote>")
        if (createdAt.isNotBlank()) append("<p><small>$createdAt</small></p>")
        append("<p><a href=\"$sourceUrl\">Source</a></p>")
        append("</body></html>")
    }

    private fun buildFallbackHtml(title: String, sourceUrl: String): String = buildString {
        append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
        append("<title>$title</title></head><body>")
        append("<p><a href=\"$sourceUrl\">$sourceUrl</a></p>")
        append("</body></html>")
    }

    private fun buildInstagramHtml(title: String, description: String, imageUrl: String?, sourceUrl: String): String = buildString {
        append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
        append("<title>$title</title></head><body>")
        append("<h1>$title</h1>")
        if (description.isNotBlank()) append("<p>$description</p>")
        if (imageUrl != null) append("<img src=\"$imageUrl\" alt=\"Instagram image\">")
        append("<p><a href=\"$sourceUrl\">Source</a></p>")
        append("</body></html>")
    }
}
