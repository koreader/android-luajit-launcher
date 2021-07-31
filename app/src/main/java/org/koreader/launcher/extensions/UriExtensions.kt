package org.koreader.launcher.extensions

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.system.Os
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

fun Uri.absolutePath(context: Context): String? {
    return when (this.scheme) {
        ContentResolver.SCHEME_FILE -> {
            this.path?.let { filePath -> File(filePath) }?.absolutePath
        }
        ContentResolver.SCHEME_CONTENT -> {
            this.authority?.let { _ ->
                try {
                    context.contentResolver.openFileDescriptor(this, "r")?.use { parcel ->
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
        }
        else -> null
    }
}

fun Uri.toFile(context: Context, path: String) {
    if (this.scheme != ContentResolver.SCHEME_CONTENT) {
        return
    }

    this.authority ?: return
    val nameColumn = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
    val contentResolver = context.contentResolver
    val name: String? = contentResolver.query(this, nameColumn,
        null, null, null)?.use {
        it.moveToFirst()
        it.getString(it.getColumnIndex(nameColumn[0]))
    }

    name ?: return

    context.contentResolver.openInputStream(this)?.use {
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
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
