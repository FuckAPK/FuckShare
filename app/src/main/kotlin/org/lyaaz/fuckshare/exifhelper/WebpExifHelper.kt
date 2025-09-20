package org.lyaaz.fuckshare.exifhelper

import org.lyaaz.fuckshare.utils.ByteUtils
import org.lyaaz.fuckshare.utils.ByteUtils.toUInt
import org.lyaaz.fuckshare.utils.FileUtils
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WebpExifHelper : ExifHelper {
    @Throws(IOException::class, ImageFormatException::class)
    override fun removeMetadata(inputStream: InputStream, outputStream: OutputStream) {
        val bis = inputStream.buffered(BUFFER_SIZE)
        val bos = outputStream.buffered(BUFFER_SIZE)

        val chunkNameBytes = ByteArray(4)
        val chunkDataLenBytes = ByteArray(4)

        // copy header
        FileUtils.copy(bis, bos, 12)
        while (bis.available() > 0) {
            ByteUtils.readNBytes(bis, chunkNameBytes)
            ByteUtils.readNBytes(bis, chunkDataLenBytes)

            val chunkName = chunkNameBytes.toString(Charsets.US_ASCII)
            var realChunkDataLength = chunkDataLenBytes.toUInt(ByteOrder.LITTLE_ENDIAN).toLong()
            // standard of tiff: fill in end with 0x00 if chunk size if odd
            realChunkDataLength += realChunkDataLength % 2

            if (webpSkippableChunks.contains(chunkName)) {
                realChunkDataLength -= ByteUtils.skipNBytes(bis, realChunkDataLength)
                Timber.d("Discard chunk: $chunkName size: $realChunkDataLength")
            } else {
                bos.write(chunkNameBytes)
                bos.write(chunkDataLenBytes)
                Timber.d("Copy chunk: $chunkName size: $realChunkDataLength")
                realChunkDataLength -= FileUtils.copy(bis, bos, realChunkDataLength)
            }
            if (realChunkDataLength != 0L) {
                throw ImageFormatException()
            }
        }
        bos.flush()
    }

    override fun postProcess(file: File) {
        runCatching {
            RandomAccessFile(file, "rw").use { f ->
                val buffer = ByteBuffer.allocate(4).apply {
                    order(ByteOrder.LITTLE_ENDIAN)
                    putInt((file.length() - 8).toInt())
                }
                f.seek(4)
                f.write(buffer.array())
            }
            Timber.d("Fixed webp header size: $file")
        }.onFailure {
            Timber.e(it, "Failed to fix webp header for file: $file")
        }
    }

    companion object {
        private const val BUFFER_SIZE = 16 * 1024 // 16KB buffer
        
        private val webpSkippableChunks = setOf(
            "EXIF",
            "XMP "
        )
    }
}
