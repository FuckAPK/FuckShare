package org.lyaaz.fuckshare.utils

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.gifencoder.AnimatedGifEncoder
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.lyaaz.fuckshare.R
import org.lyaaz.fuckshare.Settings
import org.lyaaz.fuckshare.exifhelper.*
import org.lyaaz.fuckshare.filetype.FileType
import org.lyaaz.fuckshare.filetype.ImageType
import org.lyaaz.fuckshare.filetype.VideoType
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


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
    fun refreshUri(context: Context, settings: Settings, uri: Uri): Pair<Uri, FileType>? {
        return runCatching {
            val originName = getRealFileName(context, uri) ?: AppUtils.randomString
            val randomDir = File(context.cacheDir, AppUtils.randomString).also { it.mkdirs() }
            var tempFile = File(randomDir, AppUtils.randomString)
            var fileType = FileType.fromUri(context, uri)
            if (fileType is ImageType
                && fileType.supportMetadata
                && settings.enableRemoveExif
            ) {
                processImage(context, settings, tempFile, fileType, uri)
            } else {
                copyFileFromUri(context, uri, tempFile)
            }
            // video to gif
            if (fileType is VideoType && enableVideo2GIF(settings, tempFile)) {
                video2gif(tempFile, settings)?.let {
                    tempFile = it
                    fileType = ImageType.GIF
                } ?: run {
                    Toast.makeText(
                        context,
                        R.string.video_to_gif_failed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            // rename
            File(randomDir, createNewName(settings, fileType, originName)).let {
                if (tempFile.renameTo(it)) {
                    tempFile = it
                    Timber.d("Renamed: $tempFile -> $it")
                } else {
                    Timber.e("Failed to rename: $tempFile -> $it")
                }
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            ).let {
                it to fileType
            }
        }.onFailure {
            Timber.e(it)
        }.getOrNull()
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
        Timber.d("copyFileFromUri: $uri -> $file")
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
        runCatching {
            processImgMetadata(context, settings, file, imageType, uri)
        }.onFailure {
            file.delete()
            Timber.e("Format error: $uri Type: $imageType")
            if (settings.enableFallbackToFile) {
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
            exifHelper.postProcess(file)
            if (imageType.supportMetadata) {
                context.contentResolver.openInputStream(uri)!!.use { input ->
                    ExifHelper.writeBackMetadata(
                        ExifInterface(input),
                        ExifInterface(file),
                        settings.exifTagsToKeep
                    )
                    Timber.d("rewrite metadata: $file, tags: ${settings.exifTagsToKeep}")
                }
            }
        } ?: run {
            Timber.e("unsupported image type: $imageType")
        }
    }

    private fun createNewName(
        settings: Settings,
        fileType: FileType,
        originName: String
    ): String {
        val newNameNoExt =
            if (enableRandomName(settings, fileType))
                AppUtils.randomString
            else
                getFileNameNoExt(originName)
        val ext = getExt(settings, fileType, originName)
        val newFullName = if (ext == null) newNameNoExt else "${newNameNoExt}.${ext}"
        Timber.d("new name: $newFullName")
        return newFullName
    }

    /**
     * Whether to enable random name for a file based on settings and file type.
     */
    private fun enableRandomName(
        settings: Settings,
        fileType: FileType?
    ): Boolean {
        return when (fileType) {
            is ImageType -> settings.enableImageRename
            is VideoType -> settings.enableVideoRename
            else -> settings.enableFileRename
        }
    }

    /**
     * Whether to enable video to GIF conversion based on settings and file size.
     */
    private fun enableVideo2GIF(settings: Settings, file: File): Boolean {
        return settings.enableVideoToGIF
                && file.length() <= settings.videoToGifSizeKB * 1024L
                && (settings.videoToGIFForceWithAudio || !videoHasAudio(file.path))
    }

    /**
     * Gets the file extension based on user settings, file type, and original name.
     */
    private fun getExt(settings: Settings, fileType: FileType, originName: String): String? {
        var extension: String? = if (settings.enableFileTypeSniff) {
            fileType.extension
        } else {
            null
        }
        if (extension == null) {
            extension = originName.substringAfterLast('.', "")
        }
        return extension.takeIf { it.isNotBlank() }
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
        return fullFilename.substringBeforeLast('.')
    }

    /**
     * Converts a video to GIF.
     *
     * @param video The video file.
     * @return The GIF file or null if the operation fails.
     */
    fun video2gif(video: File, settings: Settings): File? {
        val gifFile = File(video.parent, "${video.nameWithoutExtension}.gif")
        runCatching {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(video.path)
            val frameCount =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toInt() ?: 1
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 1000L
            val frameRate = duration.toFloat().div(frameCount)

            val frames = retriever.getFramesAtIndex(0, frameCount)
            retriever.close()

            val gifEncoder = AnimatedGifEncoder()
            gifEncoder.start(gifFile.outputStream().buffered())
            gifEncoder.setFrameRate(frameRate)
            gifEncoder.setRepeat(0)
            val quality = when (Settings.VideoToGIFQualityOptions.fromValue(settings.videoToGIFQuality)) {
                Settings.VideoToGIFQualityOptions.LOW -> 100
                Settings.VideoToGIFQualityOptions.MEDIUM -> 30
                Settings.VideoToGIFQualityOptions.HIGH -> 10
                Settings.VideoToGIFQualityOptions.CUSTOM -> settings.videoToGIFCustomOption.toInt()
            }
            gifEncoder.setQuality(quality)

            frames.forEach { gifEncoder.addFrame(it) }
            gifEncoder.finish()
            return gifFile
        }.onFailure {
            Timber.e(it)
            gifFile.delete()
        }
        return null
    }

    /**
     * Checks if the video has audio.
     */
    fun videoHasAudio(videoPath: String): Boolean {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(videoPath)
        return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
    }

    /**
     * Image to bitmap
     */
    fun imageToBitmap(context: Context, uri: Uri): Bitmap? {
        return runCatching {
            context.contentResolver.openInputStream(uri)!!.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        }.onFailure {
            Timber.e(it)
        }.getOrNull()
    }

    /**
     * Decode QR code in Bitmap
     */
    fun decodeQRCode(bitmap: Bitmap): String? {
        val compressedBitmap = compressBitmap(bitmap, 800, 800)
        val width = compressedBitmap.width
        val height = compressedBitmap.height
        val pixels = IntArray(width * height)
        compressedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return try {
            val binaryBitmap =
                BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width, height, pixels)))
            MultiFormatReader().decode(binaryBitmap).text.takeIf { it.isNotBlank() }
        } catch (e: NotFoundException) {
            Timber.i(e)
            null
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    /**
     * Compress bitmap to specifical max width and height
     */
    fun compressBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val ratio = minOf(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height,
            1f
        )
        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()
        return if (ratio < 1f) {
            bitmap.scale(newWidth, newHeight)
        } else {
            bitmap
        }
    }
}
