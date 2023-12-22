package org.baiyu.fuckshare.filetype

import timber.log.Timber
import java.nio.ByteBuffer

interface FileType {
    val extension: String?
    val signatures: Set<Map<Int, ByteArray>>?
    val supportMetadata: Boolean
        get() = false

    fun signatureMatch(bytes: ByteArray): Boolean {
        return signatures?.parallelStream()?.anyMatch { integerMap ->
            integerMap.entries.parallelStream()
                .allMatch {
                    if (bytes.size < it.key + it.value.size) {
                        Timber.e("bytes size too small to compare with signature")
                        return@allMatch false
                    }
                    ByteBuffer.wrap(it.value)
                        .equals(ByteBuffer.wrap(bytes, it.key, it.value.size))
                }
        } ?: false
    }
}