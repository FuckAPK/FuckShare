import java.io.File
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadataNode
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever as Media
import org.lyaaz.fuckshare.Settings
import org.lyaaz.fuckshare.utils.FileUtils

fun main() {
    val dir = kotlin.io.path.createTempDirectory("real-gif-regression").toFile()
    try {
        val video = File(dir, "source.mp4").apply { writeBytes(byteArrayOf(1)) }
        for ((count, duration) in listOf(240 to 10000L, 300 to 10000L, 600 to 10000L, 123 to 5123L)) {
            Media.count = count; Media.duration = duration; Media.batches.clear(); Media.requested.clear()
            val gif = FileUtils.video2gif(video, Settings()) ?: error("Encoding failed")
            ImageIO.createImageInputStream(gif).use { input ->
                val reader = ImageIO.getImageReadersByFormatName("gif").next()
                try {
                    reader.input = input
                    check(reader.getNumImages(true) == count)
                    var totalCs = 0L
                    for (index in 0 until count) {
                        val tree = reader.getImageMetadata(index)
                            .getAsTree("javax_imageio_gif_image_1.0") as IIOMetadataNode
                        val control = tree.getElementsByTagName("GraphicControlExtension").item(0) as IIOMetadataNode
                        totalCs += control.getAttribute("delayTime").toLong()
                    }
                    check(totalCs * 10 == (duration / 10 + if (duration % 10 >= 5) 1 else 0) * 10)
                    check(reader.read(0).width == 8 && reader.read(count - 1).height == 8)
                    println("Real GIF: $count frames / source ${duration}ms -> ${totalCs * 10}ms stored delays")
                } finally { reader.dispose() }
            }
            check(Bitmap.live == 0 && Media.released)
        }
    } finally { dir.deleteRecursively() }
}
