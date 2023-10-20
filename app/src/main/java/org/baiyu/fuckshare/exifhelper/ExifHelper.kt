package org.baiyu.fuckshare.exifhelper

import androidx.exifinterface.media.ExifInterface
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

interface ExifHelper {
    @Throws(IOException::class, ImageFormatException::class)
    fun removeMetadata(inputStream: InputStream, outputStream: OutputStream?)

    companion object {
        @Throws(IOException::class)
        fun writeBackMetadata(exifFrom: ExifInterface, exifTo: ExifInterface, tags: Set<String?>) {
            val tagsValue = tags.asSequence().filterNotNull()
                .filter { exifFrom.hasAttribute(it) }
                .map { it to exifFrom.getAttribute(it) }

            tagsValue.forEach { exifTo.setAttribute(it.first, it.second) }
            Timber.d("tags rewrite: %s", tagsValue.toMap().toString())
            exifTo.saveAttributes()
        }
    }
}