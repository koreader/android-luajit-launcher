package org.koreader.launcher;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Build;

class IntentUtils {

    /**
     * get intent by action type, used to do dict lookups on 3rd party apps.
     *
     * @param text to search
     * @param pkg that receives the query
     * @param action associated to the package
     *
     * @return a Intent based on package/action ready to do a text lookup
     */

    static Intent getByAction(String text, String pkg, String action) {
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
        } else if ("picker-send".equals(action)) {
            // app picker for Intent.ACTION_SEND
            intent = new Intent(getSendIntent(text));
        } else if ("picker-search".equals(action)) {
            // app picker for Intent.ACTION_SEARCH
            intent = new Intent(getSearchIntent(text));
        } else if ("picker-text".equals(action)) {
            // app picker for Intent.ACTION_PROCESS_TEXT
            intent = new Intent(getTextIntent(text));
        }

        return intent;
    }

    /**
     * intent to string, based on https://stackoverflow.com/a/36842135
     *
     * @param intent -
     * @return String with action and dataString of the intent
     */

    static String intentToString(Intent intent) {
        if (intent == null) return "";
        return "\naction: " + intent.getAction() + "\ndata: " + intent.getDataString() + "\n";
    }

    /* Android common intents -------- */

    // Intent.ACTION_SEND with the app picker
    private static Intent getSendIntent(String text) {
        return getSendIntent(text, null);
    }

    // Intent.ACTION_SEND with a specific package
    private static Intent getSendIntent(String text, String pkg) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setType("text/plain");
        if (pkg != null) intent.setPackage(pkg);
        return intent;
    }

    // Intent.ACTION_SEARCH with the app picker
    private static Intent getSearchIntent(String text) {
        return getSearchIntent(text, null);
    }

    // Intent.ACTION_SEARCH with a specific package
    private static Intent getSearchIntent(String text, String pkg) {
        Intent intent = new Intent(Intent.ACTION_SEARCH);
        intent.putExtra(SearchManager.QUERY, text);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        if (pkg != null) intent.setPackage(pkg);
        return intent;
    }

    // Intent.ACTION_PROCESS_TEXT with the app picker
    private static Intent getTextIntent(String text) {
        return getTextIntent(text, null);
    }

    // Intent.ACTION_PROCESS_TEXT with a specific package (available on api23+)
    private static Intent getTextIntent(String text, String pkg) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Intent.ACTION_PROCESS_TEXT);
            intent.putExtra(Intent.EXTRA_TEXT, text);
            intent.putExtra(Intent.EXTRA_PROCESS_TEXT, text);
            intent.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, text);
            intent.setType("text/plain");
            if (pkg != null) intent.setPackage(pkg);
            return intent;
        } else {
            // fallback to ACTION_SEND
            return getSendIntent(text);
        }
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
