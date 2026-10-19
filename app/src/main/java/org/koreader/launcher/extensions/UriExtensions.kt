package org.koreader.launcher.extensions

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.system.Os
import android.util.Log
import org.koreader.launcher.MainApp
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

private const val TAG = "UriHandler"
fun Uri.absolutePath(context: Context): String? {
    return when (this.scheme) {
        ContentResolver.SCHEME_FILE -> pathFromFile(this)
        ContentResolver.SCHEME_CONTENT -> pathFromContent(this, context)
        else -> null
    }
}

fun Uri.toFile(context: Context, path: String): String? {
    if (this.scheme != ContentResolver.SCHEME_CONTENT) {
        Log.e(TAG, "unsupported scheme")
        return null
    }

    this.authority ?: return null
    val nameColumn = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
    val contentResolver = context.contentResolver
    val name: String? = contentResolver.query(this, nameColumn,
        null, null, null)?.use {
        it.moveToFirst()
        it.getString(it.getColumnIndex(nameColumn[0]))
    }

    name ?: return null

    return context.contentResolver.openInputStream(this)?.use {
        try {
            val file = File(path, name)
            FileOutputStream(file).use { target ->
                val buffer = ByteArray(8 * 1024)
                var len = it.read(buffer)
                while (len != -1) {
                    target.write(buffer, 0, len)
                    len = it.read(buffer)
                }
            }
            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}

private fun pathFromFile(uri: Uri): String? {
    return uri.path?.let {
        Log.i(TAG, "Found local file-> $it")
        File(it).absolutePath
    }
}

private fun pathFromImportedFile(uri: Uri, context: Context, reason: String): String? {
    val path = File(MainApp.app_storage_path, "imported")
    return try {
        if (!path.isDirectory && !path.mkdirs()) {
            Log.e(TAG, "Cannot create import directory: $path")
            return null
        }
        val result = uri.toFile(context, path.absolutePath)
        Log.i(TAG, "[$reason]: imported to $result")
        result
    } catch (e: SecurityException) {
        Log.e(TAG, "Cannot access import directory: $path", e)
        null
    }
}

private fun pathFromContent(uri: Uri, context: Context): String? {
    val path = uri.authority?.let { _ ->
        Log.i(TAG,"Found content, trying to guess its path")
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { parcel ->
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
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    return path?.let { p ->
        // A URI grant may allow reading the descriptor but not its resolved path.
        // Import through the content resolver when that path cannot be reopened.
        val file = File(p)
        val isPrivatePath = p.startsWith("/data") || p.contains("/Android/data")
        val isReadableFile = file.isFile && file.canRead()
        if (isPrivatePath || !isReadableFile) {
            pathFromImportedFile(uri, context, "Non readable file")
        } else {
            Log.i(TAG, "Guessed path -> $p")
            p
        }
    }?: run {
        pathFromImportedFile(uri, context, "Not a local file")
    }
}
