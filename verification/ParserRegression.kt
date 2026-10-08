import java.io.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import org.lyaaz.fuckshare.exifhelper.*
import org.lyaaz.fuckshare.utils.ByteUtils
import org.lyaaz.fuckshare.utils.FileUtils

fun stream(bytes: ByteArray): InputStream = object : FilterInputStream(ByteArrayInputStream(bytes)) {
    override fun available() = 0
    override fun read(b: ByteArray, off: Int, len: Int) = super.read(b, off, minOf(len, 1))
}

fun clean(helper: ExifHelper, bytes: ByteArray): ByteArray {
    val out = ByteArrayOutputStream()
    helper.removeMetadata(stream(bytes), out)
    return out.toByteArray()
}

fun rejects(helper: ExifHelper, bytes: ByteArray) {
    check(runCatching { clean(helper, bytes) }.isFailure) { "Truncated or invalid image accepted" }
}

fun pngChunk(name: String, data: ByteArray): ByteArray {
    val type = name.toByteArray()
    val crc = CRC32().apply { update(type); update(data) }.value.toInt()
    return ByteBuffer.allocate(data.size + 12).putInt(data.size).put(type).put(data).putInt(crc).array()
}

fun riffChunk(name: String, data: ByteArray): ByteArray =
    name.toByteArray() + ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(data.size).array() +
        data + if (data.size % 2 == 1) byteArrayOf(0) else byteArrayOf()

fun main() {
    for ((format, helper) in listOf("jpeg" to JpegExifHelper(), "png" to PngExifHelper())) {
        val image = BufferedImage(5, 3, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until 3) for (x in 0 until 5) image.setRGB(x, y, (x * 51 shl 16) or (y * 80 shl 8))
        val original = ByteArrayOutputStream().also { check(ImageIO.write(image, format, it)) }.toByteArray()
        val processed = clean(helper, original)
        val before = ImageIO.read(ByteArrayInputStream(original))
        val after = ImageIO.read(ByteArrayInputStream(processed)) ?: error("Cannot decode processed $format")
        check(before.width == after.width && before.height == after.height)
        for (y in 0 until before.height) for (x in 0 until before.width) check(before.getRGB(x, y) == after.getRGB(x, y))
        rejects(helper, original.copyOf(5))
    }
    val pngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)
    val retained = pngChunk("acTL", byteArrayOf(0, 0, 0, 1, 0, 0, 0, 0)) + pngChunk("IEND", byteArrayOf())
    val metadata = pngChunk("tEXt", "author\u0000private".toByteArray())
    check(clean(PngExifHelper(), pngSignature + metadata + retained).contentEquals(pngSignature + retained))
    rejects(PngExifHelper(), pngSignature + retained.dropLast(1).toByteArray())
    rejects(PngExifHelper(), ByteArray(20))
    rejects(JpegExifHelper(), byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte(), 0xe1.toByte(), 0, 1))
    val content = riffChunk("VP8 ", byteArrayOf(1, 2, 3, 4)) + riffChunk("ANIM", byteArrayOf(5, 6))
    val privateChunks = riffChunk("EXIF", byteArrayOf(7, 8, 9)) + riffChunk("XMP ", byteArrayOf(10))
    val header = "RIFF".toByteArray() + ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        .putInt(4 + content.size + privateChunks.size).array() + "WEBP".toByteArray()
    val processed = clean(WebpExifHelper(), header + content + privateChunks)
    check(processed.copyOfRange(12, processed.size).contentEquals(content))
    val temp = File.createTempFile("webp-regression", ".webp")
    try {
        temp.writeBytes(processed)
        WebpExifHelper().postProcess(temp)
        check(ByteBuffer.wrap(temp.readBytes(), 4, 4).order(ByteOrder.LITTLE_ENDIAN).int == temp.length().toInt() - 8)
    } finally { temp.delete() }
    rejects(WebpExifHelper(), (header + content + privateChunks).dropLast(1).toByteArray())
    for (helper in listOf(JpegExifHelper(), PngExifHelper(), WebpExifHelper())) rejects(helper, byteArrayOf())
    val zeroReads = object : ByteArrayInputStream(byteArrayOf(1, 2, 3)) {
        override fun read(b: ByteArray, off: Int, len: Int) = 0
        override fun skip(n: Long) = 0L
    }
    val bytes = ByteArray(3)
    check(ByteUtils.readNBytes(zeroReads, bytes) == 3 && bytes.contentEquals(byteArrayOf(1, 2, 3)))
    check(ByteUtils.skipNBytes(object : ByteArrayInputStream(byteArrayOf(1, 2, 3)) {
        override fun read(b: ByteArray, off: Int, len: Int) = 0
        override fun skip(n: Long) = 0L
    }, 3) == 3L)
    val out = ByteArrayOutputStream()
    check(FileUtils.copy(object : ByteArrayInputStream(byteArrayOf(4, 5)) {
        override fun read(b: ByteArray, off: Int, len: Int) = 0
    }, out, 2) == 2L && out.toByteArray().contentEquals(byteArrayOf(4, 5)))
    println("Parser regressions: real JPEG/PNG pixels preserved; metadata and animation chunks; short/zero reads; truncated inputs; WebP size")
}
