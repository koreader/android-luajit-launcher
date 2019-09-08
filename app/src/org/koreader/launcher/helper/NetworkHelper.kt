package org.koreader.launcher.helper

import java.io.File
import java.util.Locale

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Environment
import android.text.format.Formatter

import org.koreader.launcher.Logger


class NetworkHelper(context: Context) : BaseHelper(context) {

    private val wifi: WifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE)

    val isWifi: Int
        get() = if (wifi.isWifiEnabled) 1 else 0

    init {
        this.wifi
    }

    fun setWifi(state: Boolean) {
        wifi.isWifiEnabled = state
    }

    /**
     * Basic network information
     *
     * @return a string containing wifi name, ip and gateway.
     */

    fun info(): String {
        val wi = wifi.connectionInfo
        val dhcp = wifi.dhcpInfo
        val ip = wi.ipAddress
        val gw = dhcp.gateway
        val ip_address = formatIp(ip)
        val gw_address = formatIp(gw)
        return String.format(Locale.US, "%s;%s;%s", wi.ssid, ip_address, gw_address)
    }

    /**
     * Download a file
     *
     * @param url   - the full qualified url to the file you'll want to download
     * @param name  - the name of the file with the extension (ie: "foo.mp4")
     *
     * @return        1 if the file is already downloaded, 0 otherwise.
     */

    fun download(url: String, name: String): Int {
        val file = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS).toString() + "/" + name)

        if (file.exists()) {
            Logger.w(tag, "File already exists: skipping download")
            return 1
        }

        val request = DownloadManager.Request(Uri.parse(url))
        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
        val manager: DownloadManager = applicationContext.getSystemService(Context.DOWNLOAD_SERVICE)
        manager.enqueue(request)
        return 0
    }

    private fun formatIp(number: Int): String {
        return if (number > 0) {
            Formatter.formatIpAddress(number)
        } else {
            number.toString()
        }
    }
}
