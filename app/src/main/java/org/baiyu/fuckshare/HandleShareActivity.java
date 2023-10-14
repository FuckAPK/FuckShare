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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.baiyu.fuckshare.exifhelper.ExifHelper;
import org.baiyu.fuckshare.exifhelper.ImageFormatException;
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
import java.util.concurrent.TimeUnit;

public class HandleShareActivity extends Activity {
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
            assert !uris.isEmpty();
            handleUris(uris);
        }
        finish();
    }

    @Override
    public void finish() {
        PeriodicWorkRequest clearCacheWorkRequest = new PeriodicWorkRequest.Builder(
                ClearCacheWorker.class,
                1, TimeUnit.DAYS
        ).setConstraints(new Constraints.Builder().setRequiresDeviceIdle(true).build())
                .setInitialDelay(10, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(ClearCacheWorker.id, ExistingPeriodicWorkPolicy.KEEP, clearCacheWorkRequest);

        super.finish();
    }

    void handleSendText(@NonNull Intent intent) {
        ShareCompat.IntentBuilder ib = new ShareCompat.IntentBuilder(this);
        ib.setType(intent.getType());
        ib.setText(getIntent().getStringExtra(Intent.EXTRA_TEXT));
        Intent chooserIntent = ib.createChooserIntent();
        chooserIntent.putExtra(
                Intent.EXTRA_EXCLUDE_COMPONENTS,
                List.of(new ComponentName(this, HandleShareActivity.class)).toArray(new Parcelable[]{}));
        startActivity(chooserIntent);
    }

    private void handleUris(@NonNull List<Uri> uris) {
        ShareCompat.IntentBuilder ib = new ShareCompat.IntentBuilder(this)
                .setType(getIntent().getType());
        uris.parallelStream()
                .map(this::refreshUri)
                .filter(Objects::nonNull)
                .forEachOrdered(ib::addStream);
        Intent chooserIntent = ib.createChooserIntent();
        chooserIntent.putExtra(
                Intent.EXTRA_EXCLUDE_COMPONENTS,
                List.of(new ComponentName(this, HandleShareActivity.class)).toArray(new Parcelable[]{}));
        startActivity(chooserIntent);
    }

    @Nullable
    private Uri refreshUri(Uri uri) {
        String originName = Utils.getRealFileName(this, uri);
        File tempfile = new File(getCacheDir(), Utils.getRandomString());
        try {
            byte[] magickBytes = new byte[16];
            try (InputStream uin = this.getContentResolver().openInputStream(uri)) {
                assert uin != null;
                Utils.inputStreamRead(uin, magickBytes);
            }
            FileType fileType = Utils.getFileType(magickBytes);

            if (fileType instanceof ImageType imageType
                    && imageType.isSupportMetadata()
                    && settings.enableRemoveExif()) {
                try {
                    processImgMetadata(tempfile, imageType, uri);
                } catch (ImageFormatException e) {
                    //noinspection ResultOfMethodCallIgnored
                    tempfile.delete();
                    Log.e("fuckshare", "Format error: " + originName + " Type: " + imageType);
                    Toast.makeText(this, "Format error: " + originName + " Type: " + imageType, Toast.LENGTH_SHORT).show();
                    if (settings.enableFallbackToFile()) {
                        copyFileFromUri(uri, tempfile);
                        fileType = OtherType.UNKNOWN;
                    } else {
                        return null;
                    }
                }
            } else {
                copyFileFromUri(uri, tempfile);
            }
            // rename
            String newNameNoExt = getNewNameNoExt(fileType, originName);
            String ext = getExt(fileType, originName);
            String newFullName = Utils.mergeFilename(newNameNoExt, ext);

            File renamed = new File(getCacheDir(), newFullName);
            if (renamed.exists()) {
                File oneTimeCacheDir = new File(getCacheDir(), Utils.getRandomString());
                //noinspection ResultOfMethodCallIgnored
                oneTimeCacheDir.mkdirs();
                renamed = new File(oneTimeCacheDir, newFullName);
            }
            if (tempfile.renameTo(renamed)) {
                tempfile = renamed;
            }
            return FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", tempfile);
        } catch (IOException e) {
            Log.e("fuckshare", e.toString());
            Toast.makeText(this, "Failed to process: " + originName, Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void copyFileFromUri(Uri uri, File file) throws IOException {
        try (InputStream uin = getContentResolver().openInputStream(uri);
             OutputStream fout = new FileOutputStream(file)) {
            assert uin != null;
            FileUtils.copy(uin, fout);
        }
    }

    private void processImgMetadata(File file, @NonNull ImageType imageType, Uri uri) throws IOException, ImageFormatException {
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

    private String getNewNameNoExt(FileType fileType, String originName) {
        String newNameNoExt;
        if ((fileType instanceof ImageType && settings.enableImageRename()) ||
                (!(fileType instanceof ImageType) && settings.enableFileRename())) {
            newNameNoExt = Utils.getRandomString();
        } else {
            newNameNoExt = Utils.getFileNameNoExt(originName);
        }
        return newNameNoExt;
    }

    @Nullable
    private String getExt(FileType fileType, String originName) {
        String extension = null;
        if (settings.enableFileTypeSniff()) {
            extension = fileType.getExtension();
        }
        if (extension == null) {
            extension = Utils.getFileRealExt(originName);
        }
        return extension;
    }
}