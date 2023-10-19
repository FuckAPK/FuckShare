package org.baiyu.fuckshare;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import timber.log.Timber;

public class ClearCacheWorker extends Worker {
    public static final String id = "clearCache";
    private final Context context;

    public ClearCacheWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        final long timeDurationMillis = 1000 * 60 * 30; // 30 mins
        boolean result = Utils.clearCache(context, timeDurationMillis);
        Timber.d("Cache cleared with result: %b", result);
        return result ? Result.success() : Result.failure();
    }
}
