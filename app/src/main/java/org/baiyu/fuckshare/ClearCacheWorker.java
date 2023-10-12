package org.baiyu.fuckshare;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ClearCacheWorker extends Worker{
    private final Context context;
    public static final String id = "clearCache";

    public ClearCacheWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        return Utils.clearCache(context) ? Result.success() : Result.failure();
    }
}
