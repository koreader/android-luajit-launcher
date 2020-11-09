package org.koreader.launcher

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

object ApkUpdater {
    fun download(activity: Activity, url: String, name: String): Int {
        val request = DownloadManager.Request(Uri.parse(url))
        request.setTitle(name)
        request.allowScanningByMediaScanner()
        request.setMimeType("application/vnd.android.package-archive")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)

        /* Try to download the request. This *should* not fail, but it fails
           on some AOSP devices that don't need to pass google CTS. */
        return try {
            if (File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS).toString() + "/" + name).exists()) {
                Logger.w("File already exists: skipping download")
                return 1
            }
            val manager: DownloadManager = activity.applicationContext.getSystemService(
                Context.DOWNLOAD_SERVICE) as DownloadManager

            manager.enqueue(request)
            0
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }
}
