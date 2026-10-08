package android.media
import android.graphics.Bitmap
class MediaMetadataRetriever {
    fun setDataSource(path: String) {}
    fun extractMetadata(key: Int): String? = when (key) {
        METADATA_KEY_VIDEO_FRAME_COUNT -> count.toString()
        METADATA_KEY_DURATION -> duration.toString()
        METADATA_KEY_VIDEO_WIDTH -> width
        METADATA_KEY_VIDEO_HEIGHT -> height
        else -> error("Unexpected metadata key: $key")
    }
    fun getFramesAtIndex(index: Int, size: Int): List<Bitmap> {
        batches.add(index to size)
        check(size in 1..16)
        if (index == throwAtIndex) throw IllegalStateException("Decoder failed")
        val returned = if (failIndex in index until index + size) failIndex - index else size
        return (index until index + returned).map { requested.add(it); Bitmap(it) }
    }
    fun release() { released = true }
    companion object {
        const val METADATA_KEY_VIDEO_FRAME_COUNT = 1
        const val METADATA_KEY_DURATION = 2
        const val METADATA_KEY_VIDEO_WIDTH = 3
        const val METADATA_KEY_VIDEO_HEIGHT = 4
        var count = 240
        var duration = 10000L
        var width: String? = "320"
        var height: String? = "180"
        var failIndex = -1
        var throwAtIndex = -1
        var released = false
        val requested = mutableListOf<Int>()
        val batches = mutableListOf<Pair<Int, Int>>()
    }
}
