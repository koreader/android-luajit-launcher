package org.koreader.launcher.extensions

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream
import java.io.*

fun File.uncompress(extract_to: String, deleteIfOk: Boolean = false): Boolean {
    val success = try {
        uncompress(this.absolutePath, extract_to)
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
private fun uncompress(archive: String, extract_to: String): Boolean {
    return getTarInput(archive)?.use {
        val output = File(extract_to)
        if (!output.exists()) {
            output.mkdir()
        }
        var tarEntry = it.nextTarEntry
        while (tarEntry != null) {
            val destPath = File(extract_to, tarEntry.name)
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
                    val buffer = ByteArray(4096)
                    val bufferOutput = BufferedOutputStream(FileOutputStream(destPath))
                    var len: Int
                    while (it.read(buffer).also { size -> len = size } != -1) {
                        bufferOutput.write(buffer, 0, len)
                    }
                    bufferOutput.close()
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
