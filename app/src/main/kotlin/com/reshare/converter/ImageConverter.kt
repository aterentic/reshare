package com.reshare.converter

import android.content.Context
import android.print.PdfPrinter
import java.util.Base64
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
 * Converts images to PDF by rendering them in a WebView and printing.
 * Must run on the Main thread (WebView requirement).
 */
class ImageConverter(private val context: Context) {

    suspend fun convertToPdf(imageBytes: ByteArray, mimeType: String): Result<File> = withContext(Dispatchers.Main) {
        try {
            val html = buildImageHtml(imageBytes, mimeType)

            val webView = WebView(context).apply {
                settings.javaScriptEnabled = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
            }

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

            val outputDir = File(context.cacheDir, "converted")
            outputDir.mkdirs()
            val outputFile = File(outputDir, "${UUID.randomUUID()}.pdf")

            val printAdapter = webView.createPrintDocumentAdapter("image")
            suspendCancellableCoroutine { cont ->
                PdfPrinter.print(printAdapter, outputFile) { result ->
                    when (result) {
                        is PdfPrinter.PrintResult.Success -> cont.resume(Unit)
                        is PdfPrinter.PrintResult.LayoutFailed ->
                            cont.resumeWithException(ConversionError.ProcessFailed(-1, result.error))
                        is PdfPrinter.PrintResult.WriteFailed ->
                            cont.resumeWithException(ConversionError.ProcessFailed(-1, result.error))
                    }
                }
            }

            Result.success(outputFile)
        } catch (e: ConversionError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(ConversionError.ProcessFailed(-1, e.message ?: "Unknown error"))
        }
    }

    companion object {
        /**
         * Builds an HTML document with the image embedded as a base64 data URI.
         * Exposed for testing.
         */
        internal fun buildImageHtml(imageBytes: ByteArray, mimeType: String): String {
            val base64 = Base64.getEncoder().encodeToString(imageBytes)
            return buildString {
                append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
                append("<style>")
                append("body { margin: 0; padding: 0; display: flex; justify-content: center; align-items: flex-start; }")
                append("img { max-width: 100%; height: auto; }")
                append("</style>")
                append("</head><body>")
                append("<img src=\"data:$mimeType;base64,$base64\" alt=\"Image\">")
                append("</body></html>")
            }
        }
    }
}
