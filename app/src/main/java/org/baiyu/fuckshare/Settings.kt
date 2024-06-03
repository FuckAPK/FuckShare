package org.baiyu.fuckshare

import android.content.SharedPreferences

class Settings private constructor(private val prefs: SharedPreferences) {
    fun enableHook(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_HOOK, DEFAULT_ENABLE_HOOK)
    }

    fun enableForceForwardHook(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_FORCE_FORWARD_HOOK, DEFAULT_ENABLE_FORCE_FORWARD_HOOK)
    }

    fun enableForceContentHook(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_FORCE_CONTENT_HOOK, DEFAULT_ENABLE_FORCE_CONTENT_HOOK)
    }

    fun enableForceDocumentHook(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_FORCE_DOCUMENT_HOOK, DEFAULT_ENABLE_FORCE_DOCUMENT_HOOK)
    }

    fun enableForcePickerHook(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_FORCE_PICKER_HOOK, DEFAULT_ENABLE_FORCE_PICKER_HOOK)
    }

    fun enableRemoveExif(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_REMOVE_EXIF, DEFAULT_ENABLE_REMOVE_EXIF)
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

    fun enableImageRename(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_IMAGE_RENAME, DEFAULT_ENABLE_IMAGE_RENAME)
    }

    fun enableFileRename(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_FILE_RENAME, DEFAULT_ENABLE_FILE_RENAME)
    }

    fun enableFileTypeSniff(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_FILE_TYPE_SNIFF, DEFAULT_ENABLE_FILE_TYPE_SNIFF)
    }

    fun enableArchiveTypeSniff(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_ARCHIVE_TYPE_SNIFF, DEFAULT_ENABLE_ARCHIVE_TYPE_SNIFF)
    }

    fun enableFallbackToFile(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_FALLBACK_TO_FILE, DEFAULT_ENABLE_FALLBACK_TO_FILE)
    }

    val toastTime: Int
        get() {
            return prefs.getInt(PREF_TOAST_TIME, DEFAULT_TOAST_TIME)
        }

    companion object {
        const val PREF_ENABLE_HOOK = "enable_hook"
        const val PREF_ENABLE_FORCE_FORWARD_HOOK = "enable_force_forward_hook"
        const val PREF_ENABLE_FORCE_CONTENT_HOOK = "enable_force_content_hook"
        const val PREF_ENABLE_FORCE_DOCUMENT_HOOK = "enable_force_document_hook"
        const val PREF_ENABLE_FORCE_PICKER_HOOK = "enable_force_picker_hook"
        const val PREF_ENABLE_REMOVE_EXIF = "enable_remove_exif"
        const val PREF_EXIF_TAGS_TO_KEEP = "exif_tags_to_keep"
        const val PREF_ENABLE_IMAGE_RENAME = "enable_image_rename"
        const val PREF_ENABLE_FILE_RENAME = "enable_file_rename"
        const val PREF_ENABLE_FILE_TYPE_SNIFF = "enable_file_type_sniff"
        const val PREF_ENABLE_ARCHIVE_TYPE_SNIFF = "enable_archive_type_sniff"
        const val PREF_ENABLE_FALLBACK_TO_FILE = "enable_fallback_to_file"
        const val PREF_TOAST_TIME = "toast_time"

        const val DEFAULT_ENABLE_HOOK = false
        const val DEFAULT_ENABLE_FORCE_FORWARD_HOOK = false
        const val DEFAULT_ENABLE_FORCE_CONTENT_HOOK = false
        const val DEFAULT_ENABLE_FORCE_DOCUMENT_HOOK = false
        const val DEFAULT_ENABLE_FORCE_PICKER_HOOK = false
        const val DEFAULT_ENABLE_REMOVE_EXIF = true
        val DEFAULT_EXIF_TAGS_TO_KEEP = setOf(
            "Orientation",
            "Gamma",
            "ColorSpace",
            "XResolution",
            "YResolution",
            "ResolutionUnit"
        )
        const val DEFAULT_ENABLE_IMAGE_RENAME = true
        const val DEFAULT_ENABLE_FILE_RENAME = false
        const val DEFAULT_ENABLE_FILE_TYPE_SNIFF = true
        const val DEFAULT_ENABLE_ARCHIVE_TYPE_SNIFF = false
        const val DEFAULT_ENABLE_FALLBACK_TO_FILE = true
        const val DEFAULT_TOAST_TIME = 500


        @Volatile
        private var INSTANCE: Settings? = null
        fun getInstance(prefs: SharedPreferences): Settings {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Settings(prefs).also { INSTANCE = it }
            }
        }
    }
}