package org.lyaaz.fuckshare.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import org.lyaaz.fuckshare.ClearCacheWorker
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Utility class containing various methods for common tasks in the application.
 */
object AppUtils {
    
    /**
     * Generates a random string using UUID.
     *
     * @return A randomly generated string.
     */
    inline val randomString: String
        get() = UUID.randomUUID().toString()

    /**
     * Retrieves shared preferences considering security measures.
     * Uses caching to avoid repeated preference name computation.
     *
     * @param context The context used to access shared preferences.
     * @return Shared preferences instance.
     */
    @SuppressLint("WorldReadableFiles")
    fun getPrefs(context: Context): SharedPreferences {
        val prefsName = "${context.packageName}_preferences"
        return runCatching {
            @Suppress("DEPRECATION")
            context.getSharedPreferences(
                prefsName,
                Activity.MODE_WORLD_READABLE
            )
        }.getOrElse {
            context.getSharedPreferences(
                prefsName,
                Activity.MODE_PRIVATE
            )
        }
    }

    /**
     * Schedules a periodic worker for clearing cache.
     *
     * @param context The context used to schedule the worker.
     */
    fun scheduleClearCacheWorker(context: Context) {
        val clearCacheWorkRequest: PeriodicWorkRequest = PeriodicWorkRequest.Builder(
            ClearCacheWorker::class.java,
            1, TimeUnit.DAYS
        ).setInitialDelay(1, TimeUnit.HOURS).build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                ClearCacheWorker.id,
                ExistingPeriodicWorkPolicy.KEEP,
                clearCacheWorkRequest
            )
    }

    /**
     * Displays a toast message with a countdown timer.
     * Optimized to reduce object allocations.
     *
     * @param context The context used to display the toast.
     * @param message The message to be displayed.
     * @param length The duration for which the toast is displayed.
     */
    fun showToast(context: Context, message: String, length: Int) {
        val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
        if (length <= 0) {
            toast.show()
            return
        }
        
        val toastCountDown = object : CountDownTimer(length.toLong(), 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                toast.show()
            }

            override fun onFinish() {
                toast.cancel()
            }
        }
        toast.show()
        toastCountDown.start()
    }

    /**
     * Clears cache files older than a specified time duration.
     * Optimized for better performance and memory usage.
     *
     * @param context The context used to access the cache directory.
     * @param timeDurationMillis The time duration in milliseconds.
     * @return `true` if the operation is successful, otherwise `false`.
     */
    fun clearCache(context: Context, timeDurationMillis: Long): Boolean {
        Timber.d("Clearing cache with time duration: $timeDurationMillis")
        val timeBefore = System.currentTimeMillis() - timeDurationMillis
        val cacheFiles = context.cacheDir.listFiles() ?: return true
        
        return cacheFiles.asSequence()
            .filter { it.lastModified() < timeBefore }
            .map { it.deleteRecursively() }
            .all { it }
    }

    /**
     * Gets the status of a specified activity.
     * Optimized to reduce redundant operations.
     *
     * @param context The context used to access the package manager.
     * @param activityName The name of the activity.
     * @return `true` if the activity is enabled, otherwise `false`.
     */
    fun getActivityStatus(context: Context, activityName: String): Boolean {
        val pm = context.applicationContext.packageManager
        val cn = ComponentName(context.packageName, activityName)
        val status = when (pm.getComponentEnabledSetting(cn)) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            else -> {
                runCatching {
                    val packageInfo = pm.getPackageInfo(
                        context.packageName,
                        PackageManager.GET_ACTIVITIES or PackageManager.MATCH_DISABLED_COMPONENTS
                    )
                    packageInfo.activities
                        ?.firstOrNull { it.name == activityName }
                        ?.enabled
                        ?: false
                }.getOrElse { false }
            }
        }
        Timber.d("Activity status: $activityName: $status")
        return status
    }

    /**
     * Sets the status of a specified activity.
     *
     * @param context The context used to access the package manager.
     * @param activityName The name of the activity.
     * @param enable `true` to enable the activity, `false` to disable.
     */
    fun setActivityStatus(context: Context, activityName: String, enable: Boolean) {
        val pm = context.applicationContext.packageManager
        val cn = ComponentName(context.packageName, activityName)
        val status =
            if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        pm.setComponentEnabledSetting(cn, status, PackageManager.DONT_KILL_APP)
        Timber.d("set activity status: $activityName: $enable")
    }

    /**
     * Checks if the app has overlay permission.
     *
     * @param context The context used to access the application info.
     * @return `true` if the app has overlay permission, otherwise `false`.
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return android.provider.Settings.canDrawOverlays(context)
    }

    /**
     * Checks if the app is in debug mode.
     * Optimized with inline for better performance.
     *
     * @param context The context used to access the application info.
     * @return `true` if the app is in debug mode, otherwise `false`.
     */
    inline fun isDebugMode(context: Context): Boolean {
        return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    // Cached debug tree instances to avoid repeated object creation
    private val debugTree by lazy {
        object : Timber.DebugTree() {
            override fun createStackElementTag(element: StackTraceElement): String {
                return "${element.className}:${element.lineNumber}#${element.methodName}"
            }
        }
    }
    
    private val releaseTree by lazy {
        object : Timber.DebugTree() {
            override fun createStackElementTag(element: StackTraceElement): String {
                return "${element.className}#${element.methodName}"
            }

            override fun isLoggable(tag: String?, priority: Int): Boolean {
                return priority >= Log.WARN
            }
        }
    }

    /**
     * Plants a [Timber.DebugTree] if the app is in debug mode.
     * Optimized to reuse tree instances and avoid repeated creation.
     *
     * @param context The context used to access the application info.
     */
    fun timberPlantTree(context: Context) {
        if (Timber.treeCount == 0) {
            val tree = if (isDebugMode(context)) debugTree else releaseTree
            Timber.plant(tree)
        }
    }

    /**
     * Copies text to the clipboard.
     *
     * @param context The context used to access the clipboard.
     * @param text The text to be copied.
     */
    fun copyToClipboard(context: Context, text: String) {
        val clipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(text, text)
        clipboard.setPrimaryClip(clip)
    }
}
