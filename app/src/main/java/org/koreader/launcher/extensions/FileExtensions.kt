package org.koreader.launcher.extensions

import android.annotation.SuppressLint
import android.os.Build
import android.system.Os
import java.io.*

@SuppressLint("DiscouragedPrivateApi")
fun File.symlink(link: String): Boolean {
    if (!this.exists()) return false
    try {
        File(link).delete()
    } catch (e: IOException) {
        e.printStackTrace()
    }

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Os.symlink(this.absolutePath, link)
            return true
        }
        val libcore = Class.forName("libcore.io.Libcore")
        val field = libcore.getDeclaredField("os")
        field.isAccessible = true
        val os = field.get(null)
        os.javaClass.getMethod("symlink", String::class.java,
            String::class.java).invoke(os, this.absolutePath, link)
        return true
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}

private const val FILE_READ_INT_DEFAULT = 0

/**
 * Reads from [this] and parses the result to [Int].
 *
 * In case of an error, [block] is invoked an returned.
 */
fun File.readOrElse(block: () -> Int = { FILE_READ_INT_DEFAULT }): Int {
    return read() ?: return block()
}

private fun File.read(): Int? {
    return try {
        this.readText().replace("\n", "").toInt()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Writes [value] to [this].
 *
 * @return true if the write op was successful, otherwise false.
 */
fun File.write(value: Int): Boolean {
    try {
        writeText(value.toString())
        return true
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}
