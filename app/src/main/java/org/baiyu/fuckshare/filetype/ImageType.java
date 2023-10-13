package org.baiyu.fuckshare.filetype;

import java.util.Map;
import java.util.Set;

public enum ImageType implements FileType {
    JPEG(true, "jpg", Set.of(
            Map.of(0, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xDB}),
            Map.of(0, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}),
            Map.of(0, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE1},
                    6, new byte[]{(byte) 0x45, (byte) 0x78, (byte) 0x69, (byte) 0x66, (byte) 0x00, (byte) 0x00}),
            Map.of(0, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xEE})
    )),
    PNG(true, "png", Set.of(
            Map.of(0, new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A})
    )),
    WEBP(true, "webp", Set.of(
            Map.of(0, new byte[]{(byte) 0x52, (byte) 0x49, (byte) 0x46, (byte) 0x46},
                    8, new byte[]{(byte) 0x57, (byte) 0x45, (byte) 0x42, (byte) 0x50})
    )),
    GIF(false, "gif", Set.of(
            Map.of(0, new byte[]{(byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38, (byte) 0x37, (byte) 0x61}),
            Map.of(0, new byte[]{(byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38, (byte) 0x39, (byte) 0x61})
    ));

    private final boolean supportMetadata;
    private final String extension;
    private final Set<Map<Integer, byte[]>> signatures;

    ImageType(boolean supportMetadata, String extension, Set<Map<Integer, byte[]>> signatures) {
        this.supportMetadata = supportMetadata;
        this.extension = extension;
        this.signatures = signatures;
    }

    @Override
    public String getExtension() {
        return extension;
    }

    @Override
    public Set<Map<Integer, byte[]>> getSignatures() {
        return signatures;
    }

    @Override
    public boolean isSupportMetadata() {
        return supportMetadata;
    }
}
