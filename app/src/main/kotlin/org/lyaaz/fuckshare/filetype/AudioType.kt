package org.lyaaz.fuckshare.filetype

enum class AudioType(
    override val extension: String,
    override val signatures: Set<Map<Int, ByteArray>>
) : FileType {
    MP3(
        "mp3", setOf(
            mapOf(0 to byteArrayOf(0xFF.toByte(), 0xFB.toByte())),
            mapOf(0 to byteArrayOf(0xFF.toByte(), 0xF2.toByte())),
            mapOf(0 to byteArrayOf(0xFF.toByte(), 0xF3.toByte())),
            mapOf(0 to "ID3".toByteArray(Charsets.US_ASCII))
        )
    ),
    M4A(
        "m4a", setOf(
            mapOf(4 to "ftypM4A".toByteArray(Charsets.US_ASCII))
        )
    ),
    FLAC(
        "flac", setOf(
            mapOf(
                0 to "fLaC".toByteArray(Charsets.US_ASCII)
            )
        )
    ),
    WAVE(
        "wav", setOf(
            mapOf(
                0 to "RIFF".toByteArray(Charsets.US_ASCII),
                8 to "WAVE".toByteArray(Charsets.US_ASCII)
            )
        )
    ),
    OGG(
        "ogg", setOf(
            mapOf(
                0 to "OggS".toByteArray(Charsets.US_ASCII)
            )
        )
    ),
    AIFF(
        "aiff", setOf(
            mapOf(
                0 to "FORM".toByteArray(Charsets.US_ASCII),
                8 to "AIFF".toByteArray(Charsets.US_ASCII)
            )
        )
    );
}