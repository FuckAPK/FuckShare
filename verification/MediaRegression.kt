import java.io.File
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever as Media
import com.bumptech.glide.gifencoder.AnimatedGifEncoder as Encoder
import org.lyaaz.fuckshare.Settings
import org.lyaaz.fuckshare.utils.FileUtils

fun reset() {
    check(Bitmap.live == 0)
    Bitmap.peak = 0
    Media.count = 240; Media.duration = 10000L; Media.width = "320"; Media.height = "180"
    Media.requested.clear(); Media.batches.clear(); Media.released = false
    Media.failIndex = -1; Media.throwAtIndex = -1
    Encoder.startOk = true; Encoder.addOk = true; Encoder.finishOk = true; Encoder.throwAtFrame = -1
    Encoder.delays.clear(); Encoder.indices.clear()
}

fun main() {
    val dir = kotlin.io.path.createTempDirectory("gif-regression").toFile()
    try {
        val video = File(dir, "source.mp4").apply { writeBytes(byteArrayOf(1)) }
        val settings = Settings()
        for ((count, duration) in listOf(240 to 10000L, 300 to 10000L, 600 to 10000L,
                35 to 1167L, 123 to 5123L, 120 to 1000L, 1 to 655350L)) {
            reset(); Media.count = count; Media.duration = duration
            check(FileUtils.video2gif(video, settings)?.isFile == true && video.isFile)
            check(Media.requested == (0 until count).toList() && Encoder.indices == Media.requested)
            check(Encoder.delays.sum().toLong() == duration / 10 + if (duration % 10 >= 5) 1 else 0)
            var elapsed = 0L
            Encoder.delays.forEachIndexed { index, delay ->
                elapsed += delay
                val target = (duration / 10 + if (duration % 10 >= 5) 1 else 0) * (index + 1) / count.toDouble()
                check(kotlin.math.abs(elapsed - target) <= 0.51)
            }
            check(Encoder.repeat == 0 && Encoder.quality == 30)
            check(Media.batches.size == (count + 15) / 16)
            check(Bitmap.peak <= 16 && Bitmap.live == 0 && Media.released)
        }
        reset(); Media.count = 35
        check(FileUtils.video2gif(video, settings) != null)
        check(Media.batches == listOf(0 to 16, 16 to 16, 32 to 3))
        for ((width, height) in listOf("1920" to "1080", "3840" to "2160", null to "1080",
                "bad" to "1080", "0" to "1080", Int.MAX_VALUE.toString() to Int.MAX_VALUE.toString())) {
            reset(); Media.width = width; Media.height = height
            check(FileUtils.video2gif(video, settings) != null)
            val w = width?.toIntOrNull()?.takeIf { it > 0 }
            val h = height.toInt()
            val limit = if (w == null) 1 else
                (minOf(16L * 1024 * 1024, Runtime.getRuntime().maxMemory() / 8) / 4 / w / h).coerceIn(1, 16).toInt()
            check(Media.batches.all { it.second <= limit } && Bitmap.peak <= limit)
        }
        settings.videoToGIFQuality = 3
        for (custom in listOf("35", "invalid", "0")) {
            reset(); settings.videoToGIFCustomOption = custom
            check(FileUtils.video2gif(video, settings) != null)
            check(Encoder.quality == if (custom == "35") 35 else 20)
        }
        for (failure in listOf("start", "add", "finish", "short-first", "short-later", "decode-throw", "encode-throw", "delay-limit", "invalid-metadata")) {
            reset()
            when (failure) {
                "start" -> Encoder.startOk = false
                "add" -> Encoder.addOk = false
                "finish" -> Encoder.finishOk = false
                "short-first" -> Media.failIndex = 2
                "short-later" -> Media.failIndex = 18
                "decode-throw" -> Media.throwAtIndex = 16
                "encode-throw" -> Encoder.throwAtFrame = 18
                "delay-limit" -> { Media.count = 1; Media.duration = 655360L }
                "invalid-metadata" -> Media.duration = 0
            }
            check(FileUtils.video2gif(video, settings) == null) { failure }
            check(!File(dir, "source.gif").exists() && video.exists())
            check(Media.released && Bitmap.live == 0) { "Frame leak: $failure" }
        }
        println("GIF regressions: bounded batches and tail; full frame order; cumulative timing; dimensions; short/failed batches and encoder cleanup")
    } finally { dir.deleteRecursively() }
}
