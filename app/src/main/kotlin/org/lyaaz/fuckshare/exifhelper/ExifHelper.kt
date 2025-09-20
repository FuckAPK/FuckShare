package org.lyaaz.fuckshare.exifhelper

import androidx.exifinterface.media.ExifInterface
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

interface ExifHelper {
    @Throws(IOException::class, ImageFormatException::class)
    fun removeMetadata(inputStream: InputStream, outputStream: OutputStream)

    @Throws(IOException::class)
    fun postProcess(file: File) {
        // Default implementation does nothing
    }

    companion object {
        // Common constants
        const val WEBP_HEADER_SIZE = 12L
        const val CHUNK_LENGTH_SIZE = 4
        const val CHUNK_NAME_SIZE = 4
        const val JPEG_MARKER_SIZE = 2
        const val PNG_HEADER_SIZE = 8L
        const val CRC_SIZE = 4

        @Throws(IOException::class)
        fun writeBackMetadata(exifFrom: ExifInterface, exifTo: ExifInterface, tags: Set<String?>) {
            val tagsValue = tags.asSequence()
                .filterNotNull()
                .map { it to exifFrom.getAttribute(it) }
                .filterNot { it.second == null }
                .filterNot { it.first == ExifInterface.TAG_ORIENTATION && it.second == ExifInterface.ORIENTATION_UNDEFINED.toString() }

            if (tagsValue.count() == 0) {
                Timber.d("no tags rewrite")
                return
            }

            tagsValue.forEach { exifTo.setAttribute(it.first, it.second) }
            Timber.d("tags rewrite: ${tagsValue.toMap()}")
            exifTo.saveAttributes()
        }
    }
}