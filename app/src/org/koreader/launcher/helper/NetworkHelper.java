package org.koreader.launcher.helper;

import java.io.File;
import java.util.Locale;

import android.app.DownloadManager;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.text.format.Formatter;

import org.koreader.launcher.Logger;


public class NetworkHelper extends BaseHelper {

    private final WifiManager wifi;

    public NetworkHelper(Context context) {
        super(context);
        this.wifi = (WifiManager) 
            getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public int isWifi() {
        return wifi.isWifiEnabled() ? 1 : 0;
    }

    public void setWifi(final boolean state) {
        wifi.setWifiEnabled(state);
    }

    /**
     * Basic network information
     *
     * @return a string containing wifi name, ip and gateway.
     */

    public String info() {
        final WifiInfo wi = wifi.getConnectionInfo();
        final DhcpInfo dhcp = wifi.getDhcpInfo();
        int ip = wi.getIpAddress();
        int gw = dhcp.gateway;
        String ip_address = formatIp(ip);
        String gw_address = formatIp(gw);
        return String.format(Locale.US,"%s;%s;%s", wi.getSSID(), ip_address, gw_address);
    }

    /**
     * Download a file
     *
     * @param url   - the full qualified url to the file you'll want to download
     * @param name  - the name of the file with the extension (ie: "foo.mp4")
     *
     * @return        1 if the file is already downloaded, 0 otherwise.
     */

    public int download(final String url, final String name) {
        File file = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS) + "/" + name);

        if (file.exists()) {
            Logger.w(getTag(), "File already exists: skipping download");
            return 1;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
        DownloadManager manager = (DownloadManager)
            getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
        return 0;
    }

    private String formatIp(int number) {
        if (number > 0) {
            return Formatter.formatIpAddress(number);
        } else {
            return String.valueOf(number);
        }
    }
}
