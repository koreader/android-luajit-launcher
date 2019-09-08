package org.koreader.launcher

import android.app.SearchManager
import android.content.Intent
import android.os.Build

internal object IntentUtils {

    /**
     * get intent by action type, used to do dict lookups on 3rd party apps.
     *
     * @param text to search
     * @param pkg that receives the query
     * @param action associated to the package
     *
     * @return a Intent based on package/action ready to do a text lookup
     */

    fun getByAction(text: String, pkg: String, action: String): Intent {
        var intent = Intent()
        if ("send" == action) {
            // Intent.ACTION_SEND
            intent = Intent(getSendIntent(text, pkg))
        } else if ("search" == action) {
            // Intent.ACTION_SEARCH
            intent = Intent(getSearchIntent(text, pkg))
        } else if ("text" == action) {
            // Intent.ACTION_PROCESS_TEXT
            intent = Intent(getTextIntent(text, pkg))
        } else if ("colordict" == action) {
            // colordict.intent.action.SEARCH
            intent = Intent(getColordictIntent(text, pkg))
        } else if ("aard2" == action) {
            // aard2.lookup
            intent = Intent(getAard2Intent(text))
        } else if ("quickdic" == action) {
            // com.hughes.action.ACTION_SEARCH_DICT
            intent = Intent(getQuickdicIntent(text))
        } else if ("picker-send" == action) {
            // app picker for Intent.ACTION_SEND
            intent = Intent(getSendIntent(text))
        } else if ("picker-search" == action) {
            // app picker for Intent.ACTION_SEARCH
            intent = Intent(getSearchIntent(text))
        } else if ("picker-text" == action) {
            // app picker for Intent.ACTION_PROCESS_TEXT
            intent = Intent(getTextIntent(text))
        }

        return intent
    }

    /**
     * intent to string, based on https://stackoverflow.com/a/36842135
     *
     * @param intent -
     * @return String with action and dataString of the intent
     */

    fun intentToString(intent: Intent?): String {
        return if (intent == null) "" else "\naction: " + intent.action +
            "\ndata: " + intent.dataString + "\n"
    }

    // Intent.ACTION_SEND with a specific package
    private fun getSendIntent(text: String, pkg: String? = null): Intent {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        intent.type = "text/plain"
        if (pkg != null) intent.setPackage(pkg)
        return intent
    }

    // Intent.ACTION_SEARCH with a specific package
    private fun getSearchIntent(text: String, pkg: String? = null): Intent {
        val intent = Intent(Intent.ACTION_SEARCH)
        intent.putExtra(SearchManager.QUERY, text)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        if (pkg != null) intent.setPackage(pkg)
        return intent
    }

    // Intent.ACTION_PROCESS_TEXT with a specific package (available on api23+)
    private fun getTextIntent(text: String, pkg: String? = null): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Intent.ACTION_PROCESS_TEXT)
            intent.putExtra(Intent.EXTRA_TEXT, text)
            intent.putExtra(Intent.EXTRA_PROCESS_TEXT, text)
            intent.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, text)
            intent.type = "text/plain"
            if (pkg != null) intent.setPackage(pkg)
            intent
        } else
            // fallback to ACTION_SEND
            return getSendIntent(text)
    }

    /* Android custom intents for some dict apps ------ */

    private fun getAard2Intent(text: String): Intent {
        val intent = Intent("aard2.lookup")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(SearchManager.QUERY, text)
        return intent
    }

    private fun getColordictIntent(text: String, pkg: String): Intent {
        val intent = Intent("colordict.intent.action.SEARCH")
        intent.putExtra("EXTRA_QUERY", text)
        intent.putExtra("EXTRA_FULLSCREEN", true)
        intent.setPackage(pkg)
        return intent
    }

    private fun getQuickdicIntent(text: String): Intent {
        val intent = Intent("com.hughes.action.ACTION_SEARCH_DICT")
        intent.putExtra(SearchManager.QUERY, text)
        return intent
    }
}
