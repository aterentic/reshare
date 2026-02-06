package com.reshare.converter

/**
 * A visual template that can be applied to converted documents.
 *
 * @param id Stable identifier used for persistence
 * @param displayName Human-readable name shown in the picker
 * @param cssAssetPath Path within assets/ to the CSS file, or null for the default (no styling)
 */
data class Template(val id: String, val displayName: String, val cssAssetPath: String?) {

    companion object {
        val DEFAULT = Template("default", "Default", null)
        val CLEAN = Template("clean", "Clean", "templates/clean/style.css")
        val ACADEMIC = Template("academic", "Academic", "templates/academic/style.css")

        /** All built-in templates in display order. */
        val ALL = listOf(DEFAULT, CLEAN, ACADEMIC)

        fun fromId(id: String): Template = ALL.find { it.id == id } ?: DEFAULT
    }
}
