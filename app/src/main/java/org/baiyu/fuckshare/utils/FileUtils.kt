package org.baiyu.fuckshare.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import org.baiyu.fuckshare.BuildConfig
import org.baiyu.fuckshare.Settings
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

/**
 * Utility class for handling file-related operations, such as copying, renaming, and processing image metadata.
 */
object FileUtils {

    /**
     * Refreshes the provided URI by copying the file content to a new file with potential metadata processing and renaming.
     *
     * @param context The context used for file operations.
     * @param settings The application settings.
     * @param uri The original URI to refresh.
     * @return The refreshed URI or null if the operation fails.
     */
    fun refreshUri(context: Context, settings: Settings, uri: Uri): Uri? {
        val originName = getRealFileName(context, uri) ?: AppUtils.randomString
        var tempFile = File(context.cacheDir, AppUtils.randomString)
        return try {
            val magickBytes = ByteArray(16)
            context.contentResolver.openInputStream(uri)!!.buffered().use { uin ->
                ByteUtils.readNBytes(uin, magickBytes)
            }
            val fileType = getFileType(magickBytes)
            if (fileType is ImageType
                && fileType.supportMetadata
                && settings.enableRemoveExif()
            ) {
                processImage(context, settings, tempFile, fileType, uri)
            } else {
                copyFileFromUri(context, uri, tempFile)
            }
            // rename
            createNewName(context, settings, fileType, originName).let {
                if (tempFile.renameTo(it)) {
                    tempFile = it
                }
            }
            FileProvider.getUriForFile(
                context,
                "${BuildConfig.APPLICATION_ID}.fileprovider",
                tempFile
            )
        } catch (e: IOException) {
            Timber.e(e)
            null
        }
    }

    /**
     * Copies a specified number of bytes from the input stream to the output stream.
     *
     * @param inputStream The input stream to copy from.
     * @param outputStream The output stream to copy to.
     * @param len The number of bytes to copy.
     * @return The number of bytes actually copied.
     * @throws IOException If an I/O error occurs.
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

    /**
     * Copies the content of the input stream to the specified file.
     */
    @Throws(IOException::class)
    private fun copyFileFromUri(context: Context, uri: Uri, file: File) {
        context.contentResolver.openInputStream(uri)!!.buffered().use { uin ->
            file.outputStream().buffered().use { fout ->
                uin.copyTo(fout)
            }
        }
    }

    @Throws(IOException::class)
    private fun processImage(
        context: Context,
        settings: Settings,
        file: File,
        imageType: ImageType,
        uri: Uri
    ) {
        try {
            processImgMetadata(context, settings, file, imageType, uri)
        } catch (e: ImageFormatException) {
            file.delete()
            Timber.e("Format error: $uri Type: $imageType")
            if (settings.enableFallbackToFile()) {
                Timber.d("fallback to file: $uri")
                copyFileFromUri(context, uri, file)
            }
        }
    }

    /**
     * Processes the metadata of an image file, removing metadata if needed.
     */
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

        eh?.let { exifHelper ->
            context.contentResolver.openInputStream(uri)!!.buffered().use { input ->
                file.outputStream().buffered().use { output ->
                    exifHelper.removeMetadata(input, output)
                }
            }
            if (exifHelper is WebpExifHelper) {
                RandomAccessFile(file, "rw").use {
                    exifHelper.fixHeaderSize(it)
                }
            }
            if (imageType.supportMetadata) {
                context.contentResolver.openInputStream(uri)!!.use { input ->
                    ExifHelper.writeBackMetadata(
                        ExifInterface(input),
                        ExifInterface(file),
                        settings.exifTagsToKeep
                    )
                }
            }
        } ?: Timber.e("unsupported image type: $imageType")
    }

    private fun createNewName(
        context: Context,
        settings: Settings,
        fileType: FileType,
        originName: String
    ): File {
        val newNameNoExt = getNewNameNoExt(settings, fileType, originName)
        val ext = getExt(settings, fileType, originName)
        val newFullName = if (ext == null) newNameNoExt else "${newNameNoExt}.${ext}"
        var renamed = File(context.cacheDir, newFullName)
        if (renamed.exists()) {
            val oneTimeCacheDir = File(context.cacheDir, AppUtils.randomString)
            oneTimeCacheDir.mkdirs()
            renamed = File(oneTimeCacheDir, newFullName)
        }
        return renamed
    }

    /**
     * Gets the new name (excluding extension) for a file based on settings and file type.
     */
    private fun getNewNameNoExt(
        settings: Settings,
        fileType: FileType?,
        originName: String?
    ): String {
        return when (fileType) {
            is ImageType -> if (settings.enableImageRename()) AppUtils.randomString else getFileNameNoExt(
                originName!!
            )

            else -> if (settings.enableFileRename()) AppUtils.randomString else getFileNameNoExt(
                originName!!
            )
        }
    }

    /**
     * Gets the file extension based on user settings, file type, and original name.
     */
    private fun getExt(settings: Settings, fileType: FileType, originName: String): String? {
        var extension: String? = if (settings.enableFileTypeSniff()) {
            if (fileType is ArchiveType && !settings.enableArchiveTypeSniff()) {
                null
            } else {
                fileType.extension
            }
        } else {
            null
        }
        if (extension == null) {
            val lastIndex = originName.lastIndexOf('.')
            extension = if (lastIndex > 0 && lastIndex < originName.length - 1) {
                originName.substring(lastIndex + 1)
            } else {
                null
            }
        }
        return extension
    }

    /**
     * Retrieves the real file name from the provided URI, considering different URI schemes.
     */
    private fun getRealFileName(context: Context, uri: Uri): String? {
        if (ContentResolver.SCHEME_FILE == uri.scheme) {
            val file = uri.path?.let { File(it) } ?: return null
            return file.name
        } else if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            context.contentResolver.query(uri, null, null, null, null).use { cursor ->
                cursor!!.moveToFirst()
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                return cursor.getString(nameIndex)
            }
        } else {
            Timber.e("Unknown scheme: ${uri.scheme}")
            return null
        }
    }

    /**
     * Gets the file name without extension from the full filename.
     */
    private fun getFileNameNoExt(fullFilename: String): String {
        val lastIndex = fullFilename.lastIndexOf('.')
        return if (lastIndex > 0 && lastIndex < fullFilename.length - 1) {
            fullFilename.substring(0, lastIndex)
        } else {
            fullFilename
        }
    }

    /**
     * Determines the file type based on the file's byte signature.
     */
    private fun getFileType(bytes: ByteArray): FileType {
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
}