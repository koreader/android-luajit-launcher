package org.koreader.launcher

import android.app.Activity
import android.content.Context
import android.content.res.AssetManager
import java.io.*

class Assets() {
    companion object {
        private const val TAG = "AssetsHelper"
        private const val CONTAINER = "7z"
        private const val BUFFER_SIZE = 1024
    }

    init {
        System.loadLibrary(CONTAINER)
    }

    private external fun extract(assetManager: AssetManager, payload: String, output: String): Int

    fun uncompress(activity: Activity, payload: String, output: String): Boolean {
        return try {
            (extract(activity.assets, payload, output) == 0)
        } catch (e: Exception) {
            Logger.w(TAG, "error extracting: %e")
            false
        }
    }

    fun copyLibs(context: Context): Boolean {
        val assetManager = context.assets
        val libsDir = File(context.filesDir.absolutePath + "/libs")
        if (!libsDir.exists()) {
            libsDir.mkdir()
        }

        val libsPath = libsDir.absolutePath
        try {
            val assets = assetManager.list("libs")
            return if (assets != null) {
                for (asset in assets) {
                    val file = File(libsPath, asset)
                    val input = assetManager.open("libs/$asset")
                    val output = FileOutputStream(file)
                    copyFile(input, output)
                    input.close()
                    output.flush()
                    output.close()
                }
                true
            } else {
                Logger.i("No libraries to copy")
                true
            }
        } catch (e: IOException) {
            Logger.e(TAG, "error copying libraries: $e")
            return false
        }
    }

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

    /* get the first compressed file inside the assets module */
    fun getFromAsset(context: Context): String? {
        val assetManager = context.assets
        try {
            val assets = assetManager.list("module")
            if (assets != null) {
                for (asset in assets) {
                    if (asset.endsWith(CONTAINER)) {
                        return asset
                    }
                }
            }
            return null
        } catch (e: Exception) {
            Logger.e(TAG, "error finding a $CONTAINER in assets store: $e")
            return null
        }
    }

    /* check if installed files have the same revision as assets */
    fun isSameVersion(context: Context, file: String): Boolean {
        val newVersion = getPackageRevision(file)
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
            Logger.i("Found new package revision $newVersion")
            return false
        }
    }

    /* get package revision from zipFile name. Zips must use the scheme: name-revision.zip */
    private fun getPackageRevision(file: String): String {
        val suffix = String.format(".%s", CONTAINER)
        val name = file.replace(suffix, "")
        val parts = name.split("-".toRegex()).dropLastWhile{ it.isEmpty() }.toTypedArray()
        return name.replace(parts[0] + "-", "")
    }

    /* copy files from stream */
    @Throws(IOException::class)
    private fun copyFile(input: InputStream, output: OutputStream) {
        try {
            input.use { source ->
                output.use { target ->
                    source.copyTo(target, BUFFER_SIZE)
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error copying file: $e")
        }
    }
}
