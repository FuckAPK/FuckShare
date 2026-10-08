package fileprobe

import java.io.File
import java.io.IOException
import timber.log.Timber

class Context(val contentResolver: Resolver = Resolver())
class Uri(val scheme: String = "content", val path: String? = null)
object ContentResolver { const val SCHEME_FILE = "file"; const val SCHEME_CONTENT = "content" }
object OpenableColumns { const val DISPLAY_NAME = "display_name" }
class Cursor(var name: String? = "photo.png", var hasRow: Boolean = true, var hasColumn: Boolean = true) : java.io.Closeable {
    var closed = false
    fun getColumnIndex(name: String) = if (hasColumn) 0 else -1
    fun moveToFirst() = hasRow
    fun getString(index: Int) = name
    override fun close() { closed = true }
}
class Resolver {
    var cursor: Cursor? = Cursor()
    var fail = false
    fun query(uri: Uri, projection: Array<String>, selection: String?, args: Array<String>?, order: String?): Cursor? {
        check(projection.contentEquals(arrayOf(OpenableColumns.DISPLAY_NAME)))
        if (fail) throw IOException("provider has no display name query")
        return cursor
    }
}
class ImageType
class Settings(var enableFallbackToFile: Boolean)
object FileProbe {
    var metadataFailure: Exception? = null
    var copyFailure: Exception? = null
    var copies = 0
    fun process(file: File, fallback: Boolean) = processImage(Context(), Settings(fallback), file, ImageType(), Uri())
    fun name(context: Context, uri: Uri) = getRealFileName(context, uri)
    private fun processImgMetadata(context: Context, settings: Settings, file: File, type: ImageType, uri: Uri) {
        file.writeText("partial")
        metadataFailure?.let { throw it }
        file.writeText("processed")
    }
    private fun copyFileFromUri(context: Context, uri: Uri, file: File) {
        copies++
        copyFailure?.let { throw it }
        file.writeText("original")
    }
    // PRODUCTION_PROCESS_IMAGE
    // PRODUCTION_FILE_NAME
}
