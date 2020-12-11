package org.koreader.launcher.utils

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.lzma.LZMACompressorInputStream
import org.koreader.launcher.Logger
import java.io.*

object ArchiveUtils {
    const val TAG = "ArchiveUtils"

    fun untar(archive: String, extract_to: String, deleteIfOk: Boolean = false): Boolean {
        val success = try {
            uncompress(archive, extract_to)
        } catch (e: IOException) {
            Logger.w(TAG, "exception uncompressing $archive: $e")
            e.printStackTrace()
            false
        }

        if (success and deleteIfOk) {
            try {
                File(archive).delete()
            } catch (e: IOException) {
                Logger.w(TAG, "exception deleting $archive: $e")
                e.printStackTrace()
            }
        }
        return success
    }

    @Throws(IOException::class)
    private fun uncompress(archive: String, dest: String): Boolean {
        return getTarInput(archive)?.use {
            val output = File(dest)
            if (!output.exists()) {
                Logger.i(TAG, "Creating destination dir: ${output.absolutePath}")
                output.mkdir()
            }
            var tarEntry = it.nextTarEntry
            while (tarEntry != null) {
                val destPath = File(dest, tarEntry.name)
                destPath.parentFile?.let { parent ->
                    if (!parent.exists()) {
                        parent.mkdirs()
                    }
                }
                var isOverride = false
                if (!tarEntry.isDirectory) {
                    if (destPath.exists()) {
                        isOverride = true
                        destPath.delete()
                    }
                    if (destPath.createNewFile()) {
                        val buffer = ByteArray(4096)
                        val bufferOutput = BufferedOutputStream(FileOutputStream(destPath))
                        var len: Int
                        while (it.read(buffer).also { size -> len = size } != -1) {
                            bufferOutput.write(buffer, 0, len)
                        }

                        val mode = if (isOverride) "overwritten" else "created"
                        Logger.v(TAG, "File ${destPath.absolutePath} $mode OK")
                        bufferOutput.close()
                    } else {
                        Logger.w(TAG, "Failed creating file ${destPath.absolutePath}")
                    }
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
