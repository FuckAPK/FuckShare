package org.baiyu.fuckshare.filetype

enum class OtherType(
    override val extension: String?,
    override val signatures: Set<Map<Int, ByteArray>>?
) : FileType {
    UNKNOWN(null, null), PDF(
        "pdf", setOf(
            mapOf(
                0 to
                        byteArrayOf(
                            0x25.toByte(),
                            0x50.toByte(),
                            0x44.toByte(),
                            0x46.toByte(),
                            0x2D.toByte()
                        )
            )
        )
    ),
    M3U(
        "m3u", setOf(
            mapOf(
                0 to
                        byteArrayOf(
                            0x23.toByte(),
                            0x45.toByte(),
                            0x58.toByte(),
                            0x54.toByte(),
                            0x4D.toByte(),
                            0x33.toByte(),
                            0x55.toByte()
                        )
            )
        )
    )

}