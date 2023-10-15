package org.baiyu.fuckshare;

import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.widget.Toast;

import androidx.annotation.Nullable;

public class ClearCacheActivity extends Activity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    @Override
    protected void onStart() {
        super.onStart();
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

    @Override
    protected void onResume() {
        finish();
        super.onResume();
    }
}
