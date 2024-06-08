package org.baiyu.fuckshare.filetype

@Suppress("SpellCheckingInspection")
enum class VideoType(
    override val extension: String,
    override val signatures: Set<Map<Int, ByteArray>>
) : FileType {
    MP4(
        "mp4", setOf(
            mapOf(
                4 to
                        "ftypisom".toByteArray(Charsets.US_ASCII)
            ),
            mapOf(
                4 to
                        "ftypiso2".toByteArray(Charsets.US_ASCII)
            ),
            mapOf(
                4 to
                        "ftypiso5".toByteArray(Charsets.US_ASCII)
            ),
            mapOf(
                4 to
                        "ftypmmp4".toByteArray(Charsets.US_ASCII)
            ),
            mapOf(
                4 to
                        "ftypmp41".toByteArray(Charsets.US_ASCII)
            ),
            mapOf(
                4 to
                        "ftypmp42".toByteArray(Charsets.US_ASCII)
            ),
            mapOf(
                4 to
                        "ftypavc1".toByteArray(Charsets.US_ASCII)
            )
        )
    ),
    AVI(
        "avi", setOf(
            mapOf(
                0 to "RIFF".toByteArray(Charsets.US_ASCII),
                8 to "AVI ".toByteArray(Charsets.US_ASCII)
            )
        )
    ),
    MPEG(
        "mpeg", setOf(
            mapOf(
                0 to
                        byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0xB3.toByte())
            ),
            mapOf(
                0 to
                        byteArrayOf(0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0xBA.toByte())
            )
        )
    ),
    MKV(
        "mkv", setOf(
            mapOf(
                0 to
                        byteArrayOf(0x1A.toByte(), 0x45.toByte(), 0xDF.toByte(), 0xA3.toByte())
            )
        )
    );
}