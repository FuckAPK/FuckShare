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
//        val signatureRequireLength = signature
//            .flatMap { it.entries }.maxOfOrNull { it.key + it.value.size } ?: return false
//        if (signatureRequireLength <= 0 || bytes.size < signatureRequireLength) {
//            return false
//        }

        return signature.parallelStream().anyMatch { integerMap ->
            integerMap.entries.parallelStream()
                .allMatch {
                    ByteBuffer.wrap(it.value).equals(ByteBuffer.wrap(bytes, it.key, it.value.size))
                }
        }
    }
}