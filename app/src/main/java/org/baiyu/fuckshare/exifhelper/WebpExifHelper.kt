package org.baiyu.fuckshare.exifhelper

import org.baiyu.fuckshare.Utils
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteOrder

class WebpExifHelper : ExifHelper {
    @Throws(IOException::class, ImageFormatException::class)
    override fun removeMetadata(inputStream: InputStream, outputStream: OutputStream?) {
        try {
            val bis = inputStream as? BufferedInputStream ?: BufferedInputStream(inputStream)
            val bos = outputStream as? BufferedOutputStream ?: BufferedOutputStream(outputStream)

            val webpHeader = ByteArray(12)
            val chunkNameBytes = ByteArray(4)
            val chunkDataLenBytes = ByteArray(4)
            var realChunkDataLength: Long

            // calculate size
            // file size doesn't contain first 8 bytes
            var newSize = (bis.available() - 8).toLong()
            bis.mark(bis.available())
            Utils.inputStreamSkip(bis, 12)
            while (bis.available() > 0) {
                Utils.inputStreamRead(bis, chunkNameBytes)
                val chunkName = chunkNameBytes.toString()
                Utils.inputStreamRead(bis, chunkDataLenBytes)
                realChunkDataLength = Utils.littleEndianBytesToLong(chunkDataLenBytes)
                realChunkDataLength += realChunkDataLength % 2
                if (webpSkippableChunks.contains(chunkName)) {
                    newSize -= realChunkDataLength + 8
                }
                realChunkDataLength -= Utils.inputStreamSkip(bis, realChunkDataLength)
                assert(realChunkDataLength == 0L)
            }
            bis.reset()

            // rewrite with new size
            Utils.inputStreamRead(bis, webpHeader)
            bos.write(webpHeader, 0, 4)
            bos.write(Utils.longToBytes(newSize, ByteOrder.LITTLE_ENDIAN), 0, 4)
            bos.write(webpHeader, 8, 4)
            while (bis.available() > 0) {
                Utils.inputStreamRead(bis, chunkNameBytes)
                val chunkName = chunkNameBytes.toString(Charsets.US_ASCII)
                Utils.inputStreamRead(bis, chunkDataLenBytes)
                realChunkDataLength = Utils.littleEndianBytesToLong(chunkDataLenBytes)
                // standard of tiff: fill in end with 0x00 if chunk size if odd
                realChunkDataLength += realChunkDataLength % 2
                if (webpSkippableChunks.contains(chunkName)) {
                    realChunkDataLength -= Utils.inputStreamSkip(bis, realChunkDataLength)
                    Timber.d("Discord chunk: %s size: %d", chunkName, realChunkDataLength)
                } else {
                    bos.write(chunkNameBytes)
                    bos.write(chunkDataLenBytes)
                    Timber.d("Copy chunk: %s size: %d", chunkName, realChunkDataLength)
                    realChunkDataLength -= Utils.copy(bis, bos, realChunkDataLength)
                }
                assert(realChunkDataLength == 0L)
            }
            bos.flush()
        } catch (error: AssertionError) {
            throw ImageFormatException()
        }
    }

    companion object {
        private val webpSkippableChunks = setOf(
            "EXIF",
            "XMP "
        )
    }
}