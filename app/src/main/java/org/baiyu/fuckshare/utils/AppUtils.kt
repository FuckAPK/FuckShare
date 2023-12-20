package org.baiyu.fuckshare.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.CountDownTimer
import android.widget.Toast
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import org.baiyu.fuckshare.BuildConfig
import org.baiyu.fuckshare.ClearCacheWorker
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
        return try {
            @Suppress("DEPRECATION")
            context.getSharedPreferences(
                "${BuildConfig.APPLICATION_ID}_preferences",
                Activity.MODE_WORLD_READABLE
            )
        } catch (ignore: SecurityException) {
            context.getSharedPreferences(
                "${BuildConfig.APPLICATION_ID}_preferences",
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
            ?.all { it.deleteRecursively() }
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
        val cn = ComponentName(BuildConfig.APPLICATION_ID, activityName)
        return pm.getComponentEnabledSetting(cn) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
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
        val cn = ComponentName(BuildConfig.APPLICATION_ID, activityName)
        val status =
            if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        pm.setComponentEnabledSetting(cn, status, PackageManager.DONT_KILL_APP)
    }
}
