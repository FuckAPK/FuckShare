package org.baiyu.fuckshare.exifhelper

import org.baiyu.fuckshare.Utils
import org.baiyu.fuckshare.Utils.toUInt
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WebpExifHelper : ExifHelper {
    @Throws(IOException::class, ImageFormatException::class)
    override fun removeMetadata(inputStream: InputStream, outputStream: OutputStream) {
        val bis = inputStream.buffered()
        val bos = outputStream.buffered()

        val chunkNameBytes = ByteArray(4)
        val chunkDataLenBytes = ByteArray(4)
        var realChunkDataLength: Long

        // copy header
        Utils.copy(bis, bos, 12)
        while (bis.available() > 0) {
            Utils.readNBytes(bis, chunkNameBytes)
            Utils.readNBytes(bis, chunkDataLenBytes)

            val chunkName = chunkNameBytes.toString(Charsets.US_ASCII)
            realChunkDataLength = chunkDataLenBytes.toUInt(ByteOrder.LITTLE_ENDIAN).toLong()
            // standard of tiff: fill in end with 0x00 if chunk size if odd
            realChunkDataLength += realChunkDataLength % 2

            if (webpSkippableChunks.contains(chunkName)) {
                realChunkDataLength -= Utils.skipNBytes(bis, realChunkDataLength)
                Timber.d("Discord chunk: %s size: %d", chunkName, realChunkDataLength)
            } else {
                bos.write(chunkNameBytes)
                bos.write(chunkDataLenBytes)
                Timber.d("Copy chunk: %s size: %d", chunkName, realChunkDataLength)
                realChunkDataLength -= Utils.copy(bis, bos, realChunkDataLength)
            }
            if (realChunkDataLength != 0L) {
                throw ImageFormatException()
            }
        }
        bos.flush()
    }

    fun fixHeaderSize(file: RandomAccessFile) {
        ByteBuffer.allocate(4).let {
            it.order(ByteOrder.LITTLE_ENDIAN)
            it.putShort((file.length() - 8).toShort())
            file.seek(4)
            file.write(it.array())
        }
    }

    companion object {
        private val webpSkippableChunks = setOf(
            "EXIF",
            "XMP "
        )
    }
}