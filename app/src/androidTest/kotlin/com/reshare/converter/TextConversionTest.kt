package com.reshare.converter

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextConversionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var converter: PandocConverter

    @Before
    fun setup() {
        converter = PandocConverter(context)
    }

    // txt -> html
    @Test
    fun convertsPlainTextToHtml() {
        val input = loadAsset("input/simple.txt")
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = input,
                contentUri = null,
                inputFormat = InputFormat.PLAIN
            ),
            OutputFormat.HTML
        )

        assertThat(result.isSuccess).isTrue()
        val output = result.getOrThrow()
        assertThat(output.exists()).isTrue()
        val content = output.readText()
        assertThat(content).contains("<p>")
        assertThat(content).contains("Hello World")
        assertThat(content).contains("paragraph")
    }

    // txt -> markdown
    @Test
    fun convertsPlainTextToMarkdown() {
        val input = loadAsset("input/simple.txt")
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = input,
                contentUri = null,
                inputFormat = InputFormat.PLAIN
            ),
            OutputFormat.MARKDOWN
        )

        assertThat(result.isSuccess).isTrue()
        val output = result.getOrThrow()
        assertThat(output.exists()).isTrue()
        val content = output.readText()
        assertThat(content).contains("Hello World")
        assertThat(content).contains("paragraph")
    }

    // txt -> docx
    @Test
    fun convertsPlainTextToDocx() {
        val input = loadAsset("input/simple.txt")
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = input,
                contentUri = null,
                inputFormat = InputFormat.PLAIN
            ),
            OutputFormat.DOCX
        )

        assertThat(result.isSuccess).isTrue()
        val output = result.getOrThrow()
        assertThat(output.exists()).isTrue()
        assertThat(output.length()).isGreaterThan(0L)
    }

    // md -> html
    @Test
    fun convertsMarkdownToHtml() {
        val input = loadAsset("input/simple.md")
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = input,
                contentUri = null,
                inputFormat = InputFormat.MARKDOWN
            ),
            OutputFormat.HTML
        )

        assertThat(result.isSuccess).isTrue()
        val output = result.getOrThrow()
        assertThat(output.exists()).isTrue()
        val content = output.readText()
        assertThat(content).contains("<h1")
        assertThat(content).contains("Hello World")
        assertThat(content).contains("<a href=")
        assertThat(content).contains("<strong>")
        assertThat(content).contains("<em>")
        assertThat(content).contains("<li>")
    }

    // md -> docx
    @Test
    fun convertsMarkdownToDocx() {
        val input = loadAsset("input/simple.md")
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = input,
                contentUri = null,
                inputFormat = InputFormat.MARKDOWN
            ),
            OutputFormat.DOCX
        )

        assertThat(result.isSuccess).isTrue()
        val output = result.getOrThrow()
        assertThat(output.exists()).isTrue()
        assertThat(output.length()).isGreaterThan(0L)
    }

    // md -> plain
    @Test
    fun convertsMarkdownToPlain() {
        val input = loadAsset("input/simple.md")
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = input,
                contentUri = null,
                inputFormat = InputFormat.MARKDOWN
            ),
            OutputFormat.PLAIN
        )

        assertThat(result.isSuccess).isTrue()
        val output = result.getOrThrow()
        assertThat(output.exists()).isTrue()
        val content = output.readText()
        assertThat(content).contains("Hello World")
        assertThat(content).contains("bold")
        assertThat(content).contains("italic")
        // Should not contain markdown syntax
        assertThat(content).doesNotContain("**")
        assertThat(content).doesNotContain("*italic*")
    }

    // html -> md
    @Test
    fun convertsHtmlToMarkdown() {
        val input = loadAsset("input/simple.html")
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = input,
                contentUri = null,
                inputFormat = InputFormat.HTML
            ),
            OutputFormat.MARKDOWN
        )

        assertThat(result.isSuccess).isTrue()
        val output = result.getOrThrow()
        assertThat(output.exists()).isTrue()
        val content = output.readText()
        assertThat(content).contains("Hello World")
        assertThat(content).contains("**bold**")
        assertThat(content).contains("*italic*")
    }

    // html -> plain
    @Test
    fun convertsHtmlToPlain() {
        val input = loadAsset("input/simple.html")
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = input,
                contentUri = null,
                inputFormat = InputFormat.HTML
            ),
            OutputFormat.PLAIN
        )

        assertThat(result.isSuccess).isTrue()
        val output = result.getOrThrow()
        assertThat(output.exists()).isTrue()
        val content = output.readText()
        assertThat(content).contains("Hello World")
        assertThat(content).contains("bold")
        // Should not contain HTML tags
        assertThat(content).doesNotContain("<h1>")
        assertThat(content).doesNotContain("<strong>")
        assertThat(content).doesNotContain("<p>")
    }

    // html -> docx
    @Test
    fun convertsHtmlToDocx() {
        val input = loadAsset("input/simple.html")
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = input,
                contentUri = null,
                inputFormat = InputFormat.HTML
            ),
            OutputFormat.DOCX
        )

        assertThat(result.isSuccess).isTrue()
        val output = result.getOrThrow()
        assertThat(output.exists()).isTrue()
        assertThat(output.length()).isGreaterThan(0L)
    }

    // latex -> html
    @Test
    fun convertsLatexToHtml() {
        val input = loadAsset("input/simple.tex")
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = input,
                contentUri = null,
                inputFormat = InputFormat.LATEX
            ),
            OutputFormat.HTML
        )

        assertThat(result.isSuccess).isTrue()
        val output = result.getOrThrow()
        assertThat(output.exists()).isTrue()
        val content = output.readText()
        assertThat(content).contains("Hello World")
        assertThat(content).contains("<strong>")
        assertThat(content).contains("<em>")
        assertThat(content).contains("<li>")
    }

    // latex -> markdown
    @Test
    fun convertsLatexToMarkdown() {
        val input = loadAsset("input/simple.tex")
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = input,
                contentUri = null,
                inputFormat = InputFormat.LATEX
            ),
            OutputFormat.MARKDOWN
        )

        assertThat(result.isSuccess).isTrue()
        val output = result.getOrThrow()
        assertThat(output.exists()).isTrue()
        val content = output.readText()
        assertThat(content).contains("Hello World")
        assertThat(content).contains("bold")
        assertThat(content).contains("italic")
    }

    // latex -> plain
    @Test
    fun convertsLatexToPlain() {
        val input = loadAsset("input/simple.tex")
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = input,
                contentUri = null,
                inputFormat = InputFormat.LATEX
            ),
            OutputFormat.PLAIN
        )

        assertThat(result.isSuccess).isTrue()
        val output = result.getOrThrow()
        assertThat(output.exists()).isTrue()
        val content = output.readText()
        assertThat(content).contains("Hello World")
        assertThat(content).contains("bold")
        // Should not contain LaTeX commands
        assertThat(content).doesNotContain("\\textbf")
        assertThat(content).doesNotContain("\\begin")
    }

    // md -> latex
    @Test
    fun convertsMarkdownToLatex() {
        val input = loadAsset("input/simple.md")
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = input,
                contentUri = null,
                inputFormat = InputFormat.MARKDOWN
            ),
            OutputFormat.LATEX
        )

        assertThat(result.isSuccess).isTrue()
        val output = result.getOrThrow()
        assertThat(output.exists()).isTrue()
        val content = output.readText()
        assertThat(content).contains("Hello World")
        assertThat(content).contains("\\textbf")
        assertThat(content).contains("\\emph")
    }

    // html -> latex
    @Test
    fun convertsHtmlToLatex() {
        val input = loadAsset("input/simple.html")
        val result = converter.convert(
            PandocConverter.ConversionInput(
                content = input,
                contentUri = null,
                inputFormat = InputFormat.HTML
            ),
            OutputFormat.LATEX
        )

        assertThat(result.isSuccess).isTrue()
        val output = result.getOrThrow()
        assertThat(output.exists()).isTrue()
        val content = output.readText()
        assertThat(content).contains("Hello World")
        assertThat(content).contains("\\textbf")
        assertThat(content).contains("\\emph")
    }

    private fun loadAsset(path: String): ByteArray {
        return context.assets.open(path).use { it.readBytes() }
    }
}
