package org.baiyu.fuckshare;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Utils {
    public static List<Uri> getUrisFromIntent(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            assert uri != null;
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
        File renamed = new File(context.getCacheDir(), newFilename);
        // do rename but return false?
        f.renameTo(renamed);
        return renamed;
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

    public static long copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        return copy(inputStream, outputStream, inputStream.available());
    }

    /**
     * copy $len bytes from inputStream to outputStream if available
     *
     * @return number of bytes copied
     */
    public static long copy(InputStream inputStream, OutputStream outputStream, long len) throws IOException {
        long remainLen = len;
        int pLen = 4096;
        byte[] buffer = new byte[pLen];
        for (int bytesRead; remainLen > 0 && (bytesRead = inputStream.read(buffer, 0, (int) Math.min(remainLen, pLen))) != -1; remainLen -= bytesRead) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return len - remainLen;
    }

    public static ImageType getImageType(byte[] bytes) {
        String magick = bytesToHex(bytes);
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

    public static String bytesToHex(byte... bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    public static long bigEndianBytesToLong(byte[] bytes) {
        byte[] newBytes = new byte[8];
        System.arraycopy(bytes, 0, newBytes, 8 - bytes.length, bytes.length);
        ByteBuffer bb = ByteBuffer.wrap(newBytes);
        bb.order(ByteOrder.BIG_ENDIAN);
        return bb.getLong();
    }

    public static long littleEndianBytesToLong(byte[] bytes) {
        byte[] newBytes = Arrays.copyOf(bytes, 8);
        ByteBuffer bb = ByteBuffer.wrap(newBytes);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getLong();
    }

    public static byte[] longToBigEndianBytes(long num) {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putLong(num);
        return bb.array();
    }

    public static byte[] longToLittleEndianBytes(long num) {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putLong(num);
        return bb.array();
    }


    public static int inputStreamRead(InputStream inputStream, byte[] bytes) throws IOException {
        return inputStreamRead(inputStream, bytes, 0, bytes.length);
    }

    public static int inputStreamRead(InputStream inputStream, byte[] bytes, int offset, int n) throws IOException {
        int remaining = n;
        while (inputStream.available() > 0 && remaining > 0) {
            remaining -= inputStream.read(bytes, offset, remaining);
        }
        return n - remaining;
    }

    public static long inputStreamSkip(InputStream inputStream, long n) throws IOException {
        long remaining = n;
        while (inputStream.available() > 0 && remaining > 0) {
            remaining -= inputStream.skip(remaining);
        }
        return n - remaining;
    }

    public static boolean clearCache(Context context, long timeDurationMillis) {
        // delete file modified at least 30 mins ago
        long timeBefore = System.currentTimeMillis() - timeDurationMillis;
        return Arrays.stream(Objects.requireNonNull(context.getCacheDir().listFiles()))
                .filter(Objects::nonNull)
                .filter(File::isFile)
                .filter(f -> f.lastModified() < timeBefore)
                .map(File::delete)
                .filter(b -> !b)
                .findFirst()
                .orElse(true);
    }
}
