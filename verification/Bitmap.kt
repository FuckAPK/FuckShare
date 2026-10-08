package android.graphics
class Bitmap(val index: Int = 0, val width: Int = 8, val height: Int = 8) {
    var recycled = false
    val isRecycled get() = recycled
    init { live++; peak = maxOf(peak, live) }
    fun recycle() { check(!recycled); recycled = true; live-- }
    fun getPixels(pixels: IntArray, offset: Int, stride: Int, x: Int, y: Int, width: Int, height: Int) {
        check(!recycled)
        val color = 0xFF000000.toInt() or ((index % 256) shl 16) or (index / 256)
        for (row in 0 until height) for (column in 0 until width) pixels[offset + row * stride + column] = color
    }
    enum class Config { ARGB_8888 }
    companion object {
        var live = 0; var peak = 0
        @JvmStatic fun createBitmap(width: Int, height: Int, config: Config) = Bitmap(0, width, height)
    }
}
class Paint
class Canvas(bitmap: Bitmap) {
    fun drawBitmap(bitmap: Bitmap, x: Float, y: Float, paint: Paint?) { error("Unexpected bitmap resize") }
}
object Color {
    @JvmStatic fun red(color: Int) = color ushr 16 and 255
    @JvmStatic fun green(color: Int) = color ushr 8 and 255
    @JvmStatic fun blue(color: Int) = color and 255
}
