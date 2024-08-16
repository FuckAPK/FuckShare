package org.lyaaz.fuckshare.filetype

import android.content.Context
import android.net.Uri
import org.lyaaz.fuckshare.utils.ByteUtils
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

    companion object {
        /**
         * Gets the file type from the file's URI.
         */
        fun fromUri(context: Context, uri: Uri): FileType {
            val magickBytes = ByteArray(16)
            return runCatching {
                context.contentResolver.openInputStream(uri)!!.buffered().use { uin ->
                    ByteUtils.readNBytes(uin, magickBytes)
                }
                fromMagickBytes(magickBytes)
            }.onSuccess {
                val magickByteStr = magickBytes.joinToString(separator = "") { str ->
                    "%02X".format(str)
                }
                if (it == OtherType.UNKNOWN) {
                    Timber.w("Unknown file type: $uri, bytes: $magickByteStr")
                } else {
                    Timber.i("File type: $it, uri: $uri, bytes: $magickByteStr")
                }
            }.onFailure {
                Timber.e(it)
            }.getOrDefault(OtherType.UNKNOWN)
        }

        /**
         * Determines the file type based on the file's byte signature.
         */
        private fun fromMagickBytes(bytes: ByteArray): FileType {
            val fileTypes = setOf(
                *ImageType.entries.toTypedArray(),
                *VideoType.entries.toTypedArray(),
                *AudioType.entries.toTypedArray(),
                *OtherType.entries.toTypedArray()
            )
            return fileTypes.parallelStream()
                .filter { it.signatureMatch(bytes) }
                .findAny()
                .orElse(OtherType.UNKNOWN)
        }
    }
}