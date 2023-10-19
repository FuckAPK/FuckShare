package org.baiyu.fuckshare;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.baiyu.fuckshare.filetype.ArchiveType;
import org.baiyu.fuckshare.filetype.AudioType;
import org.baiyu.fuckshare.filetype.FileType;
import org.baiyu.fuckshare.filetype.ImageType;
import org.baiyu.fuckshare.filetype.OtherType;
import org.baiyu.fuckshare.filetype.VideoType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import timber.log.Timber;

public class Utils {
    public static List<Uri> getUrisFromIntent(@NonNull Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            Uri uri = getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri.class);
            assert uri != null;
            return List.of(uri);
        } else {
            return getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri.class);
        }
    }

    public static <T extends Parcelable> T getParcelableExtra(Intent intent, String name, Class<T> clazz) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableExtra(name, clazz);
        } else {
            //noinspection deprecation
            return intent.getParcelableExtra(name);
        }
    }

    public static <T extends Parcelable> ArrayList<T> getParcelableArrayListExtra(Intent intent, String name, Class<T> clazz) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableArrayListExtra(name, clazz);
        } else {
            //noinspection deprecation
            return intent.getParcelableArrayListExtra(name);
        }
    }

    @Nullable
    public static String getRealFileName(Context context, @NonNull Uri uri) {

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
            Timber.e("Unknown scheme: %s", uri.getScheme());
            return null;
        }
    }

    @NonNull
    public static String getFileNameNoExt(@NonNull String fullFilename) {
        int lastIndex = fullFilename.lastIndexOf('.');
        if (lastIndex > 0 && lastIndex < fullFilename.length() - 1) {
            return fullFilename.substring(0, lastIndex);
        } else {
            return fullFilename;
        }
    }

    @Nullable
    public static String getFileRealExt(@NonNull String fullFilename) {
        int lastIndex = fullFilename.lastIndexOf('.');
        if (lastIndex > 0 && lastIndex < fullFilename.length() - 1) {
            return fullFilename.substring(lastIndex + 1);
        } else {
            return null;
        }
    }

    public static String mergeFilename(@NonNull String filename, String extension) {
        if (extension != null) {
            filename += "." + extension;
        }
        return filename;
    }

    @NonNull
    public static String getRandomString() {
        return UUID.randomUUID().toString();
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

    public static FileType getFileType(byte[] bytes) {
        Set<Set<FileType>> fileTypes = Set.of(
                Set.of(ImageType.values()),
                Set.of(VideoType.values()),
                Set.of(AudioType.values()),
                Set.of(ArchiveType.values()),
                Set.of(OtherType.values())
        );

        return fileTypes.parallelStream()
                .flatMap(Collection::stream)
                .filter(fileType -> fileType.signatureMatch(bytes))
                .findAny().orElse(OtherType.UNKNOWN);
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

    /** @noinspection unused*/
    @NonNull
    public static byte[] longToBigEndianBytes(long num) {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putLong(num);
        return bb.array();
    }

    @NonNull
    public static byte[] longToLittleEndianBytes(long num) {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putLong(num);
        return bb.array();
    }

    /** @noinspection UnusedReturnValue*/
    public static int inputStreamRead(InputStream inputStream, byte[] bytes) throws IOException {
        return inputStreamRead(inputStream, bytes, 0, bytes.length);
    }

    public static int inputStreamRead(@NonNull InputStream inputStream, byte[] bytes, int offset, int n) throws IOException {
        int remaining = n;
        while (inputStream.available() > 0 && remaining > 0) {
            remaining -= inputStream.read(bytes, offset, remaining);
        }
        return n - remaining;
    }

    public static long inputStreamSkip(@NonNull InputStream inputStream, long n) throws IOException {
        long remaining = n;
        while (inputStream.available() > 0 && remaining > 0) {
            remaining -= inputStream.skip(remaining);
        }
        return n - remaining;
    }

    public static boolean deleteRecursive(@NonNull File file) {
        return file.isFile() ? file.delete() : Arrays.stream(Objects.requireNonNull(file.listFiles()))
                .parallel()
                .allMatch(f -> f.isFile() ? f.delete() : deleteRecursive(f))
                && file.delete();
    }

    public static boolean clearCache(@NonNull Context context, long timeDurationMillis) {
        long timeBefore = System.currentTimeMillis() - timeDurationMillis;
        return Arrays.stream(Objects.requireNonNull(context.getCacheDir().listFiles()))
                .parallel()
                .filter(Objects::nonNull)
                .filter(f -> f.lastModified() < timeBefore)
                .allMatch(Utils::deleteRecursive);
    }
}
