package org.baiyu.fuckshare;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

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
        return Utils.clearCache(context, timeDurationMillis) ? Result.success() : Result.failure();
    }
}
