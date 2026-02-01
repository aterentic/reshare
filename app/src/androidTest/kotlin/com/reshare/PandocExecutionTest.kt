package com.reshare

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PandocExecutionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun pandocBinaryExists() {
        val pandocPath = "${context.applicationInfo.nativeLibraryDir}/libpandoc.so"
        assertThat(File(pandocPath).exists()).isTrue()
    }

    @Test
    fun pandocIsExecutable() {
        val nativeLibDir = context.applicationInfo.nativeLibraryDir
        val pandocPath = "$nativeLibDir/libpandoc.so"

        val process = ProcessBuilder(pandocPath, "--version")
            .apply {
                environment()["LD_LIBRARY_PATH"] = nativeLibDir
            }
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        assertThat(exitCode).isEqualTo(0)
        assertThat(output).contains("pandoc")
    }
}
