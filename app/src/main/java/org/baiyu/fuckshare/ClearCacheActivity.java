package org.baiyu.fuckshare;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class ClearCacheActivity extends Activity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // clear all cache
        boolean status = Utils.clearCache(this, 0);
        String message;
        if (status) {
            message = getResources().getString(R.string.clear_cache_success);
        } else {
            message = getResources().getString(R.string.clear_cache_failed);
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }
}
