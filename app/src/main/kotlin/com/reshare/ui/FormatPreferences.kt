package com.reshare.ui

import android.content.Context
import com.reshare.converter.InputFormat
import com.reshare.converter.OutputFormat
import com.reshare.converter.Template
import org.json.JSONObject

/**
 * Reads and writes the format matrix from SharedPreferences.
 *
 * The matrix maps each [InputFormat] to the set of [OutputFormat]s that should
 * be offered in the format picker. Default: all output formats enabled for
 * every input format.
 */
class FormatPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns the set of enabled output formats for [inputFormat].
     */
    fun enabledOutputFormats(inputFormat: InputFormat): Set<OutputFormat> {
        val matrix = loadMatrix()
        return matrix[inputFormat] ?: OutputFormat.entries.toSet()
    }

    /**
     * Sets whether [outputFormat] is enabled for [inputFormat].
     */
    fun setEnabled(inputFormat: InputFormat, outputFormat: OutputFormat, enabled: Boolean) {
        val matrix = loadMatrix()
        val current = matrix[inputFormat] ?: OutputFormat.entries.toMutableSet()
        val updated = current.toMutableSet()
        if (enabled) updated.add(outputFormat) else updated.remove(outputFormat)
        matrix[inputFormat] = updated
        saveMatrix(matrix)
    }

    /**
     * Returns true if [outputFormat] is enabled for [inputFormat].
     */
    fun isEnabled(inputFormat: InputFormat, outputFormat: OutputFormat): Boolean {
        return outputFormat in enabledOutputFormats(inputFormat)
    }

    private fun loadMatrix(): MutableMap<InputFormat, Set<OutputFormat>> {
        val json = prefs.getString(KEY_FORMAT_MATRIX, null) ?: return defaultMatrix()
        return try {
            val root = JSONObject(json)
            val result = mutableMapOf<InputFormat, Set<OutputFormat>>()
            for (inputName in root.keys()) {
                val inputFormat = InputFormat.entries.find { it.name == inputName } ?: continue
                val array = root.getJSONArray(inputName)
                val outputs = mutableSetOf<OutputFormat>()
                for (i in 0 until array.length()) {
                    val outputFormat = OutputFormat.entries.find { it.name == array.getString(i) }
                    if (outputFormat != null) outputs.add(outputFormat)
                }
                result[inputFormat] = outputs
            }
            // Fill in any input formats missing from persisted data (new formats added later)
            for (input in InputFormat.entries) {
                if (input !in result) result[input] = OutputFormat.entries.toSet()
            }
            result
        } catch (e: Exception) {
            defaultMatrix()
        }
    }

    private fun saveMatrix(matrix: Map<InputFormat, Set<OutputFormat>>) {
        val root = JSONObject()
        for ((input, outputs) in matrix) {
            val array = org.json.JSONArray()
            for (output in outputs) array.put(output.name)
            root.put(input.name, array)
        }
        prefs.edit().putString(KEY_FORMAT_MATRIX, root.toString()).apply()
    }

    private fun defaultMatrix(): MutableMap<InputFormat, Set<OutputFormat>> {
        val all = OutputFormat.entries.toSet()
        return InputFormat.entries.associateWith { input ->
            when (input) {
                InputFormat.PDF -> all - OutputFormat.PDF
                else -> all
            }
        }.toMutableMap()
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
        private const val KEY_FORMAT_MATRIX = "format_matrix"
        private const val KEY_FAVORITES = "favorite_formats"
        private const val KEY_TEMPLATE_PREFIX = "template_"
    }
}
