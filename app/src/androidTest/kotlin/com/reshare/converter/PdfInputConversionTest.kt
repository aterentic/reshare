package com.reshare.converter

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PdfInputConversionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val testContext: Context = InstrumentationRegistry.getInstrumentation().context
    private lateinit var converter: PandocConverter
    private lateinit var pdfFixture: ByteArray

    @Before
    fun setup() {
        converter = PandocConverter(context)
        pdfFixture = loadAsset("input/simple.pdf")
    }

    @Test
    fun pdfToHtmlBinaryExists() {
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val pdfToHtml = File(nativeLibDir, "libpdftohtml.so")
        assertThat(pdfToHtml.exists()).isTrue()
        assertThat(pdfToHtml.canExecute()).isTrue()
    }

    @Test
    fun convertsPdfToMarkdown() {
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = pdfFixture,
                contentUri = null,
                inputFormat = InputFormat.PDF
            ),
            OutputFormat.MARKDOWN
        )

        assertThat(result.isSuccess).isTrue()
        val content = result.getOrThrow().readText()
        assertThat(content).contains("Hello World")
    }

    @Test
    fun convertsPdfToHtml() {
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = pdfFixture,
                contentUri = null,
                inputFormat = InputFormat.PDF
            ),
            OutputFormat.HTML
        )

        assertThat(result.isSuccess).isTrue()
        val content = result.getOrThrow().readText()
        assertThat(content).contains("Hello World")
    }

    @Test
    fun convertsPdfToPlain() {
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = pdfFixture,
                contentUri = null,
                inputFormat = InputFormat.PDF
            ),
            OutputFormat.PLAIN
        )

        assertThat(result.isSuccess).isTrue()
        val content = result.getOrThrow().readText()
        assertThat(content).contains("Hello World")
    }

    @Test
    fun convertsPdfToDocx() {
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = pdfFixture,
                contentUri = null,
                inputFormat = InputFormat.PDF
            ),
            OutputFormat.DOCX
        )

        assertThat(result.isSuccess).isTrue()
        val output = result.getOrThrow()
        assertThat(output.exists()).isTrue()
        assertThat(output.length()).isGreaterThan(0L)
    }

    @Test
    fun convertsPdfToLatex() {
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = pdfFixture,
                contentUri = null,
                inputFormat = InputFormat.PDF
            ),
            OutputFormat.LATEX
        )

        assertThat(result.isSuccess).isTrue()
        val content = result.getOrThrow().readText()
        assertThat(content).contains("Hello World")
    }

    private fun loadAsset(path: String): ByteArray {
        return testContext.assets.open(path).use { it.readBytes() }
    }
}
