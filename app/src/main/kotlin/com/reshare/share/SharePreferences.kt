package com.reshare.share

import android.content.Context

/**
 * Reads and writes share-related preferences from SharedPreferences.
 */
class SharePreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Whether text-based formats (Markdown, HTML, LaTeX, plain text) should be
     * shared as plain text via EXTRA_TEXT instead of as file attachments.
     * Default: true.
     */
    var shareTextFormatsAsText: Boolean
        get() = prefs.getBoolean(KEY_SHARE_TEXT_AS_TEXT, true)
        set(value) = prefs.edit().putBoolean(KEY_SHARE_TEXT_AS_TEXT, value).apply()

    companion object {
        private const val PREFS_NAME = "share_prefs"
        private const val KEY_SHARE_TEXT_AS_TEXT = "share_text_formats_as_text"
    }
}
