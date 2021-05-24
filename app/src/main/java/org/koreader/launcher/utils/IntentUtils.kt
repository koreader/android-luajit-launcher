package org.koreader.launcher.utils

import android.app.SearchManager
import android.content.Intent
import android.os.Build

object IntentUtils {
    // Intent.ACTION_OPEN_DOCUMENT (available on api19+)
    val safIntent: Intent?
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "*/*"
                intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "application/epub+zip",
                    "application/fb2",
                    "application/fb3",
                    "application/msword",
                    "application/oxps",
                    "application/pdf",
                    "application/rtf",
                    "application/tcr",
                    "application/vnd.amazon.mobi8-ebook",
                    "application/vnd.comicbook+tar",
                    "application/vnd.comicbook+zip",
                    "application/vnd.ms-htmlhelp",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.palm",
                    "application/x-cbz",
                    "application/x-chm",
                    "application/x-fb2",
                    "application/x-fb3",
                    "application/x-mobipocket-ebook",
                    "application/x-tar",
                    "application/xhtml+xml",
                    "application/xml",
                    "application/zip",
                    "image/djvu",
                    "image/gif",
                    "image/jp2",
                    "image/jpeg",
                    "image/jxr",
                    "image/png",
                    "image/svg+xml",
                    "image/tiff",
                    "image/vnd.djvu",
                    "image/vnd.ms-photo",
                    "image/x-djvu",
                    "image/x-portable-arbitrarymap",
                    "image/x-portable-bitmap",
                    "text/html",
                    "text/plain"))
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                return intent
            } else {
                return null
            }
        }

    // Intent.ACTION_SEND
    fun getSendTextIntent(text: String, pkg: String? = null): Intent {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        intent.type = "text/plain"
        if (pkg != null) intent.setPackage(pkg)
        return intent
    }

    // Intent.ACTION_SEARCH
    fun getSearchTextIntent(text: String, pkg: String? = null): Intent {
        val intent = Intent(Intent.ACTION_SEARCH)
        intent.putExtra(SearchManager.QUERY, text)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        if (pkg != null) intent.setPackage(pkg)
        return intent
    }

    // Intent.ACTION_PROCESS_TEXT (available on api23+)
    fun getProcessTextIntent(text: String, pkg: String? = null): Intent {
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
            getSendTextIntent(text)
        }
    }

    // aard2.lookup
    fun getAard2Intent(text: String): Intent {
        val intent = Intent("aard2.lookup")
        intent.putExtra(SearchManager.QUERY, text)
        return intent
    }

    // colordict.intent.action.SEARCH
    fun getColordictIntent(text: String, pkg: String?): Intent {
        val intent = Intent("colordict.intent.action.SEARCH")
        intent.putExtra("EXTRA_QUERY", text)
        intent.putExtra("EXTRA_FULLSCREEN", true)
        if (pkg != null) intent.setPackage(pkg)
        return intent
    }

    // com.hughes.action.ACTION_SEARCH_DICT
    fun getQuickdicIntent(text: String): Intent {
        val intent = Intent("com.hughes.action.ACTION_SEARCH_DICT")
        intent.putExtra(SearchManager.QUERY, text)
        return intent
    }
}
