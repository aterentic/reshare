package android.print

import android.os.ParcelFileDescriptor
import java.io.File

/**
 * Helper class to print PDF from a PrintDocumentAdapter.
 * Must be in the android.print package to access package-private callback constructors.
 */
object PdfPrinter {

    /**
     * Result of a PDF print operation.
     */
    sealed class PrintResult {
        data class Success(val file: File) : PrintResult()
        data class LayoutFailed(val error: String) : PrintResult()
        data class WriteFailed(val error: String) : PrintResult()
    }

    /**
     * Prints a document to PDF using the provided adapter.
     *
     * @param adapter The PrintDocumentAdapter from WebView
     * @param outputFile The file to write the PDF to
     * @param onComplete Callback invoked when the operation completes
     */
    fun print(
        adapter: PrintDocumentAdapter,
        outputFile: File,
        onComplete: (PrintResult) -> Unit
    ) {
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        adapter.onLayout(
            null,
            attributes,
            null,
            object : PrintDocumentAdapter.LayoutResultCallback() {
                override fun onLayoutFinished(info: PrintDocumentInfo?, changed: Boolean) {
                    val descriptor = try {
                        ParcelFileDescriptor.open(
                            outputFile,
                            ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_WRITE_ONLY
                        )
                    } catch (e: Exception) {
                        onComplete(PrintResult.WriteFailed("Failed to open output file: ${e.message}"))
                        return
                    }

                    adapter.onWrite(
                        arrayOf(PageRange.ALL_PAGES),
                        descriptor,
                        null,
                        object : PrintDocumentAdapter.WriteResultCallback() {
                            override fun onWriteFinished(pages: Array<out PageRange>?) {
                                try {
                                    descriptor.close()
                                } catch (_: Exception) {
                                    // Ignore close errors
                                }
                                onComplete(PrintResult.Success(outputFile))
                            }

                            override fun onWriteFailed(error: CharSequence?) {
                                try {
                                    descriptor.close()
                                } catch (_: Exception) {
                                    // Ignore close errors
                                }
                                onComplete(PrintResult.WriteFailed(error?.toString() ?: "PDF write failed"))
                            }
                        }
                    )
                }

                override fun onLayoutFailed(error: CharSequence?) {
                    onComplete(PrintResult.LayoutFailed(error?.toString() ?: "PDF layout failed"))
                }
            },
            null
        )
    }
}
