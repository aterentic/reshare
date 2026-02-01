package com.reshare.converter

import android.content.Context
import android.print.PdfPrinter
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Converts documents to PDF using the flow:
 * Input -> Pandoc -> HTML -> WebView -> PrintManager -> PDF
 *
 * Note: This must run on the Main thread because WebView requires it.
 */
class PdfConverter(private val context: Context) {

    /**
     * Converts the input document to PDF.
     *
     * @param input The input data and format
     * @return Result containing the output PDF file on success, or a ConversionError on failure
     */
    suspend fun convertToPdf(input: PandocConverter.ConversionInput): Result<File> = withContext(Dispatchers.Main) {
        try {
            // 1. Convert to HTML via Pandoc
            val pandocConverter = PandocConverter(context)
            val htmlFile = pandocConverter.convert(input, OutputFormat.HTML).getOrThrow()
            val html = htmlFile.readText()

            // 2. Render in headless WebView
            val webView = WebView(context).apply {
                settings.javaScriptEnabled = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
            }

            // Wait for page load
            suspendCancellableCoroutine { cont ->
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        cont.resume(Unit)
                    }

                    @Suppress("DEPRECATION")
                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        cont.resumeWithException(
                            ConversionError.ProcessFailed(errorCode, description ?: "WebView load failed")
                        )
                    }
                }
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }

            // 3. Print to PDF
            val outputDir = File(context.cacheDir, "converted")
            outputDir.mkdirs()
            val outputFile = File(outputDir, "${UUID.randomUUID()}.pdf")

            val printAdapter = webView.createPrintDocumentAdapter("document")
            printToPdf(printAdapter, outputFile)

            // Clean up the intermediate HTML file
            htmlFile.delete()

            Result.success(outputFile)
        } catch (e: ConversionError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(ConversionError.ProcessFailed(-1, e.message ?: "Unknown error"))
        }
    }

    private suspend fun printToPdf(
        adapter: android.print.PrintDocumentAdapter,
        outputFile: File
    ): Unit = suspendCancellableCoroutine { cont ->
        PdfPrinter.print(adapter, outputFile) { result ->
            when (result) {
                is PdfPrinter.PrintResult.Success -> cont.resume(Unit)
                is PdfPrinter.PrintResult.LayoutFailed ->
                    cont.resumeWithException(ConversionError.ProcessFailed(-1, result.error))
                is PdfPrinter.PrintResult.WriteFailed ->
                    cont.resumeWithException(ConversionError.ProcessFailed(-1, result.error))
            }
        }
    }
}
