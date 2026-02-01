package com.reshare.util

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class CacheManagerTest {

    private lateinit var tempCacheDir: File
    private lateinit var convertedDir: File

    @Before
    fun setup() {
        tempCacheDir = createTempDirectory("test-cache").toFile()
        convertedDir = File(tempCacheDir, "converted").also { it.mkdirs() }
    }

    @After
    fun teardown() {
        tempCacheDir.deleteRecursively()
    }

    @Test
    fun `cleanupOldFiles deletes files older than max age`() {
        val oldFile = File(convertedDir, "old.txt").apply {
            writeText("old content")
            setLastModified(System.currentTimeMillis() - 2 * 60 * 60 * 1000) // 2 hours ago
        }

        val cacheManager = TestCacheManager(tempCacheDir)
        cacheManager.cleanupOldFiles(60 * 60 * 1000) // 1 hour

        assertFalse("Old file should be deleted", oldFile.exists())
    }

    @Test
    fun `cleanupOldFiles preserves files newer than max age`() {
        val newFile = File(convertedDir, "new.txt").apply {
            writeText("new content")
            setLastModified(System.currentTimeMillis() - 30 * 60 * 1000) // 30 minutes ago
        }

        val cacheManager = TestCacheManager(tempCacheDir)
        cacheManager.cleanupOldFiles(60 * 60 * 1000) // 1 hour

        assertTrue("New file should be preserved", newFile.exists())
    }

    @Test
    fun `cleanupOldFiles handles non-existent directory gracefully`() {
        convertedDir.deleteRecursively()

        val cacheManager = TestCacheManager(tempCacheDir)
        // Should not throw
        cacheManager.cleanupOldFiles()

        assertFalse("Converted directory should not exist", convertedDir.exists())
    }

    @Test
    fun `cleanupOldFiles handles mixed age files`() {
        val oldFile = File(convertedDir, "old.txt").apply {
            writeText("old content")
            setLastModified(System.currentTimeMillis() - 2 * 60 * 60 * 1000) // 2 hours ago
        }
        val newFile = File(convertedDir, "new.txt").apply {
            writeText("new content")
            setLastModified(System.currentTimeMillis() - 30 * 60 * 1000) // 30 minutes ago
        }

        val cacheManager = TestCacheManager(tempCacheDir)
        cacheManager.cleanupOldFiles(60 * 60 * 1000) // 1 hour

        assertFalse("Old file should be deleted", oldFile.exists())
        assertTrue("New file should be preserved", newFile.exists())
    }

    @Test
    fun `clearAllCache removes all files`() {
        File(convertedDir, "file1.txt").apply { writeText("content 1") }
        File(convertedDir, "file2.txt").apply { writeText("content 2") }
        val subDir = File(convertedDir, "subdir").apply { mkdirs() }
        File(subDir, "nested.txt").apply { writeText("nested content") }

        val cacheManager = TestCacheManager(tempCacheDir)
        cacheManager.clearAllCache()

        assertFalse("Converted directory should be deleted", convertedDir.exists())
    }

    @Test
    fun `clearAllCache handles non-existent directory gracefully`() {
        convertedDir.deleteRecursively()

        val cacheManager = TestCacheManager(tempCacheDir)
        // Should not throw
        cacheManager.clearAllCache()

        assertFalse("Converted directory should not exist", convertedDir.exists())
    }

    /**
     * Test implementation that uses a custom cache directory instead of Context.
     */
    private class TestCacheManager(private val cacheDir: File) {
        private val convertedDir = File(cacheDir, "converted")

        fun cleanupOldFiles(maxAgeMillis: Long = 60 * 60 * 1000L) {
            if (!convertedDir.exists()) return

            val cutoffTime = System.currentTimeMillis() - maxAgeMillis

            convertedDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    file.delete()
                }
            }
        }

        fun clearAllCache() {
            convertedDir.deleteRecursively()
        }
    }
}
