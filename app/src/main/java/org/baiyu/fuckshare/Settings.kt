package org.baiyu.fuckshare

import android.content.SharedPreferences

class Settings private constructor(private val prefs: SharedPreferences) {

    val enableRemoveExif: Boolean
        get() {
            return prefs.getBoolean(PREF_ENABLE_REMOVE_EXIF, DEFAULT_ENABLE_REMOVE_EXIF)
        }
    val enableFallbackToFile: Boolean
        get() {
            return prefs.getBoolean(PREF_ENABLE_FALLBACK_TO_FILE, DEFAULT_ENABLE_FALLBACK_TO_FILE)
        }
    val exifTagsToKeep: Set<String>
        get() {
            return prefs.getString(PREF_EXIF_TAGS_TO_KEEP, null)?.let { pref ->
                pref.takeIf { it.isNotBlank() }
                    ?.split("[,\\s]+".toRegex())
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.toSet()
                    ?: setOf()
            } ?: DEFAULT_EXIF_TAGS_TO_KEEP
        }

    val enableFileTypeSniff: Boolean
        get() {
            return prefs.getBoolean(PREF_ENABLE_FILE_TYPE_SNIFF, DEFAULT_ENABLE_FILE_TYPE_SNIFF)
        }
    val enableImageRename: Boolean
        get() {
            return prefs.getBoolean(PREF_ENABLE_IMAGE_RENAME, DEFAULT_ENABLE_IMAGE_RENAME)
        }
    val enableVideoRename: Boolean
        get() {
            return prefs.getBoolean(PREF_ENABLE_VIDEO_RENAME, DEFAULT_ENABLE_VIDEO_RENAME)
        }
    val enableFileRename: Boolean
        get() {
            return prefs.getBoolean(PREF_ENABLE_FILE_RENAME, DEFAULT_ENABLE_FILE_RENAME)
        }

    val enableVideoToGIF: Boolean
        get() {
            return prefs.getBoolean(PREF_ENABLE_VIDEO_TO_GIF, DEFAULT_ENABLE_VIDEO_TO_GIF)
        }
    val videoToGIFForceWithAudio: Boolean
        get() {
            return prefs.getBoolean(
                PREF_VIDEO_TO_GIF_FORCE_WITH_AUDIO,
                DEFAULT_VIDEO_TO_GIF_FORCE_WITH_AUDIO
            )
        }
    val videoToGifSizeKB: Int
        get() {
            return prefs.getInt(PREF_VIDEO_TO_GIF_SIZE_KB, DEFAULT_VIDEO_TO_GIF_SIZE_KB)
        }
    val videoToGIFQuality: Int
        get() {
            return prefs.getInt(PREF_VIDEO_TO_GIF_QUALITY, DEFAULT_VIDEO_TO_GIF_QUALITY.value)
        }
    val videoToGIFCustomOption: String
        get() {
            return prefs.getString(
                PREF_VIDEO_TO_GIF_CUSTOM_OPTION,
                DEFAULT_VIDEO_TO_GIF_CUSTOM_OPTION
            )
                ?: DEFAULT_VIDEO_TO_GIF_CUSTOM_OPTION
        }

    val enableHook: Boolean
        get() {
            return prefs.getBoolean(PREF_ENABLE_HOOK, DEFAULT_ENABLE_HOOK)
        }
    val enableForceForwardHook: Boolean
        get() {
            return prefs.getBoolean(
                PREF_ENABLE_FORCE_FORWARD_HOOK,
                DEFAULT_ENABLE_FORCE_FORWARD_HOOK
            )
        }
    val enableForceContentHook: Boolean
        get() {
            return prefs.getBoolean(
                PREF_ENABLE_FORCE_CONTENT_HOOK,
                DEFAULT_ENABLE_FORCE_CONTENT_HOOK
            )
        }
    val enableForceDocumentHook: Boolean
        get() {
            return prefs.getBoolean(
                PREF_ENABLE_FORCE_DOCUMENT_HOOK,
                DEFAULT_ENABLE_FORCE_DOCUMENT_HOOK
            )
        }
    val enableForcePickerHook: Boolean
        get() {
            return prefs.getBoolean(PREF_ENABLE_FORCE_PICKER_HOOK, DEFAULT_ENABLE_FORCE_PICKER_HOOK)
        }

    val toastTimeMS: Int
        get() {
            return prefs.getInt(PREF_TOAST_TIME_MS, DEFAULT_TOAST_TIME_MS)
        }
    val enableTextToLinkAction: Boolean
        get() {
            return prefs.getBoolean(
                PREF_ENABLE_TEXT_TO_LINK_ACTION,
                DEFAULT_ENABLE_TEXT_TO_LINK_ACTION
            )
        }

    enum class VideoToGIFQualityOptions(val value: Int) {
        LOW(0),
        HIGH(1),
        CUSTOM(2);

        companion object {
            private val map = entries.associateBy { it.value }
            fun fromValue(value: Int) = map[value] ?: DEFAULT_VIDEO_TO_GIF_QUALITY
        }
    }

    companion object {
        // Prefs
        const val PREF_ENABLE_REMOVE_EXIF = "enable_remove_exif"
        const val PREF_ENABLE_FALLBACK_TO_FILE = "enable_fallback_to_file"
        const val PREF_EXIF_TAGS_TO_KEEP = "exif_tags_to_keep"

        const val PREF_ENABLE_FILE_TYPE_SNIFF = "enable_file_type_sniff"
        const val PREF_ENABLE_IMAGE_RENAME = "enable_image_rename"
        const val PREF_ENABLE_VIDEO_RENAME = "enable_video_rename"
        const val PREF_ENABLE_FILE_RENAME = "enable_file_rename"

        const val PREF_ENABLE_VIDEO_TO_GIF = "enable_video_to_gif"
        const val PREF_VIDEO_TO_GIF_FORCE_WITH_AUDIO = "video_to_gif_ignore_audio"
        const val PREF_VIDEO_TO_GIF_SIZE_KB = "video_to_gif_size_kB"
        const val PREF_VIDEO_TO_GIF_QUALITY = "video_to_gif_quality"
        const val PREF_VIDEO_TO_GIF_CUSTOM_OPTION = "video_to_gif_custom_option"

        const val PREF_ENABLE_HOOK = "enable_hook"
        const val PREF_ENABLE_FORCE_FORWARD_HOOK = "enable_force_forward_hook"
        const val PREF_ENABLE_FORCE_CONTENT_HOOK = "enable_force_content_hook"
        const val PREF_ENABLE_FORCE_DOCUMENT_HOOK = "enable_force_document_hook"
        const val PREF_ENABLE_FORCE_PICKER_HOOK = "enable_force_picker_hook"

        const val PREF_TOAST_TIME_MS = "toast_time"
        const val PREF_ENABLE_TEXT_TO_LINK_ACTION = "enable_text_to_link_action"

        // Defaults
        const val DEFAULT_ENABLE_REMOVE_EXIF = true
        const val DEFAULT_ENABLE_FALLBACK_TO_FILE = true
        val DEFAULT_EXIF_TAGS_TO_KEEP = setOf(
            "Orientation",
            "Gamma",
            "ColorSpace",
            "XResolution",
            "YResolution",
            "ResolutionUnit"
        )
        const val DEFAULT_ENABLE_FILE_TYPE_SNIFF = true
        const val DEFAULT_ENABLE_IMAGE_RENAME = true
        const val DEFAULT_ENABLE_VIDEO_RENAME = true
        const val DEFAULT_ENABLE_FILE_RENAME = false

        const val DEFAULT_ENABLE_VIDEO_TO_GIF = false
        const val DEFAULT_VIDEO_TO_GIF_FORCE_WITH_AUDIO = false
        const val DEFAULT_VIDEO_TO_GIF_SIZE_KB = 1024 * 10
        val DEFAULT_VIDEO_TO_GIF_QUALITY = VideoToGIFQualityOptions.LOW
        const val DEFAULT_VIDEO_TO_GIF_CUSTOM_OPTION = "-i \$input \$output"

        const val DEFAULT_ENABLE_HOOK = false
        const val DEFAULT_ENABLE_FORCE_FORWARD_HOOK = false
        const val DEFAULT_ENABLE_FORCE_CONTENT_HOOK = false
        const val DEFAULT_ENABLE_FORCE_DOCUMENT_HOOK = false
        const val DEFAULT_ENABLE_FORCE_PICKER_HOOK = false

        const val DEFAULT_TOAST_TIME_MS = 500
        const val DEFAULT_ENABLE_TEXT_TO_LINK_ACTION = false

        @Volatile
        private var INSTANCE: Settings? = null
        fun getInstance(prefs: SharedPreferences): Settings {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Settings(prefs).also { INSTANCE = it }
            }
        }
    }
}