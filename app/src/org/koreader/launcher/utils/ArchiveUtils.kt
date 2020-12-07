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
        val ext = File(archive).extension
        Logger.i(TAG, "uncompressing $ext archiver")
        return try {
            uncompress(archive, extract_to, ext)
        } catch (e: IOException) {
            false
        }
    }

    @Throws(IOException::class)
    fun uncompress(archive: String, dest: String, extension: String): Boolean {
        val input = File(archive)
        val output = File(dest)
        output.mkdir()
        lateinit var tarInput: TarArchiveInputStream

        when {
            (extension == "gz") or (extension == "tgz") -> {
                tarInput = TarArchiveInputStream(
                    GzipCompressorInputStream(
                        BufferedInputStream(
                            FileInputStream(
                                input
                            )
                        )
                    )
                )
            }
            extension == "bz2" -> {
                tarInput = TarArchiveInputStream(
                    BZip2CompressorInputStream(
                        BufferedInputStream(
                            FileInputStream(
                                input
                            )
                        )
                    )
                )
            }
            extension == "lz" -> {
                tarInput = TarArchiveInputStream(
                    LZMACompressorInputStream(
                        BufferedInputStream(
                            FileInputStream(
                                input
                            )
                        )
                    )
                )
            }
            else -> return false
        }

        var tarEntry = tarInput.nextTarEntry
        while (tarEntry != null) {
            val destPath = File(dest, tarEntry.name)
            Logger.v(TAG, "working: " + destPath.canonicalPath)
            if (tarEntry.isDirectory) {
                destPath.mkdirs()
            } else {
                destPath.createNewFile()
                val buffer = ByteArray(4096)
                val bufferOutput = BufferedOutputStream(FileOutputStream(destPath))
                var len = 0
                while (tarInput.read(buffer).also { len = it } != -1) {
                    bufferOutput.write(buffer, 0, len)
                }
                bufferOutput.close()
            }
            tarEntry = tarInput.nextTarEntry
        }
        tarInput.close()
        return true
    }
}
