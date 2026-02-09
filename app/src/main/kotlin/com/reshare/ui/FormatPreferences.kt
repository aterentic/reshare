package com.reshare.ui

import android.content.Context
import com.reshare.converter.InputFormat
import com.reshare.converter.OutputFormat
import com.reshare.converter.Template
import org.json.JSONArray

/**
 * Reads and writes format preferences from SharedPreferences.
 *
 * Stores two independent sets: enabled input formats and enabled output formats.
 * All conversions between enabled inputs and enabled outputs are allowed.
 */
class FormatPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns the set of enabled input formats.
     * Default: all input formats enabled.
     */
    fun enabledInputFormats(): Set<InputFormat> {
        val json = prefs.getString(KEY_ENABLED_INPUTS, null) ?: return InputFormat.entries.toSet()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                InputFormat.entries.find { it.name == array.getString(i) }
            }.toSet()
        } catch (_: Exception) {
            InputFormat.entries.toSet()
        }
    }

    /**
     * Sets whether [format] is enabled as an input format.
     */
    fun setInputEnabled(format: InputFormat, enabled: Boolean) {
        val current = enabledInputFormats().toMutableSet()
        if (enabled) current.add(format) else current.remove(format)
        saveSet(KEY_ENABLED_INPUTS, current.map { it.name })
    }

    /**
     * Returns the set of enabled output formats (independent of input).
     * Default: all output formats enabled.
     */
    fun enabledOutputFormatsAll(): Set<OutputFormat> {
        val json = prefs.getString(KEY_ENABLED_OUTPUTS, null) ?: return OutputFormat.entries.toSet()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                OutputFormat.entries.find { it.name == array.getString(i) }
            }.toSet()
        } catch (_: Exception) {
            OutputFormat.entries.toSet()
        }
    }

    /**
     * Sets whether [format] is enabled as an output format.
     */
    fun setOutputEnabled(format: OutputFormat, enabled: Boolean) {
        val current = enabledOutputFormatsAll().toMutableSet()
        if (enabled) current.add(format) else current.remove(format)
        saveSet(KEY_ENABLED_OUTPUTS, current.map { it.name })
    }

    /**
     * Returns the set of enabled output formats for [inputFormat].
     * Returns empty if the input format itself is disabled.
     */
    fun enabledOutputFormats(inputFormat: InputFormat): Set<OutputFormat> {
        if (inputFormat !in enabledInputFormats()) return emptySet()
        return enabledOutputFormatsAll()
    }

    private fun saveSet(key: String, names: List<String>) {
        val array = JSONArray()
        for (name in names) array.put(name)
        prefs.edit().putString(key, array.toString()).apply()
    }

    /**
     * Returns the set of output formats marked as favorites.
     */
    fun getFavorites(): Set<OutputFormat> {
        val names = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        return names.mapNotNull { name ->
            OutputFormat.entries.find { it.name == name }
        }.toSet()
    }

    /**
     * Toggles the favorite status of [format].
     * Returns true if the format is now a favorite, false if removed.
     */
    fun toggleFavorite(format: OutputFormat): Boolean {
        val names = prefs.getStringSet(KEY_FAVORITES, emptySet())?.toMutableSet() ?: mutableSetOf()
        val added = if (format.name in names) {
            names.remove(format.name)
            false
        } else {
            names.add(format.name)
            true
        }
        prefs.edit().putStringSet(KEY_FAVORITES, names).apply()
        return added
    }

    /**
     * Returns the last-used template for [outputFormat], or [Template.DEFAULT] if none stored.
     */
    fun getLastTemplate(outputFormat: OutputFormat): Template {
        val id = prefs.getString("${KEY_TEMPLATE_PREFIX}${outputFormat.name}", null)
        return if (id != null) Template.fromId(id) else Template.DEFAULT
    }

    /**
     * Persists the last-used template for [outputFormat].
     */
    fun setLastTemplate(outputFormat: OutputFormat, template: Template) {
        prefs.edit().putString("${KEY_TEMPLATE_PREFIX}${outputFormat.name}", template.id).apply()
    }

    companion object {
        private const val PREFS_NAME = "format_prefs"
        private const val KEY_ENABLED_INPUTS = "enabled_inputs"
        private const val KEY_ENABLED_OUTPUTS = "enabled_outputs"
        private const val KEY_FAVORITES = "favorite_formats"
        private const val KEY_TEMPLATE_PREFIX = "template_"
    }
}
