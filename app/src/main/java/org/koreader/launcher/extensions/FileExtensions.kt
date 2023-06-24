package org.koreader.launcher.extensions

import android.annotation.SuppressLint
import android.os.Build
import android.system.Os
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream
import org.apache.commons.compress.utils.IOUtils
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

fun File.read(): Int {
    return try {
        this.readText().replace("\n", "").toInt()
    } catch (e: Exception) {
        e.printStackTrace()
        0
    }
}

fun File.write(value: Int) {
    try {
        writeText(value.toString())
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun File.uncompress(extractTo: String, deleteIfOk: Boolean = false): Boolean {
    val success = try {
        uncompress(this.absolutePath, extractTo)
    } catch (e: IOException) {
        e.printStackTrace()
        false
    }

    if (success and deleteIfOk) {
        try {
            delete()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    return success
}

@Throws(IOException::class)
private fun uncompress(archive: String, extractTo: String): Boolean {
    return getTarInput(archive)?.use {
        val output = File(extractTo)
        if (!output.exists()) {
            output.mkdir()
        }
        var tarEntry = it.nextTarEntry
        while (tarEntry != null) {
            val destPath = File(extractTo, tarEntry.name)
            destPath.parentFile?.let { parent ->
                if (!parent.exists()) {
                    parent.mkdirs()
                }
            }
            if (!tarEntry.isDirectory) {
                if (destPath.exists()) {
                    destPath.delete()
                }
                if (destPath.createNewFile()) {
                    FileOutputStream(destPath)?.use { out -> IOUtils.copy(it, out, 4096) }
                }
            }
            tarEntry = it.nextTarEntry
        }
        it.close()
        true
    } ?:false
}

private fun getTarInput(archive: String): TarArchiveInputStream? {
    val input = File(archive)
    val validExtensions = arrayOf("bz2", "gz", "lz", "tgz")
    return if (validExtensions.contains(input.extension)) {
        TarArchiveInputStream(
            when (input.extension) {
                "bz2" -> BZip2CompressorInputStream(BufferedInputStream(FileInputStream(input)))
                "lz" -> LZMACompressorInputStream(BufferedInputStream(FileInputStream(input)))
                else -> GzipCompressorInputStream(BufferedInputStream(FileInputStream(input)))
            },
            // ignore illegal values for group/userid, mode, device numbers and timestamp
            true
        )
    } else {
        null
    }
}
