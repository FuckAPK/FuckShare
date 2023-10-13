package org.baiyu.fuckshare.filetype;

import java.util.Map;
import java.util.Set;

public enum AudioType implements FileType {
    MP3("mp3", Set.of(
            Map.of(0, new byte[]{(byte) 0xFF, (byte) 0xFB}),
            Map.of(0, new byte[]{(byte) 0xFF, (byte) 0xF2}),
            Map.of(0, new byte[]{(byte) 0xFF, (byte) 0xF3}),
            Map.of(0, new byte[]{(byte) 0x49, (byte) 0x44, (byte) 0x33})
    )),
    FLAC("flac", Set.of(
            Map.of(0, new byte[]{(byte) 0x66, (byte) 0x4C, (byte) 0x61, (byte) 0x43})
    )),
    WAVE("wav", Set.of(
            Map.of(0, new byte[]{(byte) 0x52, (byte) 0x49, (byte) 0x46, (byte) 0x46},
                    8, new byte[]{(byte) 0x57, (byte) 0x41, (byte) 0x56, (byte) 0x45})
    )),
    OGG("ogg", Set.of(
            Map.of(0, new byte[]{(byte) 0x4F, (byte) 0x67, (byte) 0x67, (byte) 0x53})
    )),
    AIFF("aiff", Set.of(
            Map.of(0, new byte[]{(byte) 0x46, (byte) 0x4F, (byte) 0x52, (byte) 0x4D},
                    8, new byte[]{(byte) 0x41, (byte) 0x49, (byte) 0x46, (byte) 0x46})
    ));

    private final String extension;
    private final Set<Map<Integer, byte[]>> signatures;

    AudioType(String extension, Set<Map<Integer, byte[]>> signatures) {
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
