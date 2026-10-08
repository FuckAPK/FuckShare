package org.lyaaz.fuckshare
class Settings {
    var videoToGIFQuality = 1
    var videoToGIFCustomOption = "20"
    enum class VideoToGIFQualityOptions {
        LOW, MEDIUM, HIGH, CUSTOM;
        companion object { fun fromValue(value: Int) = entries.getOrElse(value) { MEDIUM } }
    }
    companion object { const val DEFAULT_VIDEO_TO_GIF_CUSTOM_OPTION = "20" }
}
