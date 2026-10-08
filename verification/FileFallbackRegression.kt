import java.io.File
import java.io.IOException
import fileprobe.FileProbe
import fileprobe.Context
import fileprobe.Uri
import fileprobe.Cursor

fun main() {
    val file = File.createTempFile("file-fallback", ".tmp")
    try {
        val processingError = IOException("metadata")
        FileProbe.metadataFailure = processingError
        val rejected = runCatching { FileProbe.process(file, false) }
        check(rejected.exceptionOrNull() === processingError)
        check(!file.exists() && FileProbe.copies == 0)
        FileProbe.process(file, true)
        check(file.readText() == "original" && FileProbe.copies == 1)
        val copyError = IOException("copy")
        FileProbe.copyFailure = copyError
        check(runCatching { FileProbe.process(file, true) }.exceptionOrNull() === copyError)
        FileProbe.copyFailure = null
        FileProbe.metadataFailure = null
        FileProbe.process(file, false)
        check(file.readText() == "processed")
        val context = Context()
        check(FileProbe.name(context, Uri()) == "photo.png")
        check(context.contentResolver.cursor!!.closed)
        for (cursor in listOf(null, Cursor(hasRow = false), Cursor(hasColumn = false), Cursor(name = null), Cursor(name = "../escape"), Cursor(name = "."))) {
            context.contentResolver.cursor = cursor
            check(FileProbe.name(context, Uri()) == null)
            if (cursor != null) check(cursor.closed)
        }
        context.contentResolver.fail = true
        check(FileProbe.name(context, Uri()) == null)
        check(FileProbe.name(context, Uri("file", "/some/path/keep.jpg")) == "keep.jpg")
        println("File fallback regressions: disabled fallback propagates original error; enabled fallback copies source; copy failure propagates")
        println("File name regressions: minimal projection; cursor closed; empty/missing/query failures; unsafe names; file URI")
    } finally { file.delete() }
}
