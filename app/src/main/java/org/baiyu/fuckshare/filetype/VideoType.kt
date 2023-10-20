package org.baiyu.fuckshare.filetype

enum class VideoType(
    override val extension: String,
    override val signatures: Set<Map<Int, ByteArray>>
) : FileType {
    MP4(
        "mp4", setOf(
            mapOf(
                0 to
                        byteArrayOf(
                            0x00.toByte(),
                            0x00.toByte(),
                            0x00.toByte(),
                            0x20.toByte(),
                            0x66.toByte(),
                            0x74.toByte(),
                            0x79.toByte(),
                            0x70.toByte(),
                            0x69.toByte(),
                            0x73.toByte(),
                            0x6F.toByte(),
                            0x6D.toByte()
                        )
            )
        )
    ),
    AVI(
        "avi", setOf(
            mapOf(
                0 to byteArrayOf(0x52.toByte(), 0x49.toByte(), 0x46.toByte(), 0x46.toByte()),
                8 to byteArrayOf(0x41.toByte(), 0x56.toByte(), 0x49.toByte(), 0x20.toByte())
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