package org.koreader.launcher.extensions

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.system.Os
import org.koreader.launcher.MainApp
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

fun Uri.absolutePath(context: Context): String? {
    return when (this.scheme) {
        ContentResolver.SCHEME_FILE -> pathFromFile(this)
        ContentResolver.SCHEME_CONTENT -> pathFromContent(this, context)
        else -> null
    }
}

fun Uri.toFile(context: Context, path: String): String? {
    if (this.scheme != ContentResolver.SCHEME_CONTENT) {
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
    return uri.path?.let { filePath -> File(filePath) }?.absolutePath
}

private fun pathFromImportedFile(uri: Uri, context: Context): String? {
    return uri.toFile(context, MainApp.app_storage_path)
}
private fun pathFromContent(uri: Uri, context: Context): String? {
    val path = uri.authority?.let { _ ->
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

    return path?.let { filePath ->
        if (filePath.contains("/Android/data"))
            pathFromImportedFile(uri, context)
        else
            filePath
    }?: pathFromImportedFile(uri, context)
}
