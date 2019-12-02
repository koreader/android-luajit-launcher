package org.koreader.launcher

import android.app.SearchManager
import android.content.Intent
import android.os.Build

internal object IntentUtils {

    /**
     * get intent by action type, used to do dict lookups on 3rd party apps.
     *
     * @param text to search
     * @param pkg that receives the query - null to show the app picker
     * @param action associated to the package
     *
     * @return a Intent based on package/action ready to do a text lookup
     */

    fun getByAction(text: String, pkg: String?, action: String): Intent {
        var intent = Intent()
        when (action) {
            // generic actions used by a lot of apps
            "send" -> intent = Intent(getSendIntent(text, pkg))
            "search" -> intent = Intent(getSearchIntent(text, pkg))
            "text" -> intent = Intent(getTextIntent(text, pkg))
            // actions for specific apps
            "aard2" -> intent = Intent(getAard2Intent(text))
            "colordict" -> intent = Intent(getColordictIntent(text, pkg))
            "quickdic" -> intent = Intent(getQuickdicIntent(text))
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
        return if (intent == null) "" else "\naction: " + intent.action + "\ndata: " + intent.dataString + "\n"
    }

    // Intent.ACTION_SEND
    private fun getSendIntent(text: String, pkg: String? = null): Intent {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        intent.type = "text/plain"
        if (pkg != null) intent.setPackage(pkg)
        return intent
    }

    // Intent.ACTION_SEARCH
    private fun getSearchIntent(text: String, pkg: String? = null): Intent {
        val intent = Intent(Intent.ACTION_SEARCH)
        intent.putExtra(SearchManager.QUERY, text)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        if (pkg != null) intent.setPackage(pkg)
        return intent
    }

    // Intent.ACTION_PROCESS_TEXT (available on api23+)
    private fun getTextIntent(text: String, pkg: String? = null): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Intent.ACTION_PROCESS_TEXT)
            intent.putExtra(Intent.EXTRA_TEXT, text)
            intent.putExtra(Intent.EXTRA_PROCESS_TEXT, text)
            intent.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, text)
            intent.type = "text/plain"
            if (pkg != null) intent.setPackage(pkg)
            intent
        } else {
            // fallback to ACTION_SEND
            getSendIntent(text)
        }
    }

    /* Android custom intents for some dict apps ------ */
    private fun getAard2Intent(text: String): Intent {
        val intent = Intent("aard2.lookup")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(SearchManager.QUERY, text)
        return intent
    }

    private fun getColordictIntent(text: String, pkg: String?): Intent {
        val intent = Intent("colordict.intent.action.SEARCH")
        intent.putExtra("EXTRA_QUERY", text)
        intent.putExtra("EXTRA_FULLSCREEN", true)
        if (pkg != null) intent.setPackage(pkg)
        return intent
    }

    private fun getQuickdicIntent(text: String): Intent {
        val intent = Intent("com.hughes.action.ACTION_SEARCH_DICT")
        intent.putExtra(SearchManager.QUERY, text)
        return intent
    }
}
