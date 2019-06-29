package org.koreader.launcher;

import android.app.DownloadManager;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.text.format.Formatter;

import java.io.File;

public class NetworkManager {

    private Context context;
    private WifiManager wifi;

    public NetworkManager(Context context) {
        this.context = context.getApplicationContext();
        this.wifi =  (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
    }

    public int isWifi() {
        return wifi.isWifiEnabled() ? 1 : 0;
    }

    public void setWifi(final boolean state) {
        wifi.setWifiEnabled(state);
    }

    /** Basic network information
     *
     * @return a string containing wifi name, ip and gateway.
     */
        @SuppressWarnings("deprecation")
    public String info() {
        final WifiInfo wi = wifi.getConnectionInfo();
        final DhcpInfo dhcp = wifi.getDhcpInfo();
        int ip = wi.getIpAddress();
        int gw = dhcp.gateway;
        String ip_address = formatIp(ip);
        String gw_address = formatIp(gw);
        return String.format("%s;%s;%s", wi.getSSID(), ip_address, gw_address);
    }

    /** Download a file
     *
     * @param url   - the full qualified url to the file you'll want to download
     * @param name  - the name of the file with the extension (ie: "foo.mp4")
     *
     * @return        1 if the file is already downloaded, 0 otherwise.
     */

    public int download(final String url, final String name) {
        File file = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS) + "/" + name);

        if (file.exists()) return 1;

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
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


/*
    public void setWifiEnabled(final boolean enabled) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                getWifiManager().setWifiEnabled(enabled);
            }
        });
    }
 */
