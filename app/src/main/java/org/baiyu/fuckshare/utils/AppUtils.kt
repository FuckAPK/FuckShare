package org.baiyu.fuckshare.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.CountDownTimer
import android.widget.Toast
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import org.baiyu.fuckshare.ClearCacheWorker
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
    val randomString: String
        get() = UUID.randomUUID().toString()

    /**
     * Retrieves shared preferences considering security measures.
     *
     * @param context The context used to access shared preferences.
     * @return Shared preferences instance.
     */
    @SuppressLint("WorldReadableFiles")
    fun getPrefs(context: Context): SharedPreferences {
        val prefsName = "${context.packageName}_preferences"
        return try {
            @Suppress("DEPRECATION")
            context.getSharedPreferences(
                prefsName,
                Activity.MODE_WORLD_READABLE
            )
        } catch (ignore: SecurityException) {
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
     *
     * @param context The context used to display the toast.
     * @param message The message to be displayed.
     * @param length The duration for which the toast is displayed.
     */
    fun showToast(context: Context, message: String, length: Int) {
        val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
        val toastCountDown: CountDownTimer =
            object : CountDownTimer(length.toLong(), 1000L) {
                override fun onTick(millisUntilFinished: Long) {
                    toast?.show()
                }

                override fun onFinish() {
                    toast?.cancel()
                }
            }
        toast.show()
        toastCountDown.start()
    }

    /**
     * Clears cache files older than a specified time duration.
     *
     * @param context The context used to access the cache directory.
     * @param timeDurationMillis The time duration in milliseconds.
     * @return `true` if the operation is successful, otherwise `false`.
     */
    fun clearCache(context: Context, timeDurationMillis: Long): Boolean {
        val timeBefore = System.currentTimeMillis() - timeDurationMillis
        return context.cacheDir.listFiles()?.asSequence()
            ?.filter { it.lastModified() < timeBefore }
            ?.map { it.deleteRecursively() }
            ?.findLast { !it }
            ?: true
    }

    /**
     * Gets the status of a specified activity.
     *
     * @param context The context used to access the package manager.
     * @param activityName The name of the activity.
     * @return `true` if the activity is enabled, otherwise `false`.
     */
    fun getActivityStatus(context: Context, activityName: String): Boolean {
        val pm = context.applicationContext.packageManager
        val cn = ComponentName(context.packageName, activityName)
        return when (pm.getComponentEnabledSetting(cn)) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            else -> {
                val packageInfo = pm.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_ACTIVITIES
                            or PackageManager.MATCH_DISABLED_COMPONENTS
                )
                packageInfo.activities?.asSequence()
                    ?.find { it.name == activityName }
                    ?.enabled
                    ?: false
            }
        }
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
    }

    /**
     * Checks if the overlay permission is granted.
     *
     * @param context The context used to access the package manager.
     * @return `true` if the overlay permission is granted, otherwise `false`.
     */
    fun needOverlayPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                && !android.provider.Settings.canDrawOverlays(context)
    }

    /**
     * Checks if the app is in debug mode.
     *
     * @param context The context used to access the application info.
     * @return `true` if the app is in debug mode, otherwise `false`.
     */
    fun isDebugMode(context: Context): Boolean {
        return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    /**
     * Plants a [Timber.DebugTree] if the app is in debug mode.
     *
     * @param context The context used to access the application info.
     */
    fun timberPlantTree(context: Context) {
        if (Timber.treeCount == 0) {
            if (isDebugMode(context)) {
                Timber.plant(Timber.DebugTree())
            }
        }
    }
}
