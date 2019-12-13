package org.koreader.launcher

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException
import java.util.Locale

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

internal object FileUtils {
    const val TAG = "FileUtils"

    /**
     * gets the absolute path of a document from an uri
     * @param context of the activity
     * @param uri with file/content schemes. Others return null.
     * @return a string containing the full path of the document.
     */

    fun getAbsolutePath(context: Context, uri: Uri?): String? {
        uri ?: return null
        return getFileFromUri(context, uri)?.absolutePath
    }

    /**
     * gets the absolute path of a file from an uri
     * @param context of the activity
     * @param uri with file scheme.
     * @return a string containing the full path of the file.
     */

    fun getAbsoluteFilePath(context: Context, uri: Uri?): String? {
        uri ?: return null
        return if (ContentResolver.SCHEME_FILE == uri.scheme) {
            val file = File(uri.path)
            return file.absolutePath
        } else null
    }

    /**
     * gets a file from an uri.
     * @param context of the activity
     * @param uri with scheme file or content. Invalid schemes will return null
     * @return a file containing the document we want to open.
     */

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        var file: File? = null
        val scheme = uri.scheme
        if (ContentResolver.SCHEME_FILE == scheme) {
            file = File(uri.path)
        } else if (ContentResolver.SCHEME_CONTENT == scheme) {
            file = getFileFromContentUri(context, uri)
        }
        return file
    }

    /**
     * gets a file from content:// uris
     * @param context of the activity
     * @param uri with scheme content
     * @return a file
     */

    private fun getFileFromContentUri(context: Context, uri: Uri): File? {
        var file: File? = null

        /* It is impossible to obtain a File from content schemes.

         The workflow is:
         1. Obtain the name of the file we're going to retrieve
         2. Create a new file on the cache
         3. Store the content stream on the new file
         4. return the file, which is a hard copy of the one streamed as content. */

        val nameColumn = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(uri, nameColumn, null, null, null)
        if (cursor != null) {
            cursor.moveToFirst()
            val name = cursor.getString(cursor.getColumnIndex(nameColumn[0]))
            cursor.close()
            Logger.i(TAG, String.format(Locale.US, "Importing content: %s", name))
            val path = getPathFromCache(context, uri, name)
            file = File(path)
        }
        return file
    }

    /**
     * gets the absolute path of a cache file
     * @param context of the activity
     * @param uri with scheme content
     * @param name of the file
     * @return a string containing the full path of the document.
     */

    private fun getPathFromCache(context: Context, uri: Uri, name: String): String? {
        var path: String? = null
        var stream: InputStream? = null
        if (uri.authority != null) {
            try {
                stream = context.contentResolver.openInputStream(uri)
                val file = getCacheFile(context, stream, name)
                path = file!!.path
            } catch (e: IOException) {
                Logger.e(TAG, "I/O error: $e")
            } finally {
                try {
                    stream?.close()
                } catch (e: IOException) {
                    Logger.e(TAG, "I/O error: $e")
                }
            }
        }
        return path
    }

    /**
     * gets a cache file from an inputstream buffer
     * @param context of the activity
     * @param stream from contentResolver.openInputStream(uri)
     * @param name of the file
     * @return cache file
     */

    @Throws(IOException::class)
    private fun getCacheFile(context: Context, stream: InputStream?, name: String): File? {
        return if (stream != null) {
            try {
                val file = File(context.cacheDir, name)
                FileOutputStream(file).use { target ->
                    val buffer = ByteArray(8 * 1024)
                    var len = stream.read(buffer)
                    while (len != -1) {
                        target.write(buffer, 0, len)
                        len = stream.read(buffer)
                    }
                    stream.close()
                }
                file
            } catch (e: IOException) {
                Logger.e(TAG, "I/O error: $e")
                null
            }
        } else null
    }
}
