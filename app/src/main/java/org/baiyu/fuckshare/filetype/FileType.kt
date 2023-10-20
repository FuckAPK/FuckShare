package org.baiyu.fuckshare.filetype

import java.nio.ByteBuffer

interface FileType {
    val extension: String?
    val signatures: Set<Map<Int, ByteArray>>?
    val isSupportMetadata: Boolean
        get() = false

    fun signatureMatch(bytes: ByteArray?): Boolean {
        val signature = signatures
        if (bytes == null || signature == null) {
            return false
        }
        val signatureRequireLength = signature.parallelStream()
            .flatMap { it.entries.stream() }
            .map { it.key + it.value.size }
            .max { i, i2 -> i - i2 }
            .orElse(Int.MAX_VALUE)

        return if (signatureRequireLength <= 0 || bytes.size < signatureRequireLength) {
            false
        } else signature.parallelStream().anyMatch { integerMap ->
            integerMap.entries.parallelStream()
                .allMatch {
                    ByteBuffer.wrap(it.value).equals(ByteBuffer.wrap(bytes, it.key, it.value.size))
                }
        }
    }
}