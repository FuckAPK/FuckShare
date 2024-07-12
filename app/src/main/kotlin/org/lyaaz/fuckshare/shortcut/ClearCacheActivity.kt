package org.lyaaz.fuckshare.shortcut

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import org.lyaaz.fuckshare.R
import org.lyaaz.fuckshare.utils.AppUtils
import timber.log.Timber

class ClearCacheActivity : Activity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUtils.timberPlantTree(this)
        val files = cacheDir.listFiles()
        val message = if (files?.isEmpty() == true) {
            Timber.d("No cache")
            resources.getString(R.string.clear_cache_empty)
        } else {
            // clear all cache
            val status = AppUtils.clearCache(this, 0)
            Timber.d("Cache cleared with result: $status")
            if (status) {
                resources.getString(R.string.clear_cache_success)
            } else {
                resources.getString(R.string.clear_cache_failed)
            }
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }
}