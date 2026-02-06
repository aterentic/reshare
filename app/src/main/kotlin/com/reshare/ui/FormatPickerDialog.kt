package com.reshare.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.reshare.R
import com.reshare.converter.InputFormat
import com.reshare.converter.OutputFormat

/**
 * Bottom sheet dialog for selecting output format.
 * Shows available formats, hiding the one matching the input format.
 */
class FormatPickerDialog : BottomSheetDialogFragment() {

    private var inputFormat: InputFormat? = null
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

        setupFormatButton(view.findViewById(R.id.btnPdf), OutputFormat.PDF)
        setupFormatButton(view.findViewById(R.id.btnDocx), OutputFormat.DOCX)
        setupFormatButton(view.findViewById(R.id.btnHtml), OutputFormat.HTML)
        setupFormatButton(view.findViewById(R.id.btnMarkdown), OutputFormat.MARKDOWN)
        setupFormatButton(view.findViewById(R.id.btnPlain), OutputFormat.PLAIN)
        setupFormatButton(view.findViewById(R.id.btnLatex), OutputFormat.LATEX)

        view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dismiss()
            onCancelled?.invoke()
        }

        hideMatchingFormat(view)
    }

    private fun setupFormatButton(button: ImageButton, format: OutputFormat) {
        button.setOnClickListener {
            dismiss()
            onFormatSelected?.invoke(format)
        }
    }

    private fun hideMatchingFormat(view: View) {
        val input = inputFormat ?: return

        val buttonToHide = when (input) {
            InputFormat.PLAIN -> view.findViewById<ImageButton>(R.id.btnPlain)
            InputFormat.MARKDOWN -> view.findViewById<ImageButton>(R.id.btnMarkdown)
            InputFormat.HTML -> view.findViewById<ImageButton>(R.id.btnHtml)
            InputFormat.DOCX -> view.findViewById<ImageButton>(R.id.btnDocx)
            InputFormat.LATEX -> view.findViewById<ImageButton>(R.id.btnLatex)
            InputFormat.ORG, InputFormat.ODT, InputFormat.EPUB -> null
        }

        buttonToHide?.visibility = View.GONE
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onCancelled?.invoke()
    }

    companion object {
        fun newInstance(
            inputFormat: InputFormat,
            onFormatSelected: (OutputFormat) -> Unit,
            onCancelled: () -> Unit
        ): FormatPickerDialog {
            return FormatPickerDialog().apply {
                this.inputFormat = inputFormat
                this.onFormatSelected = onFormatSelected
                this.onCancelled = onCancelled
            }
        }
    }
}
