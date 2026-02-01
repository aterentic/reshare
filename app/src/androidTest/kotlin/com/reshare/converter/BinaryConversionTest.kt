package com.reshare.converter

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

@RunWith(AndroidJUnit4::class)
class BinaryConversionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var converter: PandocConverter

    private lateinit var docxFixture: ByteArray
    private lateinit var odtFixture: ByteArray
    private lateinit var epubFixture: ByteArray

    @Before
    fun setup() {
        converter = PandocConverter(context)
        generateTestFixtures()
    }

    private fun generateTestFixtures() {
        val md = loadAsset("input/simple.md")

        // Generate DOCX from markdown
        val docxResult = converter.convert(
            PandocConverter.ConversionInput(
                content = md,
                contentUri = null,
                inputFormat = InputFormat.MARKDOWN
            ),
            OutputFormat.DOCX
        )
        assertThat(docxResult.isSuccess).isTrue()
        docxFixture = docxResult.getOrThrow().readBytes()

        // Generate ODT from markdown using Pandoc directly
        odtFixture = generateWithPandoc(md, "markdown", "odt")

        // Generate EPUB from markdown using Pandoc directly
        epubFixture = generateWithPandoc(md, "markdown", "epub")
    }

    private fun generateWithPandoc(input: ByteArray, inputFormat: String, outputFormat: String): ByteArray {
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val pandocPath = "$nativeLibDir/libpandoc.so"
        val outputFile = File(context.cacheDir, "fixture.$outputFormat")

        val process = ProcessBuilder(
            pandocPath,
            "-f", inputFormat,
            "-t", outputFormat,
            "-o", outputFile.absolutePath
        )
            .directory(context.cacheDir)
            .apply {
                environment()["LD_LIBRARY_PATH"] = nativeLibDir
            }
            .redirectErrorStream(true)
            .start()

        process.outputStream.use { it.write(input) }
        val completed = process.waitFor(30, TimeUnit.SECONDS)
        assertThat(completed).isTrue()
        assertThat(process.exitValue()).isEqualTo(0)
        assertThat(outputFile.exists()).isTrue()

        return outputFile.readBytes().also { outputFile.delete() }
    }

    // DOCX -> Markdown
    @Test
    fun convertsDocxToMarkdown() {
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = docxFixture,
                contentUri = null,
                inputFormat = InputFormat.DOCX
            ),
            OutputFormat.MARKDOWN
        )

        assertThat(result.isSuccess).isTrue()
        val content = result.getOrThrow().readText()
        assertThat(content).contains("Hello World")
        assertThat(content).contains("bold")
        assertThat(content).contains("italic")
    }

    // DOCX -> HTML
    @Test
    fun convertsDocxToHtml() {
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = docxFixture,
                contentUri = null,
                inputFormat = InputFormat.DOCX
            ),
            OutputFormat.HTML
        )

        assertThat(result.isSuccess).isTrue()
        val content = result.getOrThrow().readText()
        assertThat(content).contains("Hello World")
        assertThat(content).contains("<strong>")
        assertThat(content).contains("<em>")
    }

    // DOCX -> Plain
    @Test
    fun convertsDocxToPlain() {
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = docxFixture,
                contentUri = null,
                inputFormat = InputFormat.DOCX
            ),
            OutputFormat.PLAIN
        )

        assertThat(result.isSuccess).isTrue()
        val content = result.getOrThrow().readText()
        assertThat(content).contains("Hello World")
        assertThat(content).contains("bold")
        // Should not contain markdown or HTML syntax
        assertThat(content).doesNotContain("**")
        assertThat(content).doesNotContain("<")
    }

    // ODT -> Markdown
    @Test
    fun convertsOdtToMarkdown() {
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = odtFixture,
                contentUri = null,
                inputFormat = InputFormat.ODT
            ),
            OutputFormat.MARKDOWN
        )

        assertThat(result.isSuccess).isTrue()
        val content = result.getOrThrow().readText()
        assertThat(content).contains("Hello World")
        assertThat(content).contains("bold")
        assertThat(content).contains("italic")
    }

    // ODT -> HTML
    @Test
    fun convertsOdtToHtml() {
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = odtFixture,
                contentUri = null,
                inputFormat = InputFormat.ODT
            ),
            OutputFormat.HTML
        )

        assertThat(result.isSuccess).isTrue()
        val content = result.getOrThrow().readText()
        assertThat(content).contains("Hello World")
        assertThat(content).contains("<strong>")
        assertThat(content).contains("<em>")
    }

    // ODT -> DOCX
    @Test
    fun convertsOdtToDocx() {
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = odtFixture,
                contentUri = null,
                inputFormat = InputFormat.ODT
            ),
            OutputFormat.DOCX
        )

        assertThat(result.isSuccess).isTrue()
        val output = result.getOrThrow()
        assertThat(output.exists()).isTrue()
        assertThat(output.length()).isGreaterThan(0L)

        // Verify valid DOCX structure
        ZipFile(output).use { zip ->
            assertThat(zip.getEntry("word/document.xml")).isNotNull()
            assertThat(zip.getEntry("[Content_Types].xml")).isNotNull()
        }
    }

    // EPUB -> HTML
    @Test
    fun convertsEpubToHtml() {
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = epubFixture,
                contentUri = null,
                inputFormat = InputFormat.EPUB
            ),
            OutputFormat.HTML
        )

        assertThat(result.isSuccess).isTrue()
        val content = result.getOrThrow().readText()
        assertThat(content).contains("Hello World")
    }

    // EPUB -> Markdown
    @Test
    fun convertsEpubToMarkdown() {
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = epubFixture,
                contentUri = null,
                inputFormat = InputFormat.EPUB
            ),
            OutputFormat.MARKDOWN
        )

        assertThat(result.isSuccess).isTrue()
        val content = result.getOrThrow().readText()
        assertThat(content).contains("Hello World")
    }

    // Verify DOCX output structure (round-trip: md -> docx -> verify)
    @Test
    fun verifyDocxOutputStructure() {
        val md = loadAsset("input/simple.md")
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = md,
                contentUri = null,
                inputFormat = InputFormat.MARKDOWN
            ),
            OutputFormat.DOCX
        )

        assertThat(result.isSuccess).isTrue()
        val docxFile = result.getOrThrow()

        // Verify it's a valid DOCX (ZIP with required entries)
        ZipFile(docxFile).use { zip ->
            assertThat(zip.getEntry("word/document.xml")).isNotNull()
            assertThat(zip.getEntry("[Content_Types].xml")).isNotNull()
            assertThat(zip.getEntry("_rels/.rels")).isNotNull()
        }
    }

    private fun loadAsset(path: String): ByteArray {
        return context.assets.open(path).use { it.readBytes() }
    }
}
