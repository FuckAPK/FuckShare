package org.baiyu.fuckshare.exifhelper;

import android.util.Log;

import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface ExifHelper {

    static void writeBackMetadata(ExifInterface exifFrom, ExifInterface exifTo, Set<String> tags) throws IOException {
        Map<String, String> tagsValue = tags.stream()
                .filter(exifFrom::hasAttribute)
                .collect(Collectors.toMap(tag -> tag, tag -> Optional.ofNullable(exifFrom.getAttribute(tag)).orElse("")));
        tagsValue.forEach(exifTo::setAttribute);
        Log.d("fuckshare", "tags rewrite: " + tagsValue);
        exifTo.saveAttributes();
    }

    void removeMetadata(InputStream inputStream, OutputStream outputStream);
}
