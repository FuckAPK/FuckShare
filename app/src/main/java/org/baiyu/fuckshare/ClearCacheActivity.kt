package org.baiyu.fuckshare;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.File;

import timber.log.Timber;

public class ClearCacheActivity extends Activity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String message;

        File[] files = getCacheDir().listFiles();
        if (files == null || files.length == 0) {
            message = getResources().getString(R.string.clear_cache_empty);
            Timber.d("No cache");
        } else {
            // clear all cache
            boolean status = Utils.clearCache(this, 0);
            if (status) {
                message = getResources().getString(R.string.clear_cache_success);
            } else {
                message = getResources().getString(R.string.clear_cache_failed);
            }
            Timber.d("Cache cleared with result: %b", status);
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }
}
