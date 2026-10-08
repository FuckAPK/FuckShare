package com.bumptech.glide.gifencoder
import java.io.OutputStream
import android.graphics.Bitmap
import kotlin.math.roundToInt
class AnimatedGifEncoder {
    private var delayCs = 0
    fun start(output: OutputStream): Boolean { output.write(1); return startOk }
    fun setDelay(value: Int) { delayCs = (value / 10f).roundToInt(); check(delayCs in 0..65535) }
    fun setRepeat(value: Int) { repeat = value }
    fun setQuality(value: Int) { quality = value }
    fun addFrame(bitmap: Bitmap): Boolean {
        check(!bitmap.recycled)
        if (bitmap.index == throwAtFrame) throw IllegalStateException("Encoder failed")
        delays.add(delayCs); indices.add(bitmap.index)
        return addOk
    }
    fun finish(): Boolean = finishOk
    companion object {
        var startOk = true; var addOk = true; var finishOk = true; var throwAtFrame = -1
        var repeat = -1; var quality = 0
        val delays = mutableListOf<Int>()
        val indices = mutableListOf<Int>()
    }
}
