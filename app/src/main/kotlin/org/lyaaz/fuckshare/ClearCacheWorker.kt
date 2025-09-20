package org.lyaaz.fuckshare

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.lyaaz.fuckshare.utils.AppUtils
import timber.log.Timber

class ClearCacheWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    override fun doWork(): Result {
        AppUtils.timberPlantTree(context)
        val result = AppUtils.clearCache(context, CACHE_DURATION_MILLIS)
        Timber.i("Cache cleared with result: $result")
        return if (result) Result.success() else Result.failure()
    }

    companion object {
        const val id = "clearCache"
        private const val CACHE_DURATION_MILLIS = 30L * 60L * 1000L // 30 minutes
    }
}