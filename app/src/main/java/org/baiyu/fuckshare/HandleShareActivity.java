package org.baiyu.fuckshare;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

public class HandleShareActivity extends Activity {
    private static Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = Settings.getInstance(getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_PRIVATE));
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
        try (OutputStream fout = new FileOutputStream(f);
             InputStream fin = new FileInputStream(f);
             InputStream uin = this.getContentResolver().openInputStream(uri)) {

            Utils.copy(Objects.requireNonNull(uin), fout);

            ImageType imageType = Utils.getImageType(fin);
            Log.d("fuckshare", imageType.toString());
            if (Utils.isKnownImageType(imageType) && settings.enableRemoveExif()) {
                ExifInterface exifInterface = new ExifInterface(f);
                ExifHelper.removeMetadataExclude(exifInterface, settings.getExifTagsToKeep());
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