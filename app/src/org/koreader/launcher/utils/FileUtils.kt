package org.koreader.launcher.utils

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.IOException
import java.util.Locale

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.system.Os

import org.koreader.launcher.MainApp

object FileUtils {
    const val TAG = "FileUtils"

    /**
     * save data as a file from a content uri
     *
     * Content providers can dispatch local or remote files.
     * When importing from a local content provider you're making a copy of the file!
     *
     * @param uri with content scheme
     * @param path where the new file will be stored
     * @return 1 if the file(s) were imported, 0 otherwise (IOError, invalid path, invalid uri..)
     */

    fun saveAsFile(context: Context, uri: Uri?, path: String?): Int {
        return uri?.let { getFileFromContentUri(context, it, path)?.let { 1 } ?: 0 } ?: 0
    }

    /**
     * gets the absolute path of a file from an uri.
     *
     * @param uri with file scheme.
     * @return a string containing the full path of the file.
     */

    fun getPathFromUri(context: Context, uri: Uri?): String? {
        return uri?.let { contentUri ->
            when {
                ContentResolver.SCHEME_FILE == contentUri.scheme -> {
                    contentUri.path?.let { filePath -> File(filePath) }?.absolutePath
                }
                ContentResolver.SCHEME_CONTENT == contentUri.scheme -> {
                    uri.authority?.let { domain ->
                        Logger.v(TAG, "trying to resolve a file path from content delivered by $domain")
                    }
                    val fd = context.contentResolver.openFileDescriptor(contentUri, "r")
                    getFileDescriptorPath(fd)
                }
                else -> null
            }
        }
    }

    /**
     * tries to get the absolute path of a file from a content provider. It works with most
     * applications that use a fileProvider to deliver files to other applications.
     * If the data in the uri is not a file this will fail
     *
     * @param pfd - parcelable file descriptor from contentResolver.openFileDescriptor
     * @return absolute path to file or null
     */

    private fun getFileDescriptorPath(pfd: ParcelFileDescriptor?): String? {
        return pfd?.use { parcel ->
            try {
                val file = File("/proc/self/fd/" + parcel.fd)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Os.readlink(file.absolutePath)
                } else {
                    file.canonicalPath
                }
            } catch (e: IOException) {
                null
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * gets a file from content:// uris
     * @param context of the activity
     * @param uri with scheme content
     * @param path to store the imported copy, if null the root folder of the storage path will be used.
     * @return a file
     */

    private fun getFileFromContentUri(context: Context, uri: Uri, path: String?): File? {

        /* It is impossible to obtain a File from content schemes.

         The workflow is:
         1. Obtain the name of the file we're going to retrieve
         2. Create a new file on the specified path
         3. Store the content stream on the new file
         4. return the file, which is a hard copy of the one streamed as content. */

        var file: File? = null
        val importPath: String = path ?: MainApp.storage_path
        val nameColumn = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(uri, nameColumn, null, null, null)
        if (cursor != null) {
            cursor.moveToFirst()
            val name = cursor.getString(cursor.getColumnIndex(nameColumn[0]))
            cursor.close()

            var stream: InputStream? = null
            if (uri.authority != null) {
                try {
                    stream = context.contentResolver.openInputStream(uri)
                    file = getFileFromInputStream(stream, importPath, name)
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
        }
        return file
    }

    /**
     * Get a file from InputStream
     * @param stream from contentResolver.openInputStream(uri)
     * @param path of the file
     * @param name of the file
     * @return nullable file
     */

    @Throws(IOException::class)
    private fun getFileFromInputStream(stream: InputStream?, path: String, name: String): File? {
        return stream?.let {
            try {
                val file = File(path, name)
                Logger.i(TAG, String.format(Locale.US,
                    "saving document: %s in %s", name, path))
                FileOutputStream(file).use { target ->
                    val buffer = ByteArray(8 * 1024)
                    var len = it.read(buffer)
                    while (len != -1) {
                        target.write(buffer, 0, len)
                        len = it.read(buffer)
                    }
                    it.close()
                }
                Logger.v(TAG, file.absolutePath + " created without errors")
                file
            } catch (e: IOException) {
                Logger.e(TAG, "I/O error: $e")
                null
            }
        }
    }
}
