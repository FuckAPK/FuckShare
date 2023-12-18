package org.baiyu.fuckshare

import android.content.SharedPreferences

class Settings private constructor(private val prefs: SharedPreferences) {
    fun enableHook(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_HOOK, false)
    }

    fun enableForceForwardHook(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_FORCE_FORWARD_HOOK, false)
    }

    fun enableForceContentHook(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_FORCE_CONTENT_HOOK, false)
    }

    fun enableForceDocumentHook(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_FORCE_DOCUMENT_HOOK, false)
    }

    fun enableForcePickerHook(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_FORCE_PICKER_HOOK, false)
    }

    fun enableRemoveExif(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_REMOVE_EXIF, true)
    }

    val exifTagsToKeep: Set<String>
        get() {
            val rawPref = prefs.getString(
                PREF_EXIF_TAGS_TO_KEEP,
                "Orientation, Gamma, ColorSpace, XResolution, YResolution, ResolutionUnit"
            )
            return rawPref?.split("[, ]+".toRegex())?.asSequence()
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.toSet() ?: setOf()
        }

    fun enableImageRename(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_IMAGE_RENAME, true)
    }

    fun enableFileRename(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_FILE_RENAME, false)
    }

    fun enableFileTypeSniff(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_FILE_TYPE_SNIFF, false)
    }

    fun enableFallbackToFile(): Boolean {
        return prefs.getBoolean(PREF_ENABLE_FALLBACK_TO_FILE, true)
    }

    val toastTime: Int
        get() {
            return prefs.getString(PREF_TOAST_TIME, null)
                ?.takeIf { it.isNotBlank() }
                ?.toInt() ?: 500
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
        const val PREF_ENABLE_FALLBACK_TO_FILE = "enable_fallback_to_file"
        const val PREF_TOAST_TIME = "toast_time"

        @Volatile
        private var INSTANCE: Settings? = null
        fun getInstance(prefs: SharedPreferences): Settings {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Settings(prefs).also { INSTANCE = it }
            }
        }
    }
}