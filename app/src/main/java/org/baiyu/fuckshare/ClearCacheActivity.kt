package org.baiyu.fuckshare

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import timber.log.Timber

class ClearCacheActivity : Activity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val files = cacheDir.listFiles()
        if (files == null || files.isEmpty()) {
            Timber.d("No cache")
            resources.getString(R.string.clear_cache_empty)
        } else {
            // clear all cache
            val status = Utils.clearCache(this, 0)
            Timber.d("Cache cleared with result: %b", status)
            if (status) {
                resources.getString(R.string.clear_cache_success)
            } else {
                resources.getString(R.string.clear_cache_failed)
            }
        }.also {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}