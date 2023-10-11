package org.baiyu.fuckshare;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class HandleShareActivity extends Activity {
    private static Settings settings;

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
        uris.stream().map(this::refreshUri).forEach(ib::addStream);
        Intent chooserIntent = ib.createChooserIntent();
        chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, List.of(new ComponentName(this, HandleShareActivity.class)).toArray(new Parcelable[]{}));
        startActivity(chooserIntent);
    }

    private Uri refreshUri(Uri uri) {
        String realFilename = Utils.getRealFileName(this, uri);
        assert realFilename != null;
        File f = new File(getCacheDir(), realFilename);
        try (OutputStream fout = new BufferedOutputStream(new FileOutputStream(f));
             InputStream uin = new BufferedInputStream(this.getContentResolver().openInputStream(uri))) {

//            uin.mark(32);
            ImageType imageType = Utils.getImageType(uin);
//            uin.reset();

            Log.d("fuckshare", imageType.toString());
            if (Utils.isKnownImageType(imageType) && settings.enableRemoveExif()) {
                switch (imageType) {
                    case JPEG -> ExifHelper.jpegToNewWithoutMetadata(uin, fout);
                    case PNG -> ExifHelper.pngToNewWithoutMetadata(uin, fout);
                    case WEBP -> ExifHelper.webpToNewWithoutMetadata(uin, fout);
                    default -> Log.e("fuckshare", "unsupported image type: " + imageType);
                }
//                ExifHelper.writeBackMetadata(new ExifInterface(uin), new ExifInterface(f), settings.getExifTagsToKeep());
            } else {
                // is file or disabled exif remove
                Utils.copy(uin, fout);
            }
            // do file/image rename
            if ((Utils.isKnownImageType(imageType) && settings.enableImageRename()) ||
                    !Utils.isKnownImageType(imageType) && settings.enableFileRename()) {
                f = Utils.renameToRandom(this, f);
            }
        } catch (IOException e) {
            Log.e("fuckshare", e.toString());
        }

        return FileProvider.getUriForFile(this, this.getPackageName() + ".fileprovider", f);
    }
}