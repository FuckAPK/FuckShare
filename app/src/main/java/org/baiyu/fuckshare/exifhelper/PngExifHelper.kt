package org.baiyu.fuckshare.exifhelper

import org.baiyu.fuckshare.Utils
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class PngExifHelper : ExifHelper {
    @Throws(ImageFormatException::class, IOException::class)
    override fun removeMetadata(inputStream: InputStream, outputStream: OutputStream?) {
        try {
            val bis = inputStream as? BufferedInputStream ?: BufferedInputStream(inputStream)
            val bos = outputStream as? BufferedOutputStream ?: BufferedOutputStream(outputStream)

            val buffer = ByteArray(8)
            bis.read(buffer, 0, 8)
            bos.write(buffer)
//            Utils.copy(bis, bos, 8)

            val chunkLengthBytes = ByteArray(4)
            val chunkNameBytes = ByteArray(4)
            var chunkDataCRCLength: Long
            while (bis.available() > 0) {
                Utils.inputStreamRead(bis, chunkLengthBytes)
                // 4 bytes of crc
                chunkDataCRCLength = Utils.bigEndianBytesToLong(chunkLengthBytes) + 4
                Utils.inputStreamRead(bis, chunkNameBytes)
                val chunkName = chunkNameBytes.toString(Charsets.US_ASCII)
                if (pngCriticalChunks.contains(chunkName)) {
                    bos.write(chunkLengthBytes)
                    bos.write(chunkNameBytes)
                    Timber.d("Copy chunk: %s size: %d", chunkName, chunkDataCRCLength + 4)
                    chunkDataCRCLength -= Utils.copy(bis, bos, chunkDataCRCLength)
                } else {
                    // skip chunkData and chunkCrc
                    Timber.d("Discord chunk: %s size: %d", chunkName, chunkDataCRCLength + 4)
                    chunkDataCRCLength -= Utils.inputStreamSkip(bis, chunkDataCRCLength)
                }
                assert(chunkDataCRCLength == 0L)
                if (chunkName == "IEND") {
                    break
                }
            }
            bos.flush()
        } catch (error: AssertionError) {
            throw ImageFormatException()
        }
    }

    companion object {
        private val pngCriticalChunks = setOf(
            "acTL",  //   animation control
            "bKGD",  //   background color
            "cHRM",  //   Primary Chromaticities
            "gAMA",  //   Gamma
            "gIFg",  //   GIFGraphicControlExtension
            "gIFt",  //   GIFPlainTextExtension
            "gIFx",  //   GIFApplicationExtension
            "fcTL",  // * frame control
            "fdAT",  // * frame data
            "hIST",  //   PaletteHistogram
            "IDAT",  // * image data
            "IEND",  // * trailer
            "IHDR",  // * header
            "pCAL",  //   Pixel Calibration
            "pHYs",  //   PhysicalPixel
            "PLTE",  // * palette
            "sBIT",  //   SignificantBits
            "sCAL",  //   SubjectScale
            "sRGB",  //   SRGBRendering
            "sTER",  //   StereoImage
            "tRNS",  //   Transparency
            "vpAg" //   VirtualPage
        )
    }
}