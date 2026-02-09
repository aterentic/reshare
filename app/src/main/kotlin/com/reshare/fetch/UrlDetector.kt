package com.reshare.fetch

import java.net.URI

object UrlDetector {

    private val URL_PATTERN = Regex(
        """https?://[^\s<>"{}|\\^`\[\]]+""",
        RegexOption.IGNORE_CASE
    )

    private val TWITTER_HOSTS = setOf(
        "twitter.com", "www.twitter.com",
        "x.com", "www.x.com",
        "mobile.twitter.com", "mobile.x.com"
    )

    private val INSTAGRAM_HOSTS = setOf(
        "instagram.com", "www.instagram.com",
        "m.instagram.com"
    )

    /**
     * Extracts the first URL found in [text], or null if none found.
     */
    fun extractUrl(text: String): String? =
        URL_PATTERN.find(text)?.value

    /**
     * Returns true if [text] contains a URL.
     */
    fun isUrl(text: String): Boolean =
        URL_PATTERN.containsMatchIn(text)

    /**
     * Returns true if [url] points to Twitter/X.
     */
    fun isTwitterUrl(url: String): Boolean =
        hostOf(url) in TWITTER_HOSTS

    /**
     * Returns true if [url] points to Instagram.
     */
    fun isInstagramUrl(url: String): Boolean =
        hostOf(url) in INSTAGRAM_HOSTS

    private fun hostOf(url: String): String? =
        try { URI(url).host?.lowercase() } catch (_: Exception) { null }
}
