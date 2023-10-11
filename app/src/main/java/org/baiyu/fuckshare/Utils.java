package org.baiyu.fuckshare;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Utils {
    public static List<Uri> getUrisFromIntent(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            assert uri != null;
            Log.d("fuckshare", uri.toString());
            return List.of(uri);
        } else {
            return intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        }
    }

    public static File renameToRandom(Context context, File f) {
        String newFilename = Utils.getRandomName();
        String ext = Utils.getFileExt(f.getName());
        if (ext != null) {
            newFilename += "." + ext;
        }
        File rename = new File(context.getCacheDir(), newFilename);
        f.renameTo(rename);
        return rename;
    }

    public static String getRealFileName(Context context, Uri uri) {

        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            File file = new File(Objects.requireNonNull(uri.getPath()));
            return file.getName();
        } else if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                assert cursor != null;
                cursor.moveToFirst();
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                return cursor.getString(nameIndex);
            }
        } else {
            return null;
        }
    }

    public static String getFileExt(String fullFilename) {
        int lastIndex = fullFilename.lastIndexOf(".");
        if (lastIndex > 0 && lastIndex < fullFilename.length() - 1) {
            return fullFilename.substring(lastIndex + 1);
        } else {
            return null;
        }
    }

    public static String getRandomName() {
        return UUID.randomUUID().toString();
    }

    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        copy(inputStream, outputStream, inputStream.available());
    }

    /**
     * copy $len bytes from inputStream to outputStream if available
     *
     * @param inputStream
     * @param outputStream
     * @param len
     * @return number of bytes copied
     * @throws IOException
     */
    public static int copy(InputStream inputStream, OutputStream outputStream, int len) throws IOException {
        int remainLen = len;
        int pLen = 4096;
        byte[] buffer = new byte[pLen];
        for (int bytesRead; remainLen > 0 && (bytesRead = inputStream.read(buffer, 0, Math.min(remainLen, pLen))) != -1; remainLen -= bytesRead) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        return len - remainLen;
    }

    public static ImageType getImageType(InputStream inputStream) {
        String magick = getMagickNumber(inputStream);
        if (magick.startsWith("89504E470D0A1A0A")) {
            return ImageType.PNG;
        }
        if (magick.startsWith("52494646") && magick.substring(16).startsWith("57454250")) {
            return ImageType.WEBP;
        }
        if (magick.startsWith("FFD8FFDB")
                || magick.startsWith("FFD8FFE0")
                || magick.startsWith("FFD8FFEE")
                || (magick.startsWith("FFD8FFE1") && magick.substring(12).startsWith("457869660000"))) {
            return ImageType.JPEG;
        }
        return ImageType.UNKNOWN;
    }

    public static boolean isKnownImageType(ImageType imageType) {
        return !ImageType.UNKNOWN.equals(imageType);
    }

    public static String getMagickNumber(InputStream inputStream) {
        InputStream in;
        if (inputStream.markSupported()) {
            in = inputStream;
        } else {
            in = new BufferedInputStream(inputStream);
        }

        byte[] bytes = new byte[16];
        try {
            in.mark(32);
            in.read(bytes, 0, 16);
            in.reset();
        } catch (IOException e) {
            Log.e("fuckshare", e.toString());
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        String magickNumber = sb.toString().toUpperCase();
        Log.d("fuckshare", magickNumber);
        return magickNumber;
    }
}
