package org.koreader.launcher;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;


public final class MainApp extends android.app.Application {

    private static String app_name;
    private static String assets_path;
    private static String library_path;
    private static String storage_path;

    private static boolean debuggable;
    private static boolean is_system_app;

    @Override
    public void onCreate() {
        super.onCreate();
        getAppInfo();
        Log.i(app_name, "Application started");
        Log.v(app_name, formatAppInfo());
    }

    public static String getName() {
        return app_name;
    }

    /* app information into a String */
    private String formatAppInfo() {
        StringBuilder sb = new StringBuilder(400);
        sb.append("Application info {\n  Flags: ");

        if (is_system_app) {
            sb.append("system");
        } else {
            sb.append("user");
        }
        if (debuggable) {
            sb.append(", debuggable");
        }

        sb.append("\n  Paths {")
            .append("\n    Assets: ")
            .append(assets_path)
            .append("\n    Library: ")
            .append(library_path)
            .append("\n    Storage: ")
            .append(storage_path)
            .append("\n  }\n}");

        return sb.toString();
    }

    /* get app information that doesn't change during the lifetime of the application. */
    private void getAppInfo() {
        try {
            final PackageManager pm = getPackageManager();
            final PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
            final ApplicationInfo ai = pm.getApplicationInfo(getPackageName(), 0);

            app_name = getString(pi.applicationInfo.labelRes);
            library_path = ai.nativeLibraryDir;
            assets_path = getFilesDir().getAbsolutePath();
            storage_path = Environment.getExternalStorageDirectory().getAbsolutePath();

            if ((ai.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) debuggable = true;
            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) == 1) is_system_app = true;

        } catch (Exception e) {
            app_name = "MainApp";
            assets_path = "?";
            library_path = "?";
            storage_path = "?";
        }
    }
}
