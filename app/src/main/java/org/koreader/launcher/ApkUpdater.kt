package org.koreader.launcher

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

class ApkUpdater {

    companion object {
        private const val DOWNLOAD_NOT_SUPPORTED = -2
        private const val DOWNLOAD_FAILED = -1
        private const val DOWNLOAD_OK = 0
        private const val DOWNLOAD_EXISTS = 1
    }
    private val tag = this::class.java.simpleName
    private var downloadPath: String? = null

    fun download(context: Context, url: String, name: String): Int {
        return if (MainApp.OTA_UPDATES) {
            val file = File(context.getExternalFilesDir(null), name)
            val result = try {
                if (file.exists()) {
                    DOWNLOAD_EXISTS
                } else {
                    val request = DownloadManager.Request(Uri.parse(url))
                    request.setMimeType("application/vnd.android.package-archive")
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                    request.setDestinationInExternalFilesDir(context, null, name)
                    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE)
                        as DownloadManager
                    manager.enqueue(request)
                    DOWNLOAD_OK
                }
            } catch (e: Exception) {
                e.printStackTrace()
                DOWNLOAD_FAILED
            }
            downloadPath = when (result) {
                DOWNLOAD_EXISTS,
                DOWNLOAD_OK -> {
                    file.absolutePath
                }
                else -> {
                    null
                }
            }
            result
        } else {
            Log.w(tag, "Download APK is not supported in ${MainApp.FLAVOR}")
            DOWNLOAD_NOT_SUPPORTED
        }
    }

    fun install(context: Context) {
        if (MainApp.OTA_UPDATES) {
            downloadPath?.let { apk ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val uri = FileProvider.getUriForFile(context, MainApp.PROVIDER, File(apk))
                    @Suppress("DEPRECATION") val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
                    intent.data = uri
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.startActivity(intent)
                } else {
                    val uri = Uri.fromFile(File(apk))
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setDataAndType(uri, "application/vnd.android.package-archive")
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                }
                downloadPath = null
            }
        } else {
            Log.w(tag, "Install APK is not supported in ${MainApp.FLAVOR}")
        }
    }
}
