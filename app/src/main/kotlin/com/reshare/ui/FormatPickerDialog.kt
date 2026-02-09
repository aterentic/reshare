package com.reshare.ui

import android.content.DialogInterface
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.materialswitch.MaterialSwitch
import com.reshare.R
import com.reshare.converter.InputFormat
import com.reshare.converter.OutputFormat
import com.reshare.share.SharePreferences

/**
 * Bottom sheet dialog for selecting output format.
 * Shows available formats, hiding the one matching the input format.
 * Favorites (toggled via long-press) appear first in the grid.
 */
class FormatPickerDialog : BottomSheetDialogFragment() {

    private var inputFormat: InputFormat? = null
    private var enabledFormats: Set<OutputFormat>? = null
    private var onFormatSelected: ((OutputFormat) -> Unit)? = null
    private var onCancelled: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_format_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dismiss()
            onCancelled?.invoke()
        }

        setupPlainTextSwitch(view)
        populateGrid(view)
    }

    private fun setupPlainTextSwitch(view: View) {
        val switch = view.findViewById<MaterialSwitch>(R.id.switchPlainText)
        val sharePreferences = SharePreferences(requireContext())
        switch.isChecked = sharePreferences.shareTextFormatsAsText
        switch.setOnCheckedChangeListener { _, checked ->
            sharePreferences.shareTextFormatsAsText = checked
        }
    }

    private fun populateGrid(view: View) {
        val grid = view.findViewById<GridLayout>(R.id.formatGrid)
        grid.removeAllViews()

        val context = requireContext()
        val formatPreferences = FormatPreferences(context)
        val favorites = formatPreferences.getFavorites()
        val visibleFormats = visibleFormats()

        val sorted = visibleFormats.sortedWith(compareByDescending<OutputFormat> { it in favorites }
            .thenBy { OutputFormat.entries.indexOf(it) })

        for (format in sorted) {
            val button = ImageButton(context).apply {
                setImageResource(formatDrawable(format))
                contentDescription = getString(formatContentDescription(format))
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                background = context.getDrawable(resolveSelectableBackground())
            }

            if (format in favorites) {
                applyFavoriteIndicator(button)
            }

            button.setOnClickListener {
                dismiss()
                onFormatSelected?.invoke(format)
            }

            button.setOnLongClickListener {
                val added = formatPreferences.toggleFavorite(format)
                val label = formatDisplayName(format)
                val message = if (added) {
                    getString(R.string.favorite_added, label)
                } else {
                    getString(R.string.favorite_removed, label)
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                populateGrid(view)
                true
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dpToPx(72)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            }
            grid.addView(button, params)
        }
    }

    private fun visibleFormats(): List<OutputFormat> {
        val input = inputFormat ?: return OutputFormat.entries
        val enabled = enabledFormats ?: OutputFormat.entries.toSet()

        val matchingOutput: OutputFormat? = when (input) {
            InputFormat.PLAIN -> OutputFormat.PLAIN
            InputFormat.MARKDOWN -> OutputFormat.MARKDOWN
            InputFormat.HTML -> OutputFormat.HTML
            InputFormat.DOCX -> OutputFormat.DOCX
            InputFormat.LATEX -> OutputFormat.LATEX
            InputFormat.PDF -> OutputFormat.PDF
            InputFormat.ORG, InputFormat.ODT, InputFormat.EPUB -> null
        }

        return OutputFormat.entries.filter { format ->
            format != matchingOutput && format in enabled
        }
    }

    private fun applyFavoriteIndicator(button: ImageButton) {
        val accentColor = resolveAccentColor()
        button.backgroundTintList = ColorStateList.valueOf(accentColor and 0x00FFFFFF or 0x20000000)
    }

    private fun resolveAccentColor(): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
        return typedValue.data
    }

    private fun resolveSelectableBackground(): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(
            android.R.attr.selectableItemBackgroundBorderless, typedValue, true
        )
        return typedValue.resourceId
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics
        ).toInt()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onCancelled?.invoke()
    }

    companion object {

        fun newInstance(
            inputFormat: InputFormat,
            enabledFormats: Set<OutputFormat> = OutputFormat.entries.toSet(),
            onFormatSelected: (OutputFormat) -> Unit,
            onCancelled: () -> Unit
        ): FormatPickerDialog {
            return FormatPickerDialog().apply {
                this.inputFormat = inputFormat
                this.enabledFormats = enabledFormats
                this.onFormatSelected = onFormatSelected
                this.onCancelled = onCancelled
            }
        }

        private fun formatDrawable(format: OutputFormat): Int = when (format) {
            OutputFormat.PDF -> R.drawable.ic_format_pdf
            OutputFormat.DOCX -> R.drawable.ic_format_docx
            OutputFormat.HTML -> R.drawable.ic_format_html
            OutputFormat.MARKDOWN -> R.drawable.ic_format_markdown
            OutputFormat.PLAIN -> R.drawable.ic_format_plain
            OutputFormat.LATEX -> R.drawable.ic_format_latex
        }

        private fun formatContentDescription(format: OutputFormat): Int = when (format) {
            OutputFormat.PDF -> R.string.format_pdf_desc
            OutputFormat.DOCX -> R.string.format_docx_desc
            OutputFormat.HTML -> R.string.format_html_desc
            OutputFormat.MARKDOWN -> R.string.format_markdown_desc
            OutputFormat.PLAIN -> R.string.format_plain_desc
            OutputFormat.LATEX -> R.string.format_latex_desc
        }

        private fun formatDisplayName(format: OutputFormat): String = when (format) {
            OutputFormat.PDF -> "PDF"
            OutputFormat.DOCX -> "DOCX"
            OutputFormat.HTML -> "HTML"
            OutputFormat.MARKDOWN -> "Markdown"
            OutputFormat.PLAIN -> "Plain Text"
            OutputFormat.LATEX -> "LaTeX"
        }
    }
}
