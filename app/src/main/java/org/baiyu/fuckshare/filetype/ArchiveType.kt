package org.baiyu.fuckshare.filetype

enum class ArchiveType(
    override val extension: String,
    override val signatures: Set<Map<Int, ByteArray>>
) : FileType {
    LZIP(
        "lz", setOf(
            mapOf(
                0 to "LZIP".toByteArray(Charsets.US_ASCII)
            )
        )
    ),
    RAR(
        "rar", setOf(
            mapOf(
                0 to
                        byteArrayOf(
                            0x52.toByte(),
                            0x61.toByte(),
                            0x72.toByte(),
                            0x21.toByte(),
                            0x1A.toByte(),
                            0x07.toByte(),
                            0x00.toByte()
                        )
            )
        )
    ),
    TAR(
        "tar", setOf(
            mapOf(
                0 to
                        byteArrayOf(
                            *"ustar".toByteArray(Charsets.US_ASCII),
                            0x00.toByte(),
                            0x30.toByte(),
                            0x30.toByte()
                        )
            ),
            mapOf(
                0 to
                        byteArrayOf(
                            *"ustar".toByteArray(Charsets.US_ASCII),
                            0x20.toByte(),
                            0x20.toByte(),
                            0x00.toByte()
                        )
            )
        )
    ),
    _7Z(
        "7z", setOf(
            mapOf(
                0 to
                        byteArrayOf(
                            0x37.toByte(),
                            0x7A.toByte(),
                            0xBC.toByte(),
                            0xAF.toByte(),
                            0x27.toByte(),
                            0x1C.toByte()
                        )
            )
        )
    ),
    GZ(
        "gz", setOf(
            mapOf(0 to byteArrayOf(0x1F.toByte(), 0x8B.toByte()))
        )
    ),
    XZ(
        "xz", setOf(
            mapOf(
                0 to byteArrayOf(0xFD.toByte()),
                1 to "7zXZ".toByteArray(Charsets.US_ASCII)
            )
        )
    );
}