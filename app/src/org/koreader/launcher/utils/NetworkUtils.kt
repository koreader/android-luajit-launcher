package org.koreader.launcher.utils

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import java.util.*

@Suppress("DEPRECATION")
object NetworkUtils {
    private const val ACTIVE_NETWORK_NONE = 0
    private const val ACTIVE_NETWORK_WIFI = 1
    private const val ACTIVE_NETWORK_MOBILE = 2
    private const val ACTIVE_NETWORK_ETHERNET = 3
    private const val ACTIVE_NETWORK_BLUETOOTH = 4
    private const val ACTIVE_NETWORK_VPN = 5

    fun getNetworkInfo(activity: Activity): String {
        val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager

        var connectionType: Int = ACTIVE_NETWORK_NONE
        val connected: Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                connectivityManager.activeNetwork?.let { net ->
                    connectivityManager.getNetworkCapabilities(net)?.let {
                        when {
                            it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                                connectionType = ACTIVE_NETWORK_WIFI
                                true
                            }
                            it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                                connectionType = ACTIVE_NETWORK_MOBILE
                                true
                            }
                            it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                                connectionType = ACTIVE_NETWORK_ETHERNET
                                true
                            }
                            it.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> {
                                connectionType = ACTIVE_NETWORK_BLUETOOTH
                                true
                            }
                            it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> {
                                connectionType = ACTIVE_NETWORK_VPN
                                true
                            }
                            else -> false
                        }
                    } ?: false
                } ?: false
            } else {
                connectivityManager.run {
                    connectivityManager.activeNetworkInfo?.run {
                        when (type) {
                            ConnectivityManager.TYPE_WIFI -> {
                                connectionType = ACTIVE_NETWORK_WIFI
                                true
                            }
                            ConnectivityManager.TYPE_MOBILE -> {
                                connectionType = ACTIVE_NETWORK_MOBILE
                                true
                            }
                            ConnectivityManager.TYPE_ETHERNET -> {
                                connectionType = ACTIVE_NETWORK_ETHERNET
                                true
                            }
                            ConnectivityManager.TYPE_BLUETOOTH -> {
                                connectionType = ACTIVE_NETWORK_BLUETOOTH
                                true
                            }
                            ConnectivityManager.TYPE_VPN -> {
                                connectionType = ACTIVE_NETWORK_VPN
                                true
                            }
                            else -> false
                        }
                    }
                } ?: false
            }
        return String.format(Locale.US, "%d;%d", if (connected) 1 else 0, connectionType)
    }
}
