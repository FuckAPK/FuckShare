package org.lyaaz.fuckshare.utils

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.service.chooser.ChooserAction
import androidx.annotation.RequiresApi
import org.lyaaz.fuckshare.CopyTextReceiver
import org.lyaaz.fuckshare.R

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
    fun getUrisFromIntent(intent: Intent): List<Uri> {
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = getParcelableExtra(
                    intent,
                    Intent.EXTRA_STREAM,
                    Uri::class.java
                )
                listOfNotNull(uri)
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                getParcelableArrayListExtra<Uri>(
                    intent,
                    Intent.EXTRA_STREAM
                )?.toList() ?: listOf()
            }

            Intent.ACTION_VIEW -> {
                listOfNotNull(intent.data)
            }

            else -> listOf()
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
     * @return The Parcelable ArrayList extra or null if not found.
     */
    inline fun <reified T : Parcelable?> getParcelableArrayListExtra(
        intent: Intent,
        name: String?
    ): ArrayList<T>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(name, T::class.java)
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

    /**
     * Restores a Parcelable array extra from one Intent to another.
     *
     * This function tries to retrieve a Parcelable array extra from the `from` Intent using a key
     * with the specified suffix and then puts it into the `to` Intent using the original key.
     *
     * @param from The source Intent from which to get the Parcelable array extra.
     * @param to The target Intent into which to put the Parcelable array extra.
     * @param key The key used to retrieve and store the Parcelable array extra.
     * @param T The type of Parcelable contained in the array.
     */
    inline fun <reified T : Parcelable> restoreArrayExtras(
        from: Intent,
        to: Intent,
        key: String
    ) {
        getParcelableArrayExtra<T>(from, "$key$EXTRAS_KEY_SUFFIX")?.let {
            to.putExtra(key, it)
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun createOpenLinkAction(context: Context, uris: List<String>): Array<ChooserAction> {
        val options = ActivityOptions
            .makeBasic()
            .setPendingIntentCreatorBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            ).toBundle()
        return uris.filterIndexed { index, _ -> index < 5 }.mapIndexed { index, str ->
            ChooserAction.Builder(
                Icon.createWithResource(context, R.drawable.open_in_browser),
                str,
                PendingIntent.getActivity(
                    context.applicationContext,
                    123 + index,
                    Intent(Intent.ACTION_VIEW, Uri.parse(str)),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT,
                    options
                )
            ).build()
        }.toTypedArray()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun createCopyTextAction(context: Context, text: String): ChooserAction {
        return ChooserAction.Builder(
            Icon.createWithResource(context, R.drawable.qr_code_scanner),
            text,
            PendingIntent.getBroadcast(
                context.applicationContext,
                122,
                Intent(context, CopyTextReceiver::class.java).apply {
                    action = CopyTextReceiver.ACTION
                    putExtra(CopyTextReceiver.EXTRA_TEXT, text)
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT,
            )
        ).build()
    }

    const val EXTRAS_KEY_SUFFIX = "_FS"
}
