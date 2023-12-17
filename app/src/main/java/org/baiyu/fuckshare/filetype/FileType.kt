package org.baiyu.fuckshare.filetype

import java.nio.ByteBuffer

interface FileType {
    val extension: String?
    val signatures: Set<Map<Int, ByteArray>>?
    val isSupportMetadata: Boolean
        get() = false

    fun signatureMatch(bytes: ByteArray?): Boolean {
        bytes ?: return false

        return signatures?.parallelStream()?.anyMatch { integerMap ->
            integerMap.entries.parallelStream()
                .allMatch {
                    it.key + it.value.size >= bytes.size
                            && ByteBuffer.wrap(it.value)
                        .equals(ByteBuffer.wrap(bytes, it.key, it.value.size))
                }
        } ?: false
    }
}