package org.lyaaz.fuckshare.exifhelper

import org.lyaaz.fuckshare.utils.ByteUtils
import org.lyaaz.fuckshare.utils.ByteUtils.toUShort
import org.lyaaz.fuckshare.utils.FileUtils
import timber.log.Timber
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteOrder

class JpegExifHelper : ExifHelper {
    @Throws(IOException::class, ImageFormatException::class)
    override fun removeMetadata(inputStream: InputStream, outputStream: OutputStream) {
        val bis = inputStream.buffered()
        val bos = outputStream.buffered()

        val maker = ByteArray(2)
        val lenBytes = ByteArray(2)
        var chunkDataLength = 0L
        ByteUtils.readFully(bis, maker)
        if (!maker.contentEquals(byteArrayOf(0xFF.toByte(), 0xD8.toByte()))) {
            throw ImageFormatException()
        }
        bos.write(maker)

        while (true) {
            val prefix = bis.read()
            if (prefix == -1) throw EOFException("Truncated image field")
            if (prefix != 0xFF) {
                throw ImageFormatException()
            }

            var markerType = bis.read()
            while (markerType == 0xFF) {
                markerType = bis.read()
            }
            if (markerType == -1) throw EOFException("Truncated image field")
            if (markerType == 0x00) {
                throw ImageFormatException()
            }

            maker[0] = 0xFF.toByte()
            maker[1] = markerType.toByte()

            val makerName = maker[1].toUByte().toString(16).uppercase()
            when (maker[1]) {
                0xD8.toByte(),
                0x01.toByte(),
                in 0xD0.toByte()..0xD7.toByte() -> {
                    bos.write(maker)
                    Timber.d("Copy chunk: $makerName size: 2")
                }

                0xD9.toByte() -> {   // EOI
                    bos.write(maker)
                    Timber.d("Copy chunk: $makerName size: 2")
                    break
                }

                0xDA.toByte() -> {   // SOS
                    bos.write(maker)
                    // The entropy-coded payload may contain marker-like bytes. Copy it unchanged.
                    ByteUtils.readFully(bis, lenBytes)
                    chunkDataLength = lenBytes.toUShort(ByteOrder.BIG_ENDIAN).toLong() - 2
                    if (chunkDataLength < 0) throw ImageFormatException()
                    bos.write(lenBytes)
                    if (FileUtils.copy(bis, bos, chunkDataLength) != chunkDataLength) {
                        throw ImageFormatException()
                    }
                    bis.copyTo(bos)
                    break
                }

                in jpegSkippableChunks -> {
                    ByteUtils.readFully(bis, lenBytes)
                    chunkDataLength = lenBytes.toUShort(ByteOrder.BIG_ENDIAN).toLong() - 2
                    if (chunkDataLength < 0) throw ImageFormatException()
                    Timber.d("Discord chunk: $makerName size: ${chunkDataLength + 4}")
                    chunkDataLength -= ByteUtils.skipNBytes(bis, chunkDataLength)
                }

                else -> {
                    ByteUtils.readFully(bis, lenBytes)
                    chunkDataLength = lenBytes.toUShort(ByteOrder.BIG_ENDIAN).toLong() - 2
                    if (chunkDataLength < 0) throw ImageFormatException()
                    bos.write(maker)
                    bos.write(lenBytes)
                    Timber.d("Copy chunk: $makerName size: ${chunkDataLength + 4}")
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