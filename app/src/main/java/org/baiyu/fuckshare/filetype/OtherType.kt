package org.baiyu.fuckshare.filetype

enum class OtherType(
    override val extension: String?,
    override val signatures: Set<Map<Int, ByteArray>>?
) : FileType {
    UNKNOWN(null, null),
    PDF(
        "pdf", setOf(
            mapOf(
                0 to "%PDF-".toByteArray(Charsets.US_ASCII)
            )
        )
    ),
    M3U(
        "m3u", setOf(
            mapOf(
                0 to "#EXTM3U".toByteArray(Charsets.US_ASCII)
            )
        )
    );
}