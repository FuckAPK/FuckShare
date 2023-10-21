package org.baiyu.fuckshare

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.provider.OpenableColumns
import org.baiyu.fuckshare.filetype.ArchiveType
import org.baiyu.fuckshare.filetype.AudioType
import org.baiyu.fuckshare.filetype.FileType
import org.baiyu.fuckshare.filetype.ImageType
import org.baiyu.fuckshare.filetype.OtherType
import org.baiyu.fuckshare.filetype.VideoType
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Objects
import java.util.UUID

object Utils {
    fun getUrisFromIntent(intent: Intent): List<Uri?>? {
        return if (Intent.ACTION_SEND == intent.action) {
            val uri = getParcelableExtra(
                intent,
                Intent.EXTRA_STREAM,
                Uri::class.java
            )!!
            listOf(uri)
        } else {
            getParcelableArrayListExtra(
                intent,
                Intent.EXTRA_STREAM,
                Uri::class.java
            )
        }
    }

    fun <T : Parcelable?> getParcelableExtra(intent: Intent, name: String?, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(name, clazz)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(name)
        }
    }

    fun <T : Parcelable?> getParcelableArrayListExtra(
        intent: Intent,
        name: String?,
        clazz: Class<T>
    ): ArrayList<T>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(name, clazz)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(name)
        }
    }

    fun getRealFileName(context: Context, uri: Uri): String? {
        if (ContentResolver.SCHEME_FILE == uri.scheme) {
            val file = File(Objects.requireNonNull(uri.path))
            return file.name
        } else if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            context.contentResolver.query(uri, null, null, null, null).use { cursor ->
                assert(cursor != null)
                cursor!!.moveToFirst()
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                return cursor.getString(nameIndex)
            }
        } else {
            Timber.e("Unknown scheme: %s", uri.scheme)
            return null
        }
    }

    fun getFileNameNoExt(fullFilename: String): String {
        val lastIndex = fullFilename.lastIndexOf('.')
        return if (lastIndex > 0 && lastIndex < fullFilename.length - 1) {
            fullFilename.substring(0, lastIndex)
        } else {
            fullFilename
        }
    }

    fun getFileRealExt(fullFilename: String): String? {
        val lastIndex = fullFilename.lastIndexOf('.')
        return if (lastIndex > 0 && lastIndex < fullFilename.length - 1) {
            fullFilename.substring(lastIndex + 1)
        } else {
            null
        }
    }

    fun mergeFilename(filename: String, extension: String?): String {
        return if (extension == null) filename else "${filename}.${extension}"
    }

    val randomString: String
        get() = UUID.randomUUID().toString()

    /**
     * copy $len bytes from inputStream to outputStream if available
     *
     * @return number of bytes copied
     */
    @Throws(IOException::class)
    fun copy(inputStream: InputStream, outputStream: OutputStream, len: Long): Long {
        val buffer = ByteArray(8192) // Set the buffer size as per your requirement
        var bytesRemaining = len
        while (bytesRemaining > 0) {
            val bytesRead =
                inputStream.read(buffer, 0, minOf(buffer.size.toLong(), bytesRemaining).toInt())
            if (bytesRead == -1) {
                break
            }
            outputStream.write(buffer, 0, bytesRead)
            bytesRemaining -= bytesRead
        }
        return len - bytesRemaining
    }

    fun getFileType(bytes: ByteArray?): FileType {
        val fileTypes = setOf(
            *ImageType.values(),
            *VideoType.values(),
            *AudioType.values(),
            *ArchiveType.values(),
            *OtherType.values()
        )
        return fileTypes.parallelStream()
            .filter { it.signatureMatch(bytes) }
            .findAny()
            .orElse(OtherType.UNKNOWN)
    }

    fun bigEndianBytesToLong(bytes: ByteArray): Long {
        val newBytes = ByteArray(8)
        System.arraycopy(bytes, 0, newBytes, 8 - bytes.size, bytes.size)
        return ByteBuffer.wrap(newBytes).apply {
            order(ByteOrder.BIG_ENDIAN)
        }.long
    }

    fun littleEndianBytesToLong(bytes: ByteArray): Long {
        val newBytes = bytes.copyOf(8)
        return ByteBuffer.wrap(newBytes).apply {
            order(ByteOrder.LITTLE_ENDIAN)
        }.long
    }

    fun longToBytes(num: Long, order: ByteOrder): ByteArray {
        val bb = ByteBuffer.allocate(8).apply {
            order(order)
        }
        bb.putLong(num)
        return bb.array()
    }

    /** @noinspection UnusedReturnValue
     */
    @Throws(IOException::class)
    fun inputStreamRead(inputStream: InputStream, bytes: ByteArray): Int {
        return inputStreamRead(inputStream, bytes, 0, bytes.size)
    }

    @Throws(IOException::class)
    fun inputStreamRead(inputStream: InputStream, b: ByteArray, off: Int, len: Int): Int {
        Objects.checkFromIndexSize(off, len, b.size)
        var count: Int
        var n = 0
        while (n < len) {
            count = inputStream.read(b, off + n, len - n)
            if (count < 0) break
            n += count
        }
        return n
    }

    fun clearCache(context: Context, timeDurationMillis: Long): Boolean {
        val timeBefore = System.currentTimeMillis() - timeDurationMillis
        return context.cacheDir.listFiles()?.asSequence()
            ?.filter { it.lastModified() < timeBefore }
            ?.all { it.deleteRecursively() }
            ?: true
    }
}