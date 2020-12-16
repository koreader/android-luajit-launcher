package org.koreader.launcher

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.AssetManager
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import java.io.*

class Assets {
    companion object {
        private const val TAG = "AssetsHelper"
        private const val CONTAINER = "7z"
    }

    init {
        System.loadLibrary(CONTAINER)
    }

    fun extract(activity: Activity): Boolean {
        val output = activity.filesDir.absolutePath
        return try {
            // check if the app has zipped assets
            val payload = getFromAsset(activity)
            if (payload != null) {
                var ok = true
                Logger.i("Check file in asset module: $payload")
                // upgrade or downgrade files from zip
                if (!isSameVersion(activity, payload)) {
                    showProgress(activity) // show progress dialog (animated dots)
                    val startTime = System.nanoTime()
                    Logger.i("Installing new package to $output")
                    ok = uncompress(activity, "module/$payload", output)
                    val endTime = System.nanoTime()
                    val elapsedTime = endTime - startTime
                    Logger.i("update installed in ${elapsedTime/1000000} milliseconds")
                    dismissProgress(activity) // dismiss progress dialog
                }
                if (!ok) {
                    false
                } else {
                    copyLibs(activity)
                }
            } else {
                // check if the app has other, non-zipped, raw assets
                Logger.i("Zip file not found, trying raw assets...")
                copyRawAssets(activity)
            }
        } catch (e: IOException) {
            Logger.e(TAG, "error extracting assets:\n$e")
            dismissProgress(activity)
            false
        }
    }

    private external fun extract(assetManager: AssetManager, payload: String, output: String): Int

    private fun uncompress(activity: Activity, payload: String, output: String): Boolean {
        return try {
            (extract(activity.assets, payload, output) == 0)
        } catch (e: Exception) {
            Logger.w(TAG, "error extracting: %e")
            false
        }
    }

    private fun copyLibs(context: Context): Boolean {
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
    private fun copyRawAssets(context: Context): Boolean {
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
    private fun getFromAsset(context: Context): String? {
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
    private fun isSameVersion(context: Context, file: String): Boolean {
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
                    source.copyTo(target)
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error copying file: $e")
        }
    }

    /* dialog used while extracting assets from zip */
    private var dialog: FramelessProgressDialog? = null
    private class FramelessProgressDialog private constructor(context: Context):
        Dialog(context, R.style.FramelessDialog) {
        companion object {
            fun show(context: Context, title: CharSequence): FramelessProgressDialog {
                val dialog = FramelessProgressDialog(context)
                dialog.setTitle(title)
                dialog.setCancelable(false)
                dialog.setOnCancelListener(null)
                dialog.window?.setGravity(Gravity.BOTTOM)
                val progressBar = ProgressBar(context)
                try {
                    ContextCompat.getDrawable(context, R.drawable.discrete_spinner)
                        ?.let { spinDrawable -> progressBar.indeterminateDrawable = spinDrawable }
                } catch (e: Exception) {
                    Logger.w("Failed to set progress drawable:\n$e")
                }
                /* The next line will add the ProgressBar to the dialog. */
                dialog.addContentView(progressBar, ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                )
                dialog.show()
                return dialog
            }
        }
    }

    private fun showProgress(activity: Activity) {
        activity.runOnUiThread {
            dialog = FramelessProgressDialog.show(activity, "") }
    }

    private fun dismissProgress(activity: Activity) {
        activity.runOnUiThread {
            dialog?.dismiss()
        }
    }
}
