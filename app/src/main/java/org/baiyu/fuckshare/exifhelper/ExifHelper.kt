package org.baiyu.fuckshare.exifhelper;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import timber.log.Timber;

public interface ExifHelper {

    static void writeBackMetadata(@NonNull ExifInterface exifFrom, @NonNull ExifInterface exifTo, @NonNull Set<String> tags) throws IOException {
        Map<String, String> tagsValue = tags.parallelStream()
                .filter(Objects::nonNull)
                .filter(exifFrom::hasAttribute)
                .collect(Collectors.toMap(
                        tag -> tag,
                        tag -> Optional.ofNullable(exifFrom.getAttribute(tag)).orElse("")));
        tagsValue.forEach(exifTo::setAttribute);
        Timber.d("tags rewrite: %s", tagsValue);
        exifTo.saveAttributes();
    }

    void removeMetadata(InputStream inputStream, OutputStream outputStream) throws IOException, ImageFormatException;
}
