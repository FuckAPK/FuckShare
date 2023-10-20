package org.baiyu.fuckshare

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import timber.log.Timber

class ClearCacheWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        val timeDurationMillis = 1000L * 60L * 30L // 30 mins
        val result = Utils.clearCache(context, timeDurationMillis)
        Timber.d("Cache cleared with result: %b", result)
        return if (result) Result.success() else Result.failure()
    }

    companion object {
        const val id = "clearCache"
    }
}