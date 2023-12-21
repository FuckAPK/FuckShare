package org.baiyu.fuckshare.filetype

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
                0 to byteArrayOf(0x52.toByte(), 0x49.toByte(), 0x46.toByte(), 0x46.toByte()),
                8 to byteArrayOf(0x57.toByte(), 0x45.toByte(), 0x42.toByte(), 0x50.toByte())
            )
        )
    ),
    GIF(
        false, "gif", setOf(
            mapOf(
                0 to
                        byteArrayOf(
                            0x47.toByte(),
                            0x49.toByte(),
                            0x46.toByte(),
                            0x38.toByte(),
                            0x37.toByte(),
                            0x61.toByte()
                        )
            ),
            mapOf(
                0 to
                        byteArrayOf(
                            0x47.toByte(),
                            0x49.toByte(),
                            0x46.toByte(),
                            0x38.toByte(),
                            0x39.toByte(),
                            0x61.toByte()
                        )
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
    PSD(
        false, "psd", setOf(
            mapOf(
                0 to
                        byteArrayOf(0x38.toByte(), 0x42.toByte(), 0x50.toByte(), 0x53.toByte())
            )
        )
    ),
    SVG(
        false, "svg", setOf(
            mapOf(
                0 to
                        byteArrayOf(
                            0x3c.toByte(),
                            0x73.toByte(),
                            0x76.toByte(),
                            0x67.toByte(),
                            0x20.toByte()
                        )
            )
        )
    );
}