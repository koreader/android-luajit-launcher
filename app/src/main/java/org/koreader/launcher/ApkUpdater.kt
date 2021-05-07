package org.koreader.launcher

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

@Suppress("DEPRECATION")
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
        return if (MainApp.has_ota_updates) {
            val file = File(
                Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + name)
            val result = try {
                if (file.exists()) {
                    DOWNLOAD_EXISTS
                } else {
                    val request = DownloadManager.Request(Uri.parse(url))
                    request.setMimeType("application/vnd.android.package-archive")
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
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
            Logger.w(tag, "Download APK is not supported in ${MainApp.flavor}")
            DOWNLOAD_NOT_SUPPORTED
        }
    }

    fun install(context: Context) {
        if (MainApp.has_ota_updates) {
            downloadPath?.let { apk ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val uri = FileProvider.getUriForFile(context, MainApp.provider, File(apk))
                    val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
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
            Logger.w(tag, "Install APK is not supported in ${MainApp.flavor}")
        }
    }
}
