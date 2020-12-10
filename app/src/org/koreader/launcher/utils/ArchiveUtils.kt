package org.koreader.launcher.utils

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream
import org.koreader.launcher.Logger
import java.io.*

object ArchiveUtils {
    const val TAG = "ArchiveUtils"

    fun untar(archive: String, extract_to: String): Boolean {
        return try {
            uncompress(archive, extract_to)
        } catch (e: IOException) {
            Logger.w(TAG, "exception uncompressing $archive: $e")
            false
        }
    }

    @Throws(IOException::class)
    private fun uncompress(archive: String, dest: String): Boolean {
        return getTarInput(archive)?.use {
            val output = File(dest)
            if (!output.exists()) {
                Logger.v(TAG, "Creating dir ${output.absolutePath}")
                output.mkdir()
            }
            var tarEntry = it.nextTarEntry
            while (tarEntry != null) {
                val destPath = File(dest, tarEntry.name)
                if (tarEntry.isDirectory) {
                    Logger.v(TAG, "Creating dir ${destPath.absolutePath}")
                    destPath.mkdirs()
                } else {
                    Logger.v(TAG, "Creating file ${destPath.absolutePath}")
                    destPath.createNewFile()
                    val buffer = ByteArray(4096)
                    val bufferOutput = BufferedOutputStream(FileOutputStream(destPath))
                    var len: Int
                    while (it.read(buffer).also { size -> len = size } != -1) {
                        Logger.v(TAG, "Copying $len bytes to ${destPath.absolutePath}")
                        bufferOutput.write(buffer, 0, len)
                    }
                    Logger.v(TAG, "File ${destPath.absolutePath} created OK")
                    bufferOutput.close()
                }
                tarEntry = it.nextTarEntry
            }
            it.close()
            true
        } ?:false
    }

    private fun getTarInput(archive: String): TarArchiveInputStream? {
        val ext = File(archive).extension
        return if ((ext == "gz") or (ext == "tgz") or (ext == "bz2") or (ext == "lz")) {
            Logger.i(TAG, "uncompressing $archive with compression $ext")
            // ignore illegal values for group/userid, mode, device numbers and timestamp
            TarArchiveInputStream(getCompressor(archive, ext) as InputStream, true)
        } else {
            Logger.w(TAG, "invalid extension for $archive. Aborting")
            null
        }
    }

    private fun getCompressor(archive: String, validExtension: String): Any {
        val input = File(archive)
        return when (validExtension) {
            "bz2" -> BZip2CompressorInputStream(BufferedInputStream(FileInputStream(input)))
            "lz" -> LZMACompressorInputStream(BufferedInputStream(FileInputStream(input)))
            else -> GzipCompressorInputStream(BufferedInputStream(FileInputStream(input)))
        }
    }
}
