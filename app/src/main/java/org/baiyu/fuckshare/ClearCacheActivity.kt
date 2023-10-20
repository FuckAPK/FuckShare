package org.baiyu.fuckshare

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import timber.log.Timber

class ClearCacheActivity : Activity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val message: String
        val files = cacheDir.listFiles()
        if (files == null || files.isEmpty()) {
            message = resources.getString(R.string.clear_cache_empty)
            Timber.d("No cache")
        } else {
            // clear all cache
            val status = Utils.clearCache(this, 0)
            message = if (status) {
                resources.getString(R.string.clear_cache_success)
            } else {
                resources.getString(R.string.clear_cache_failed)
            }
            Timber.d("Cache cleared with result: %b", status)
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }
}