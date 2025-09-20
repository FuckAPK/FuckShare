package org.lyaaz.fuckshare.exifhelper

import org.lyaaz.fuckshare.utils.ByteUtils
import org.lyaaz.fuckshare.utils.ByteUtils.toUShort
import org.lyaaz.fuckshare.utils.FileUtils
import org.lyaaz.fuckshare.exifhelper.ExifHelper.Companion.JPEG_MARKER_SIZE
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteOrder

class JpegExifHelper : ExifHelper {
    @Throws(IOException::class, ImageFormatException::class)
    override fun removeMetadata(inputStream: InputStream, outputStream: OutputStream) {
        val bis = inputStream.buffered()
        val bos = outputStream.buffered()

        val maker = ByteArray(JPEG_MARKER_SIZE)
        val lenBytes = ByteArray(JPEG_MARKER_SIZE)
        var chunkDataLength = 0L

        while (bis.available() > 0) {

            ByteUtils.readNBytes(bis, maker)

            if (maker[0] != 0xFF.toByte()) {
                throw ImageFormatException()
            }

            val makerName = maker[1].toUByte().toString(16).uppercase()
            when (maker[1]) {
                0xD8.toByte() -> {
                    bos.write(maker)
                    Timber.d("Copy chunk: $makerName size: $JPEG_MARKER_SIZE")

                }

                0xD9.toByte() -> {   // EOI
                    bos.write(maker)
                    Timber.d("Copy chunk: $makerName size: $JPEG_MARKER_SIZE")
                    break
                }

                0xDA.toByte() -> {   // SOS
                    bos.write(maker)
                    // write all data
                    chunkDataLength = bis.available().toLong()
                    Timber.d("Copy DA and following chunks size: ${chunkDataLength + JPEG_MARKER_SIZE}")
                    chunkDataLength -= bis.copyTo(bos)
                }

                in jpegSkippableChunks -> {
                    ByteUtils.readNBytes(bis, lenBytes)
                    chunkDataLength = lenBytes.toUShort(ByteOrder.BIG_ENDIAN).toLong() - JPEG_MARKER_SIZE
                    Timber.d("Discard chunk: $makerName size: ${chunkDataLength + JPEG_MARKER_SIZE * 2}")
                    chunkDataLength -= ByteUtils.skipNBytes(bis, chunkDataLength)
                }

                else -> {
                    ByteUtils.readNBytes(bis, lenBytes)
                    chunkDataLength = lenBytes.toUShort(ByteOrder.BIG_ENDIAN).toLong() - JPEG_MARKER_SIZE
                    bos.write(maker)
                    bos.write(lenBytes)
                    Timber.d("Copy chunk: $makerName size: ${chunkDataLength + JPEG_MARKER_SIZE * 2}")
                    chunkDataLength -= FileUtils.copy(bis, bos, chunkDataLength)
                }
            }

            if (chunkDataLength != 0L) {
                throw ImageFormatException()
            }
        }
        bos.flush()
    }

    companion object {
        private val jpegSkippableChunks = setOf(
            0xE0.toByte(),
            0xE1.toByte(),
            0xE2.toByte(),
            0xE3.toByte(),
            0xE4.toByte(),
            0xE5.toByte(),
            0xE6.toByte(),
            0xE7.toByte(),
            0xE8.toByte(),
            0xE9.toByte(),
            0xEA.toByte(),
            0xEB.toByte(),
            0xEC.toByte(),
            0xED.toByte(),
            0xEE.toByte(),
            0xEF.toByte(),
            0xFE.toByte()
        )
    }
}