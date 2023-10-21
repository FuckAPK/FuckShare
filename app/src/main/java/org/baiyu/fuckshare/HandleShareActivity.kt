package org.baiyu.fuckshare

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ShareCompat.IntentBuilder
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import org.baiyu.fuckshare.exifhelper.ExifHelper
import org.baiyu.fuckshare.exifhelper.ImageFormatException
import org.baiyu.fuckshare.exifhelper.JpegExifHelper
import org.baiyu.fuckshare.exifhelper.PngExifHelper
import org.baiyu.fuckshare.exifhelper.WebpExifHelper
import org.baiyu.fuckshare.filetype.FileType
import org.baiyu.fuckshare.filetype.ImageType
import org.baiyu.fuckshare.filetype.OtherType
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class HandleShareActivity : Activity() {
    @SuppressLint("WorldReadableFiles")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG && Timber.treeCount == 0) {
            Timber.plant(DebugTree())
        }
        val prefs: SharedPreferences = try {
            @Suppress("DEPRECATION")
            getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", MODE_WORLD_READABLE)
        } catch (ignore: SecurityException) {
            getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", MODE_PRIVATE)
        }
        settings = Settings.getInstance(prefs)

        val intent = intent
        if ("text/plain" == intent.type) {
            handleSendText(intent)
        } else {
            val uris = Utils.getUrisFromIntent(intent)!!
            assert(uris.isNotEmpty())
            handleUris(uris)
        }
        finish()
    }

    override fun finish() {
        val clearCacheWorkRequest: PeriodicWorkRequest = PeriodicWorkRequest.Builder(
            ClearCacheWorker::class.java,
            1, TimeUnit.DAYS
        ).setInitialDelay(1, TimeUnit.HOURS).build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                ClearCacheWorker.id,
                ExistingPeriodicWorkPolicy.KEEP,
                clearCacheWorkRequest
            )
        super.finish()
    }

    private fun handleSendText(intent: Intent) {
        val ib = IntentBuilder(this)
        ib.setType(intent.type)
        ib.setText(getIntent().getStringExtra(Intent.EXTRA_TEXT))
        val chooserIntent = ib.createChooserIntent()
        chooserIntent.putExtra(
            Intent.EXTRA_EXCLUDE_COMPONENTS,
            listOf(ComponentName(this, HandleShareActivity::class.java)).toTypedArray()
        )
        startActivity(chooserIntent)
    }

    private fun handleUris(uris: List<Uri?>) {
        val ib = IntentBuilder(this).setType(intent.type)
        uris.filterNotNull()
            .parallelStream()
            .map { refreshUri(it) }
            .forEachOrdered { it?.let { it1 -> ib.addStream(it1) } }

        val chooserIntent = ib.createChooserIntent()
        chooserIntent.putExtra(
            Intent.EXTRA_EXCLUDE_COMPONENTS,
            listOf(ComponentName(this, HandleShareActivity::class.java)).toTypedArray()
        )
        Timber.d("intent: %s", chooserIntent.toString())
        startActivity(chooserIntent)
    }

    private fun refreshUri(uri: Uri): Uri? {
        val originName = Utils.getRealFileName(this, uri)
        var tempFile = File(cacheDir, Utils.randomString)
        return try {
            val magickBytes = ByteArray(16)
            this.contentResolver.openInputStream(uri)!!.buffered().use { uin ->
                Utils.readNBytes(uin, magickBytes)
            }
            var fileType = Utils.getFileType(magickBytes)
            if (fileType is ImageType
                && fileType.isSupportMetadata
                && settings!!.enableRemoveExif()
            ) {
                try {
                    processImgMetadata(tempFile, fileType, uri)
                } catch (e: ImageFormatException) {
                    tempFile.delete()
                    Timber.e("Format error: %s Type: %s", originName, fileType)
                    Toast.makeText(
                        this,
                        "Format error: $originName Type: $fileType",
                        Toast.LENGTH_SHORT
                    ).show()
                    fileType = if (settings!!.enableFallbackToFile()) {
                        copyFileFromUri(uri, tempFile)
                        OtherType.UNKNOWN
                    } else {
                        return null
                    }
                }
            } else {
                copyFileFromUri(uri, tempFile)
            }
            // rename
            val newNameNoExt = getNewNameNoExt(fileType, originName)
            val ext = getExt(fileType, originName)
            val newFullName = Utils.mergeFilename(newNameNoExt, ext)
            var renamed = File(cacheDir, newFullName)
            if (renamed.exists()) {
                val oneTimeCacheDir = File(cacheDir, Utils.randomString)
                oneTimeCacheDir.mkdirs()
                renamed = File(oneTimeCacheDir, newFullName)
            }
            if (tempFile.renameTo(renamed)) {
                tempFile = renamed
            }
            FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", tempFile)
        } catch (e: IOException) {
            Timber.e(e)
            Toast.makeText(this, "Failed to process: $originName", Toast.LENGTH_SHORT).show()
            null
        }
    }

    @Throws(IOException::class)
    private fun copyFileFromUri(uri: Uri, file: File) {
        contentResolver.openInputStream(uri)!!.buffered().use { uin ->
            file.outputStream().buffered().use { fout ->
                uin.copyTo(fout)
            }
        }
    }

    @Throws(IOException::class, ImageFormatException::class)
    private fun processImgMetadata(file: File, imageType: ImageType, uri: Uri) {
        val eh: ExifHelper? = when (imageType) {
            ImageType.JPEG -> JpegExifHelper()
            ImageType.PNG -> PngExifHelper()
            ImageType.WEBP -> WebpExifHelper()
            else -> null
        }
        if (eh == null) {
            Timber.e("unsupported image type: %s", imageType)
        } else {
            contentResolver.openInputStream(uri)!!.buffered().use { uin ->
                file.outputStream().buffered().use { fout ->
                    eh.removeMetadata(uin, fout)
                }
            }
        }
        if (imageType.isSupportMetadata) {
            contentResolver.openInputStream(uri).use { uin ->
                assert(uin != null)
                ExifHelper.writeBackMetadata(
                    ExifInterface(uin!!),
                    ExifInterface(file),
                    settings!!.exifTagsToKeep
                )
            }
        }
    }

    private fun getNewNameNoExt(fileType: FileType?, originName: String?): String {
        return when (fileType) {
            is ImageType -> if (settings!!.enableImageRename()) Utils.randomString else Utils.getFileNameNoExt(
                originName!!
            )

            else -> if (settings!!.enableFileRename()) Utils.randomString else Utils.getFileNameNoExt(
                originName!!
            )
        }
    }

    private fun getExt(fileType: FileType, originName: String?): String? {
        var extension: String? = null
        if (settings!!.enableFileTypeSniff()) {
            extension = fileType.extension
        }
        if (extension == null) {
            extension = Utils.getFileRealExt(originName!!)
        }
        return extension
    }

    companion object {
        private var settings: Settings? = null
    }
}