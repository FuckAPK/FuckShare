package org.baiyu.fuckshare.filetype

enum class AudioType(
    override val extension: String,
    override val signatures: Set<Map<Int, ByteArray>>
) : FileType {
    MP3(
        "mp3", setOf(
            mapOf(0 to byteArrayOf(0xFF.toByte(), 0xFB.toByte())),
            mapOf(0 to byteArrayOf(0xFF.toByte(), 0xF2.toByte())),
            mapOf(0 to byteArrayOf(0xFF.toByte(), 0xF3.toByte())),
            mapOf(0 to byteArrayOf(0x49.toByte(), 0x44.toByte(), 0x33.toByte()))
        )
    ),
    FLAC(
        "flac", setOf(
            mapOf(
                0 to
                        byteArrayOf(0x66.toByte(), 0x4C.toByte(), 0x61.toByte(), 0x43.toByte())
            )
        )
    ),
    WAVE(
        "wav", setOf(
            mapOf(
                0 to byteArrayOf(0x52.toByte(), 0x49.toByte(), 0x46.toByte(), 0x46.toByte()),
                8 to byteArrayOf(0x57.toByte(), 0x41.toByte(), 0x56.toByte(), 0x45.toByte())
            )
        )
    ),
    OGG(
        "ogg", setOf(
            mapOf(
                0 to
                        byteArrayOf(0x4F.toByte(), 0x67.toByte(), 0x67.toByte(), 0x53.toByte())
            )
        )
    ),
    AIFF(
        "aiff", setOf(
            mapOf(
                0 to byteArrayOf(0x46.toByte(), 0x4F.toByte(), 0x52.toByte(), 0x4D.toByte()),
                8 to byteArrayOf(0x41.toByte(), 0x49.toByte(), 0x46.toByte(), 0x46.toByte())
            )
        )
    );
}