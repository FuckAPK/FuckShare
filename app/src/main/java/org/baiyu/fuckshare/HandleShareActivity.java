package org.baiyu.fuckshare;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Parcelable;
import android.util.Log;

import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.baiyu.fuckshare.exifhelper.ExifHelper;
import org.baiyu.fuckshare.exifhelper.jpegExifHelper;
import org.baiyu.fuckshare.exifhelper.pngExifHelper;
import org.baiyu.fuckshare.exifhelper.webpExifHelper;
import org.baiyu.fuckshare.filetype.FileType;
import org.baiyu.fuckshare.filetype.ImageType;
import org.baiyu.fuckshare.filetype.OtherType;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HandleShareActivity extends Activity {
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private static Settings settings;

    /**
     * @noinspection deprecation
     */
    @SuppressLint("WorldReadableFiles")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs;
        try {
            prefs = getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_WORLD_READABLE);
        } catch (SecurityException ignore) {
            prefs = getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_PRIVATE);
        }
        settings = Settings.getInstance(prefs);

        Intent intent = getIntent();
        if ("text/plain".equals(intent.getType())) {
            handleSendText(intent);
        } else {
            List<Uri> uris = Utils.getUrisFromIntent(intent);
            assert uris != null;
            handleUris(uris);
        }
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        PeriodicWorkRequest clearCacheWR = new PeriodicWorkRequest.Builder(
                ClearCacheWorker.class,
                1, TimeUnit.DAYS
        ).setConstraints(new Constraints.Builder().setRequiresDeviceIdle(true).build())
                .setInitialDelay(10, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(ClearCacheWorker.id, ExistingPeriodicWorkPolicy.KEEP, clearCacheWR);
    }

    void handleSendText(Intent intent) {
        ShareCompat.IntentBuilder ib = new ShareCompat.IntentBuilder(this);
        ib.setType(intent.getType());
        ib.setText(getIntent().getStringExtra(Intent.EXTRA_TEXT));
        Intent chooserIntent = ib.createChooserIntent();
        chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, List.of(new ComponentName(this, HandleShareActivity.class)).toArray(new Parcelable[]{}));
        startActivity(chooserIntent);
    }

    private void handleUris(List<Uri> uris) {
        ShareCompat.IntentBuilder ib = new ShareCompat.IntentBuilder(this).setType(getIntent().getType());
        uris.stream().map(uri -> executorService.submit(() -> refreshUri(uri))).map(uriFuture -> {
                    try {
                        return uriFuture.get();
                    } catch (ExecutionException | InterruptedException e) {
                        Log.d("fuckshare", e.toString());
                        return null;
                    }
                })
                .filter(Objects::nonNull).forEach(ib::addStream);
        Intent chooserIntent = ib.createChooserIntent();
        chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, List.of(new ComponentName(this, HandleShareActivity.class)).toArray(new Parcelable[]{}));
        startActivity(chooserIntent);
    }

    @Nullable
    private Uri refreshUri(Uri uri) {
        try {
            byte[] magickBytes = new byte[16];
            try (InputStream uin = this.getContentResolver().openInputStream(uri)) {
                assert uin != null;
                Utils.inputStreamRead(uin, magickBytes);
            }

            FileType fileType = Utils.getFileType(magickBytes);

            String originName = Utils.getRealFileName(this, uri);
            String newFilename = getNewName(fileType, originName);

            File file = new File(getCacheDir(), newFilename);

            if (fileType instanceof ImageType imageType && settings.enableRemoveExif()) {
                processImgMetadata(file, imageType, uri);
            } else {
                try (InputStream uin = getContentResolver().openInputStream(uri);
                     OutputStream fout = new FileOutputStream(file)) {
                    assert uin != null;
                    FileUtils.copy(uin, fout);
                }
            }
            return FileProvider.getUriForFile(this, this.getPackageName() + ".fileprovider", file);
        } catch (IOException e) {
            Log.d("fuckshare", e.toString());
            return null;
        }
    }

    private void processImgMetadata(File file, ImageType imageType, Uri uri) throws IOException {
        ExifHelper eh = null;
        switch (imageType) {
            case JPEG -> eh = new jpegExifHelper();
            case PNG -> eh = new pngExifHelper();
            case WEBP -> eh = new webpExifHelper();
        }
        if (eh == null) {
            Log.e("fuckshare", "unsupported image type: " + imageType);
        } else {
            try (InputStream uin = getContentResolver().openInputStream(uri);
                 OutputStream fout = new FileOutputStream(file)) {
                eh.removeMetadata(uin, fout);
            }
        }
        if (imageType.isSupportMetadata()) {
            try (InputStream uin = getContentResolver().openInputStream(uri)) {
                assert uin != null;
                ExifHelper.writeBackMetadata(
                        new ExifInterface(uin),
                        new ExifInterface(file),
                        settings.getExifTagsToKeep());
            }
        }
    }

    private String getNewName(FileType fileType, String originName) {
        String newFilename;
        String newExt = null;

        if ((fileType instanceof ImageType && settings.enableImageRename()) ||
                (!(fileType instanceof ImageType) && settings.enableFileRename())) {
            newFilename = Utils.getRandomString();
        } else {
            newFilename = Utils.getFileName(originName);
        }

        if (settings.enableFileTypeSniff()) {
            newExt = fileType.getExtension();
        }
        if (Objects.equals(newExt, OtherType.UNKNOWN.getExtension())) {
            newExt = Utils.getFileExt(originName);
        }
        return Utils.mergeFilename(newFilename, newExt);
    }
}