package org.baiyu.fuckshare.filetype;

import java.util.Map;
import java.util.Set;

public enum OtherType implements FileType {
    UNKNOWN(null, null),
    PDF("pdf", Set.of(
            Map.of(0, new byte[]{(byte) 0x25, (byte) 0x50, (byte) 0x44, (byte) 0x46, (byte) 0x2D})
    )),
    M3U("m3u", Set.of(
            Map.of(0, new byte[]{(byte) 0x23, (byte) 0x45, (byte) 0x58, (byte) 0x54, (byte) 0x4D, (byte) 0x33, (byte) 0x55})
    ));

    private final String extension;
    private final Set<Map<Integer, byte[]>> signatures;

    OtherType(String extension, Set<Map<Integer, byte[]>> signatures) {
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
