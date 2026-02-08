package com.reshare.ui

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.reshare.R
import com.reshare.converter.OutputFormat
import com.reshare.converter.Template

/**
 * Shows a dialog for selecting a visual template to apply during conversion.
 * Only shown for formats that support styling (HTML, PDF).
 */
object TemplatePickerDialog {

    /** Output formats that support template styling. */
    private val TEMPLATE_FORMATS = setOf(OutputFormat.HTML, OutputFormat.PDF)

    /**
     * Returns true if [outputFormat] supports template selection.
     */
    fun supportsTemplates(outputFormat: OutputFormat): Boolean =
        outputFormat in TEMPLATE_FORMATS

    /**
     * Shows the template picker for [outputFormat].
     *
     * @param context Activity context for dialog display
     * @param outputFormat The selected output format (used to recall last-used template)
     * @param formatPreferences Preferences store for template persistence
     * @param onTemplateSelected Called with the chosen template
     * @param onCancelled Called when the user dismisses the dialog
     */
    fun show(
        context: Context,
        outputFormat: OutputFormat,
        formatPreferences: FormatPreferences,
        onTemplateSelected: (Template) -> Unit,
        onCancelled: () -> Unit
    ) {
        val templates = Template.ALL
        val names = templates.map { it.displayName }.toTypedArray()
        val lastUsed = formatPreferences.getLastTemplate(outputFormat)
        val checkedIndex = templates.indexOfFirst { it.id == lastUsed.id }.coerceAtLeast(0)
        var selectedIndex = checkedIndex

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.template_picker_title)
            .setSingleChoiceItems(names, checkedIndex) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val template = templates[selectedIndex]
                formatPreferences.setLastTemplate(outputFormat, template)
                onTemplateSelected(template)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                onCancelled()
            }
            .setOnCancelListener {
                onCancelled()
            }
            .show()
    }
}
