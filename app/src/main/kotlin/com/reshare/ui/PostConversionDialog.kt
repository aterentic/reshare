package com.reshare.ui

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Context
import com.reshare.R

/**
 * Shows a post-conversion dialog offering "Share" and "Save to..." options.
 */
object PostConversionDialog {

    fun show(
        context: Context,
        onShare: () -> Unit,
        onSaveTo: () -> Unit,
        onCancelled: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.conversion_complete)
            .setPositiveButton(R.string.share) { dialog, _ ->
                dialog.dismiss()
                onShare()
            }
            .setNeutralButton(R.string.save_to) { dialog, _ ->
                dialog.dismiss()
                onSaveTo()
            }
            .setOnCancelListener {
                onCancelled()
            }
            .show()
    }
}
