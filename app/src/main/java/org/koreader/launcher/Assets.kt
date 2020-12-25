package org.koreader.launcher

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.res.AssetManager
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import org.koreader.launcher.utils.FileUtils
import java.io.*

class Assets {

    companion object {
        private const val BUNDLE = "module"
        private const val MAP = "${BUNDLE}/map.txt"
        private const val VERSION = "${BUNDLE}/version.txt"
        private const val INSTALLED_VERSION = "git-rev"
    }

    init {
        System.loadLibrary("7z")
    }

    fun extract(activity: Activity): Boolean {
        return if (isNewBundle(activity)) {
            val startTime = System.nanoTime()
            val result = bootstrap(activity)
            val elapsedTime = System.nanoTime() - startTime
            Logger.i("update installed in ${elapsedTime / 1000000} milliseconds")
            result
        } else {
            true
        }
    }

    private fun isNewBundle(context: Context): Boolean {
        val path = "${context.filesDir.absolutePath}/$INSTALLED_VERSION"
        return try {
            if (!File(path).exists()) {
                Logger.i("New install")
                return true
            }
            context.assets.open(VERSION).bufferedReader().use {
                val bundledVersion = it.readLine()
                val fileReader = FileReader(File(path).absolutePath)
                val bufferedReader = BufferedReader(fileReader)
                val installedVersion = bufferedReader.readLine()
                bufferedReader.close()
                return if (bundledVersion == installedVersion) {
                    Logger.i("Skip installation for revision $bundledVersion")
                    false
                } else {
                    Logger.i("Found new package revision $bundledVersion")
                    true
                }
            }
        } catch (e: Exception) {
            Logger.i("New install")
            true
        }
    }

    private fun bootstrap(activity: Activity): Boolean {
        val filesDir = activity.filesDir.absolutePath
        val nativeLibsDir = activity.applicationInfo.nativeLibraryDir
        activity.runOnUiThread { dialog = FramelessProgressDialog.show(activity, "") }
        activity.assets.list(BUNDLE)?.let { bundle ->
            for (asset in bundle) {
                Logger.i("Asset found", asset)
                val assetName = "$BUNDLE/$asset"
                if (assetName != VERSION) {
                    when {
                        (assetName == MAP) -> {
                            activity.assets.open(assetName).bufferedReader().use { text ->
                                Logger.v("Reading symlink from $assetName")
                                text.forEachLine { line ->
                                    try {
                                        val array = line.split(" ").toTypedArray()
                                        if (array.size == 2) {
                                            val link = "$filesDir/${array[0]}"
                                            val file = "$nativeLibsDir/${array[1]}"
                                            if (File(file).exists()) {
                                                FileUtils.symlink(link, file)
                                            } else {
                                                Logger.w("File $file does not exist, skipping")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                        (asset.endsWith("7z")) -> {
                            Logger.v("Uncompressing $assetName")
                            try {
                                val ok = (extract(activity.assets, assetName, filesDir) == 0)
                                if (!ok)
                                    return false
                            } catch (e: Exception) {
                                Logger.e("Error extracting 7z file: %e")
                                return false
                            }
                        }
                        else -> {
                            Logger.v("Extracting $assetName")
                            try {
                                val file = File(filesDir, asset)
                                val inputStream = activity.assets.open(assetName)
                                val outputStream = FileOutputStream(file)
                                inputStream.use { source ->
                                    outputStream.use { target ->
                                        source.copyTo(target)
                                    }
                                }
                                inputStream.close()
                                outputStream.flush()
                                outputStream.close()
                            } catch (e: IOException) {
                                Logger.w("Error copying $assetName:\n$e")
                            }
                        }
                    }
                }
            }
        }
        activity.runOnUiThread { dialog?.dismiss() }
        return true
    }

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

    private external fun extract(assetManager: AssetManager, payload: String, output: String): Int
}
