package org.koreader.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ApkUpdaterTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun cleanupPreservesDocumentsAndLegacyDownloads() {
        val filesDir = temporary.newFolder("files")
        val book = File(filesDir, "book.fb2").apply { writeText("book") }
        val metadata = File(filesDir, "book.sdr").apply { mkdir() }
        val settings = File(metadata, "metadata.fb2.lua").apply { writeText("settings") }
        val legacyApk = File(filesDir, "old-update.apk").apply { writeText("legacy") }
        val ota = File(filesDir, "ota").apply { mkdir() }
        val update = File(ota, "update.apk").apply { writeText("update") }
        val partial = File(ota, "partial-download").apply { writeText("partial") }

        ApkUpdater.pruneDownloadsIn(filesDir)

        assertTrue(book.isFile)
        assertTrue(settings.isFile)
        assertTrue(legacyApk.isFile)
        assertFalse(update.exists())
        assertFalse(partial.exists())
        assertTrue(ota.isDirectory)
    }

    @Test
    fun cleanupWithoutOtaDirectoryLeavesFilesAlone() {
        val filesDir = temporary.newFolder("files")
        val book = File(filesDir, "book.epub").apply { writeText("book") }

        ApkUpdater.pruneDownloadsIn(filesDir)

        assertTrue(book.isFile)
        assertFalse(File(filesDir, "ota").exists())
    }
}
