package org.baiyu.fuckshare

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import org.baiyu.fuckshare.exifhelper.ExifHelper
import org.baiyu.fuckshare.exifhelper.ImageFormatException
import org.baiyu.fuckshare.exifhelper.JpegExifHelper
import org.baiyu.fuckshare.exifhelper.PngExifHelper
import org.baiyu.fuckshare.exifhelper.WebpExifHelper
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
import java.io.RandomAccessFile
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

    fun <T : Parcelable?> getParcelableExtra(
        intent: Intent,
        name: String?,
        clazz: Class<T>
    ): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(name, clazz)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(name)
        }
    }

    private fun <T : Parcelable?> getParcelableArrayListExtra(
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


    fun refreshUri(context: Context, settings: Settings, uri: Uri): Uri? {
        val originName = getRealFileName(context, uri)
        var tempFile = File(context.cacheDir, randomString)
        return try {
            val magickBytes = ByteArray(16)
            context.contentResolver.openInputStream(uri)!!.buffered().use { uin ->
                readNBytes(uin, magickBytes)
            }
            var fileType = getFileType(magickBytes)
            if (fileType is ImageType
                && fileType.isSupportMetadata
                && settings.enableRemoveExif()
            ) {
                try {
                    processImgMetadata(context, settings, tempFile, fileType, uri)
                } catch (e: ImageFormatException) {
                    tempFile.delete()
                    Timber.e("Format error: %s Type: %s", originName, fileType)
                    Toast.makeText(
                        context,
                        "Format error: $originName Type: $fileType",
                        Toast.LENGTH_SHORT
                    ).show()
                    fileType = if (settings.enableFallbackToFile()) {
                        copyFileFromUri(context, uri, tempFile)
                        OtherType.UNKNOWN
                    } else {
                        return null
                    }
                }
            } else {
                copyFileFromUri(context, uri, tempFile)
            }
            // rename
            val newNameNoExt = getNewNameNoExt(settings, fileType, originName)
            val ext = getExt(settings, fileType, originName)
            val newFullName = mergeFilename(newNameNoExt, ext)
            var renamed = File(context.cacheDir, newFullName)
            if (renamed.exists()) {
                val oneTimeCacheDir = File(context.cacheDir, randomString)
                oneTimeCacheDir.mkdirs()
                renamed = File(oneTimeCacheDir, newFullName)
            }
            if (tempFile.renameTo(renamed)) {
                tempFile = renamed
            }
            FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".fileprovider",
                tempFile
            )
        } catch (e: IOException) {
            Timber.e(e)
            Toast.makeText(context, "Failed to process: $originName", Toast.LENGTH_SHORT).show()
            null
        }
    }

    @Throws(IOException::class)
    private fun copyFileFromUri(context: Context, uri: Uri, file: File) {
        context.contentResolver.openInputStream(uri)!!.buffered().use { uin ->
            file.outputStream().buffered().use { fout ->
                uin.copyTo(fout)
            }
        }
    }

    @Throws(IOException::class, ImageFormatException::class)
    private fun processImgMetadata(
        context: Context,
        settings: Settings,
        file: File,
        imageType: ImageType,
        uri: Uri
    ) {
        val eh: ExifHelper? = when (imageType) {
            ImageType.JPEG -> JpegExifHelper()
            ImageType.PNG -> PngExifHelper()
            ImageType.WEBP -> WebpExifHelper()
            else -> null
        }
        if (eh == null) {
            Timber.e("unsupported image type: %s", imageType)
        } else {
            context.contentResolver.openInputStream(uri)!!.buffered().use { uin ->
                file.outputStream().buffered().use { fout ->
                    eh.removeMetadata(uin, fout)
                }
            }
            (eh as? WebpExifHelper)?.let { webpExifHelper ->
                RandomAccessFile(file, "rw").use {
                    webpExifHelper.fixHeaderSize(it)
                }
            }
            if (imageType.isSupportMetadata) {
                context.contentResolver.openInputStream(uri)!!.use { uin ->
                    ExifHelper.writeBackMetadata(
                        ExifInterface(uin),
                        ExifInterface(file),
                        settings.exifTagsToKeep
                    )
                }
            }
        }
    }

    private fun getNewNameNoExt(
        settings: Settings,
        fileType: FileType?,
        originName: String?
    ): String {
        return when (fileType) {
            is ImageType -> if (settings.enableImageRename()) randomString else getFileNameNoExt(
                originName!!
            )

            else -> if (settings.enableFileRename()) randomString else getFileNameNoExt(
                originName!!
            )
        }
    }

    private fun getExt(settings: Settings, fileType: FileType, originName: String?): String? {
        var extension: String? = null
        if (settings.enableFileTypeSniff()) {
            extension = fileType.extension
        }
        if (extension == null) {
            extension = getFileRealExt(originName!!)
        }
        return extension
    }


    private fun getRealFileName(context: Context, uri: Uri): String? {
        if (ContentResolver.SCHEME_FILE == uri.scheme) {
            val file = File(Objects.requireNonNull(uri.path))
            return file.name
        } else if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            context.contentResolver.query(uri, null, null, null, null).use { cursor ->
                cursor!!.moveToFirst()
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                return cursor.getString(nameIndex)
            }
        } else {
            Timber.e("Unknown scheme: %s", uri.scheme)
            return null
        }
    }

    private fun getFileNameNoExt(fullFilename: String): String {
        val lastIndex = fullFilename.lastIndexOf('.')
        return if (lastIndex > 0 && lastIndex < fullFilename.length - 1) {
            fullFilename.substring(0, lastIndex)
        } else {
            fullFilename
        }
    }

    private fun getFileRealExt(fullFilename: String): String? {
        val lastIndex = fullFilename.lastIndexOf('.')
        return if (lastIndex > 0 && lastIndex < fullFilename.length - 1) {
            fullFilename.substring(lastIndex + 1)
        } else {
            null
        }
    }

    private fun mergeFilename(filename: String, extension: String?): String {
        return if (extension == null) filename else "${filename}.${extension}"
    }

    private val randomString: String
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

    private fun getFileType(bytes: ByteArray?): FileType {
        val fileTypes = setOf(
            *ImageType.entries.toTypedArray(),
            *VideoType.entries.toTypedArray(),
            *AudioType.entries.toTypedArray(),
            *ArchiveType.entries.toTypedArray(),
            *OtherType.entries.toTypedArray()
        )
        return fileTypes.parallelStream()
            .filter { it.signatureMatch(bytes) }
            .findAny()
            .orElse(OtherType.UNKNOWN)
    }

    /**
     * 4.. bytes to UInt
     */
    fun ByteArray.toUInt(order: ByteOrder): UInt {
        return ByteBuffer.wrap(this).apply {
            order(order)
        }.int.toUInt()
    }

    /**
     * 2.. bytes to UShort
     */
    fun ByteArray.toUShort(order: ByteOrder): UShort {
        return ByteBuffer.wrap(this).apply {
            order(order)
        }.short.toUShort()
    }

    @Throws(IOException::class)
    fun readNBytes(inputStream: InputStream, bytes: ByteArray): Int {
        return readNBytes(inputStream, bytes, 0, bytes.size)
    }

    /**
     * copy of InputStream.readNBytes, which has a api 33 limit
     */
    @Throws(IOException::class)
    fun readNBytes(inputStream: InputStream, b: ByteArray, off: Int, len: Int): Int {
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

    /**
     * copy (not extract) of InputStream.skipNBytes with return, which has a api 33 limit
     */
    fun skipNBytes(inputStream: InputStream, len: Long): Long {
        var n = len
        while (n > 0L) {
            val ns: Long = inputStream.skip(n)
            if (ns in 1L..n) {
                n -= ns
                continue
            }
            if (ns == 0L) {
                if (inputStream.read() == -1) {
                    break
                }
                --n
                continue
            }
            throw IOException("Unable to skip exactly")
        }
        return len - n
    }

    fun clearCache(context: Context, timeDurationMillis: Long): Boolean {
        val timeBefore = System.currentTimeMillis() - timeDurationMillis
        return context.cacheDir.listFiles()?.asSequence()
            ?.filter { it.lastModified() < timeBefore }
            ?.all { it.deleteRecursively() }
            ?: true
    }
}