package org.lyaaz.fuckshare.filetype

@Suppress("SpellCheckingInspection")
enum class ImageType(
    override val supportMetadata: Boolean,
    override val extension: String,
    override val signatures: Set<Map<Int, ByteArray>>
) : FileType {
    JPEG(
        true,
        "jpg",
        setOf(
            mapOf(0 to byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))
        )
    ),
    PNG(
        true, "png", setOf(
            mapOf(
                0 to
                        byteArrayOf(
                            0x89.toByte(),
                            0x50.toByte(),
                            0x4E.toByte(),
                            0x47.toByte(),
                            0x0D.toByte(),
                            0x0A.toByte(),
                            0x1A.toByte(),
                            0x0A.toByte()
                        )
            )
        )
    ),
    WEBP(
        true, "webp", setOf(
            mapOf(
                0 to "RIFF".toByteArray(Charsets.US_ASCII),
                8 to "WEBP".toByteArray(Charsets.US_ASCII)
            )
        )
    ),
    GIF(
        false, "gif", setOf(
            mapOf(
                0 to "GIF87a".toByteArray(Charsets.US_ASCII)
            ),
            mapOf(
                0 to "GIF89a".toByteArray(Charsets.US_ASCII)
            )
        )
    ),
    TIFF(
        false, "tiff", setOf(
            mapOf(
                0 to
                        byteArrayOf(0x49.toByte(), 0x49.toByte(), 0x2A.toByte(), 0x00.toByte())
            ),
            mapOf(
                0 to
                        byteArrayOf(0x4D.toByte(), 0x4D.toByte(), 0x00.toByte(), 0x2A.toByte())
            )
        )
    ),
    HEIC(
        false, "heic", setOf(
            mapOf(
                4 to "ftypmif1".toByteArray(Charsets.US_ASCII)
            )
        )
    ),
    JP2(
        false, "jp2", setOf(
            mapOf(
                0 to
                        byteArrayOf(
                            0x00.toByte(),
                            0x00.toByte(),
                            0x00.toByte(),
                            0x0C.toByte(),
                            0x6A.toByte(),
                            0x50.toByte(),
                            0x20.toByte(),
                            0x20.toByte(),
                            0x0D.toByte(),
                            0x0A.toByte()
                        )
            )
        )
    ),
    PSD(
        false, "psd", setOf(
            mapOf(
                0 to "8BPS".toByteArray(Charsets.US_ASCII)
            )
        )
    ),
    SVG(
        false, "svg", setOf(
            mapOf(
                0 to "<svg ".toByteArray(Charsets.US_ASCII)
            )
        )
    );
}