package org.baiyu.fuckshare.exifhelper

import android.os.FileUtils
import org.baiyu.fuckshare.Utils
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class JpegExifHelper : ExifHelper {
    @Throws(IOException::class, ImageFormatException::class)
    override fun removeMetadata(inputStream: InputStream, outputStream: OutputStream?) {
        try {
            val bis = inputStream as? BufferedInputStream ?: BufferedInputStream(inputStream)
            val bos = outputStream as? BufferedOutputStream ?: BufferedOutputStream(outputStream)

            val maker = ByteArray(2)
            val lenBytes = ByteArray(2)
            var chunkDataLength: Long = 0
            while (bis.available() > 0) {
                Utils.inputStreamRead(bis, maker)
                assert(maker[0] == 0xFF.toByte())
                when (maker[1]) {
                    0xD8.toByte() -> {
                        bos.write(maker)
                        Timber.d("Copy chunk: %02X size: %d", maker[1], 2)
                    }

                    0xD9.toByte() -> {   // EOI
                        bos.write(maker)
                        Timber.d("Copy chunk: %02X size: %d", maker[1], 2)
                        break
                    }

                    in jpegSkippableChunks -> {
                        Utils.inputStreamRead(bis, lenBytes)
                        chunkDataLength = Utils.bigEndianBytesToLong(lenBytes) - 2
                        Timber.d("Discord chunk: %02X size: %d", maker[1], chunkDataLength + 4)
                        chunkDataLength -= Utils.inputStreamSkip(bis, chunkDataLength)
                    }

                    0xDA.toByte() -> {   // SOS
                        bos.write(maker)
                        // write all data
                        chunkDataLength = bis.available().toLong()
                        Timber.d("Copy DA and following chunks size: %s", chunkDataLength + 2)
                        chunkDataLength -= FileUtils.copy(bis, bos)
                    }

                    else -> {
                        Utils.inputStreamRead(bis, lenBytes)
                        chunkDataLength = Utils.bigEndianBytesToLong(lenBytes) - 2
                        bos.write(maker)
                        bos.write(lenBytes)
                        Timber.d("Copy chunk: %02X size: %d", maker[1], chunkDataLength + 4)
                        chunkDataLength -= Utils.copy(bis, bos, chunkDataLength)
                    }
                }
                assert(chunkDataLength == 0L)
            }
            bos.flush()
        } catch (error: AssertionError) {
            throw ImageFormatException()
        }
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