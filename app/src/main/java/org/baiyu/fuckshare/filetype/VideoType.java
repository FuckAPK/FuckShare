package org.baiyu.fuckshare.filetype;

import java.util.Map;
import java.util.Set;

public enum VideoType implements FileType {
    MP4("mp4", Set.of(
            Map.of(0, new byte[]{(byte) 0x66, (byte) 0x74, (byte) 0x79, (byte) 0x70, (byte) 0x69, (byte) 0x73, (byte) 0x6F, (byte) 0x6D})
    )),
    AVI("avi", Set.of(
            Map.of(0, new byte[]{(byte) 0x52, (byte) 0x49, (byte) 0x46, (byte) 0x46},
                    8, new byte[]{(byte) 0x41, (byte) 0x56, (byte) 0x49, (byte) 0x20}
            )
    )),
    MPEG("mpeg", Set.of(
            Map.of(0, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0xB3})
    ));

    private final String extension;
    private final Set<Map<Integer, byte[]>> signatures;


    VideoType(String extension, Set<Map<Integer, byte[]>> signatures) {
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
}
