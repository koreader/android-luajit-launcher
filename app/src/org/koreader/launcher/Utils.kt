import android.app.DownloadManager
import android.content.ClipboardManager
import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager


internal object Utils {

    fun getClipboardManager(context: Context): ClipboardManager {
        return context.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE)
            as ClipboardManager
    }

    fun getDownloadManager(context: Context): DownloadManager {
        return context.applicationContext.getSystemService(Context.DOWNLOAD_SERVICE)
            as DownloadManager
    }

    fun getPowerManager(context: Context): PowerManager {
        return context.applicationContext.getSystemService(Context.POWER_SERVICE)
            as PowerManager
    }

    fun getWifiManager(context: Context): WifiManager {
        return context.applicationContext.getSystemService(Context.WIFI_SERVICE)
            as WifiManager
    }
}
