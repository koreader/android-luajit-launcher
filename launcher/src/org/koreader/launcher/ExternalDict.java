package org.koreader.launcher;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;


public class ExternalDict {

    private final static String TAG = "DICT";

    /* do a word lookup on a thirdparty android application
     *
     * @param context of the activity that does the lookup
     * @param text to search
     * @param package that receives the query
     * @param action associated with the package
     */

    public static void lookup(Context context, String text, String pkg, String action) {
        try {
            Intent intent = new Intent(getIntentByAction(text, pkg, action));
            context.startActivity(intent);
        } catch (Exception e) {
            Logger.e(TAG, e.toString());
        }
    }

    public static boolean isAvailable(Context context, String pkg) {
        try {
            // is the package available (installed and enabled) ?
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
            boolean enabled = pm.getApplicationInfo(pkg, 0).enabled;
            Logger.d(TAG, String.format("Package %s is installed. Enabled? -> %s", pkg, Boolean.toString(enabled)));
            return enabled;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.d(TAG, String.format("Package %s is not installed.", pkg));
            return false;
        }
    }

    /* get intent by action type.
     *
     * @param text to search
     * @param package that receives the query
     * @param action associated to the package
     *
     * @returns a Intent based on package/action ready to do a text lookup
     */

    private static Intent getIntentByAction(String text, String pkg, String action) {
        Intent intent = new Intent();
        if ("send".equals(action)) {
            // Intent.ACTION_SEND
            intent = new Intent(getSendIntent(text, pkg));
        } else if ("search".equals(action)) {
            // Intent.ACTION_SEARCH
            intent = new Intent(getSearchIntent(text, pkg));
        } else if ("text".equals(action)) {
            // Intent.ACTION_PROCESS_TEXT
            intent = new Intent(getTextIntent(text, pkg));
        } else if ("colordict".equals(action)) {
            // colordict.intent.action.SEARCH
            intent = new Intent(getColordictIntent(text, pkg));
        } else if ("aard2".equals(action)) {
            // aard2.lookup
            intent = new Intent(getAard2Intent(text));
        } else if ("quickdic".equals(action)) {
            // com.hughes.action.ACTION_SEARCH_DICT
            intent = new Intent(getQuickdicIntent(text));
        }
        return intent;
    }

    /* Android common intents -------- */

    private static Intent getSendIntent(String text, String pkg) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setType("text/plain");
        intent.setPackage(pkg);
        return intent;
    }

    private static Intent getSearchIntent(String text, String pkg) {
        Intent intent = new Intent(Intent.ACTION_SEARCH);
        intent.putExtra(SearchManager.QUERY, text);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setPackage(pkg);
        return intent;
    }

    private static Intent getTextIntent(String text, String pkg) {
        Intent intent = new Intent(Intent.ACTION_PROCESS_TEXT);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.putExtra(Intent.EXTRA_PROCESS_TEXT, text);
        intent.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, text);
        intent.setType("text/plain");
        intent.setPackage(pkg);
        return intent;
    }

    /* Android custom intents for some dict apps ------ */

    private static Intent getAard2Intent(String text) {
        Intent intent = new Intent("aard2.lookup");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(SearchManager.QUERY, text);
        return intent;
    }

    private static Intent getColordictIntent(String text, String pkg) {
        Intent intent = new Intent("colordict.intent.action.SEARCH");
        intent.putExtra("EXTRA_QUERY", text);
        intent.putExtra("EXTRA_FULLSCREEN", true);
        intent.setPackage(pkg);
        return intent;
    }

    private static Intent getQuickdicIntent(String text) {
        Intent intent = new Intent("com.hughes.action.ACTION_SEARCH_DICT");
        intent.putExtra(SearchManager.QUERY, text);
        return intent;
    }
}
