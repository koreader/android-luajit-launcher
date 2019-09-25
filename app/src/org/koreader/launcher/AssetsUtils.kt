package org.koreader.launcher

import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import android.content.Context

/* Utils to extract compressed assets from the asset loader */

internal object AssetsUtils {
    private const val TAG = "AssetsUtils"
    private const val BASE_BUFFER_SIZE = 1024

    /* copy raw assets from the assets module */
    fun copyRawAssets(context: Context): Boolean {
        val assetManager = context.assets
        val assetsDir = context.filesDir.absolutePath
        var entryPoint = false
        try {
            val assets = assetManager.list("module")
            if (assets != null) {
                for (asset in assets) {
                    val file = File(assetsDir, asset)
                    val input = assetManager.open("module/$asset")
                    val output = FileOutputStream(file)
                    copyFile(input, output)
                    input.close()
                    output.flush()
                    output.close()
                    // llapp_main.lua is the entry point for frontend code.
                    if ("llapp_main.lua" == asset) {
                        entryPoint = true
                    }
                }
            }
        } catch (e: IOException) {
            entryPoint = false
            Logger.e(TAG, "error copying raw assets: $e")
        }
        return entryPoint
    }

    /* get the first zip file inside the assets module */
    fun getZipFromAsset(context: Context): String? {
        val assetManager = context.assets
        try {
            val assets = assetManager.list("module")
            if (assets != null) {
                for (asset in assets) {
                    if (asset.endsWith(".zip")) {
                        return asset
                    }
                }
            }
            return null
        } catch (e: Exception) {
            Logger.e(TAG, "error finding a zip in assets store: $e")
            return null
        }
    }

    /* check if installed files have the same revision as the asset zipfile */
    fun isSameVersion(context: Context, zipFile: String): Boolean {
        val newVersion = getPackageRevision(zipFile)
        try {
            val output = context.filesDir.absolutePath
            val fileReader = FileReader("$output/git-rev")
            val bufferedReader = BufferedReader(fileReader)
            val installedVersion = bufferedReader.readLine()
            bufferedReader.close()
            return if (newVersion == installedVersion) {
                Logger.i("Skip installation for revision $newVersion")
                true
            } else {
                Logger.i("Found new package revision $newVersion")
                false
            }
        } catch (e: Exception) {
            Logger.w(TAG, "$e")
            Logger.i("Found new package revision $newVersion")
            return false
        }
    }

    /**
     * unzip contents of a zip file from stream into an absolute path.
     *
     * @param stream the InputStream where the zip file is stored
     * @param output full path to the folder where the zip is extracted
     * @param overwrite files on destination
     * @return boolean ok
     */
    fun unzip(stream: InputStream, output: String, overwrite: Boolean = true): Boolean {
        var error = false
        var newFiles = 0
        var updatedFiles = 0
        var skippedFiles = 0
        try {
            val inputStream = ZipInputStream(stream)
            var zipEntry: ZipEntry? = inputStream.nextEntry
            while (zipEntry != null) {
                if (zipEntry.isDirectory) {
                    dirChecker(output, zipEntry.name)
                } else {
                    val f = File(output, zipEntry.name)
                    if (f.exists()) {
                        if (overwrite) {
                            updatedFiles = ++updatedFiles
                        } else {
                            skippedFiles = ++skippedFiles
                        }
                    } else {
                        newFiles = ++newFiles
                        if (!f.createNewFile()) {
                            Logger.e(TAG, "Failed to create file " + f.name)
                            error = true
                            continue
                        }
                    }
                    if (f.exists() and overwrite) {
                        FileOutputStream(f).use { target ->
                            val buffer = ByteArray(16 * BASE_BUFFER_SIZE)
                            var len = inputStream.read(buffer)
                            while (len != -1) {
                                target.write(buffer, 0, len)
                                len = inputStream.read(buffer)
                            }
                            inputStream.closeEntry()
                        }
                    }
                }
                zipEntry = inputStream.nextEntry
            }
            inputStream.close()
            Logger.i(String.format(Locale.US,
                "Assets extracted without errors: %d updated, %d skipped, %d new",
                updatedFiles, skippedFiles, newFiles))
            return !error
        } catch (e: Exception) {
            Logger.e(TAG, "Error extracting assets: $e")
            return false
        }
    }

    /* get package revision from zipFile name. Zips must use the scheme: name-revision.zip */
    private fun getPackageRevision(zipFile: String): String {
        val zipName = zipFile.replace(".zip", "")
        val parts = zipName.split("-".toRegex()).dropLastWhile{ it.isEmpty() }.toTypedArray()
        return zipName.replace(parts[0] + "-", "")
    }

    /* copy files from stream */
    @Throws(IOException::class)
    private fun copyFile(input: InputStream, output: OutputStream) {
        try {
            input.use { source ->
                output.use { target ->
                    source.copyTo(target, BASE_BUFFER_SIZE)
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error copying file: $e")
        }
    }

    /* create new folders on demand */
    private fun dirChecker(path: String, file: String) {
        val f = File(path, file)
        if (!f.isDirectory) {
            val success = f.mkdirs()
            if (!success) {
                Logger.w(TAG, "failed to create folder " + f.name)
            }
        }
    }
}
