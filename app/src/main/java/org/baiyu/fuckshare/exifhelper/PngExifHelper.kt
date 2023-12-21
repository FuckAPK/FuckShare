package org.baiyu.fuckshare.exifhelper

import org.baiyu.fuckshare.utils.ByteUtils
import org.baiyu.fuckshare.utils.ByteUtils.toUInt
import org.baiyu.fuckshare.utils.FileUtils
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteOrder

class PngExifHelper : ExifHelper {
    @Throws(ImageFormatException::class, IOException::class)
    override fun removeMetadata(inputStream: InputStream, outputStream: OutputStream) {
        val bis = inputStream.buffered()
        val bos = outputStream.buffered()

        FileUtils.copy(bis, bos, 8)

        val chunkLengthBytes = ByteArray(4)
        val chunkNameBytes = ByteArray(4)
        var chunkDataCRCLength: Long

        while (bis.available() > 0) {
            ByteUtils.readNBytes(bis, chunkLengthBytes)
            ByteUtils.readNBytes(bis, chunkNameBytes)
            // 4 bytes of crc
            chunkDataCRCLength = chunkLengthBytes.toUInt(ByteOrder.BIG_ENDIAN).toLong() + 4
            val chunkName = chunkNameBytes.toString(Charsets.US_ASCII)
            chunkDataCRCLength -= if (pngCriticalChunks.contains(chunkName)) {
                bos.write(chunkLengthBytes)
                bos.write(chunkNameBytes)
                Timber.d("Copy chunk: $chunkName size: ${chunkDataCRCLength + 4}")
                FileUtils.copy(bis, bos, chunkDataCRCLength)
            } else {
                // skip chunkData and chunkCrc
                Timber.d("Discord chunk: $chunkName size: ${chunkDataCRCLength + 4}")
                ByteUtils.skipNBytes(bis, chunkDataCRCLength)
            }
            if (chunkDataCRCLength != 0L) {
                throw ImageFormatException()
            }
            if (chunkName == "IEND") {
                break
            }
        }
        bos.flush()
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