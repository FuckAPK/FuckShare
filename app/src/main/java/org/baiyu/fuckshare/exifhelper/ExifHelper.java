package org.baiyu.fuckshare.exifhelper;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface ExifHelper {

    static void writeBackMetadata(@NonNull ExifInterface exifFrom, @NonNull ExifInterface exifTo, @NonNull Set<String> tags) throws IOException {
        Map<String, String> tagsValue = tags.parallelStream()
                .filter(exifFrom::hasAttribute)
                .collect(Collectors.toMap(tag -> tag, tag -> Optional.ofNullable(exifFrom.getAttribute(tag)).orElse("")));
        tagsValue.entrySet().parallelStream().forEach(entry -> exifTo.setAttribute(entry.getKey(), entry.getValue()));
        Log.d("fuckshare", "tags rewrite: " + tagsValue);
        exifTo.saveAttributes();
    }

    void removeMetadata(InputStream inputStream, OutputStream outputStream) throws IOException, ImageFormatException;
}
