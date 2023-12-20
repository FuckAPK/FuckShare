package org.baiyu.fuckshare.utils

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable

/**
 * Utility class for working with Intents and extracting URIs from them.
 */
object IntentUtils {

    /**
     * Extracts URIs from the provided Intent based on the action.
     *
     * @param intent The Intent to extract URIs from.
     * @return A list of URIs or null if the action is not supported.
     */
    fun getUrisFromIntent(intent: Intent): List<Uri?>? {
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = getParcelableExtra(
                    intent,
                    Intent.EXTRA_STREAM,
                    Uri::class.java
                )!!
                listOf(uri)
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                getParcelableArrayListExtra(
                    intent,
                    Intent.EXTRA_STREAM,
                    Uri::class.java
                )
            }

            Intent.ACTION_VIEW -> {
                listOf(intent.data)
            }

            else -> null
        }
    }

    /**
     * Gets a Parcelable extra from the Intent.
     *
     * @param intent The Intent to retrieve the extra from.
     * @param name The name of the extra.
     * @param clazz The class type of the Parcelable.
     * @return The Parcelable extra or null if not found.
     */
    fun <T : Parcelable?> getParcelableExtra(
        intent: Intent,
        name: String?,
        clazz: Class<T>
    ): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(name, clazz)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(name)
        }
    }

    /**
     * Gets a Parcelable ArrayList extra from the Intent.
     *
     * @param intent The Intent to retrieve the extra from.
     * @param name The name of the extra.
     * @param clazz The class type of the Parcelable.
     * @return The Parcelable ArrayList extra or null if not found.
     */
    fun <T : Parcelable?> getParcelableArrayListExtra(
        intent: Intent,
        name: String?,
        clazz: Class<T>
    ): ArrayList<T>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(name, clazz)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(name)
        }
    }

    /**
     * Gets a Parcelable Array extra from the Intent.
     *
     * @param intent The Intent to retrieve the extra from.
     * @param name The name of the extra.
     * @return The Parcelable Array extra or null if not found.
     */
    inline fun <reified T : Parcelable?> getParcelableArrayExtra(
        intent: Intent,
        name: String?
    ): Array<T>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayExtra(name, Parcelable::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayExtra(name)
        }?.map { it as T }?.toTypedArray()
    }
}
