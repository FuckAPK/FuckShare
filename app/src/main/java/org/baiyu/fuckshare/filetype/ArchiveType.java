package org.baiyu.fuckshare.filetype;

import java.util.Map;
import java.util.Set;

public enum ArchiveType implements FileType {
    LZIP("lz", Set.of(
            Map.of(0, new byte[]{(byte) 0x4C, (byte) 0x5A, (byte) 0x49, (byte) 0x50})
    )),
    RAR("rar", Set.of(
            Map.of(0, new byte[]{(byte) 0x52, (byte) 0x61, (byte) 0x72, (byte) 0x21, (byte) 0x1A, (byte) 0x07, (byte) 0x00})
    )),
    TAR("tar", Set.of(
            Map.of(0, new byte[]{(byte) 0x75, (byte) 0x73, (byte) 0x74, (byte) 0x61, (byte) 0x72, (byte) 0x00, (byte) 0x30, (byte) 0x30}),
            Map.of(0, new byte[]{(byte) 0x75, (byte) 0x73, (byte) 0x74, (byte) 0x61, (byte) 0x72, (byte) 0x20, (byte) 0x20, (byte) 0x00})
    )),
    _7Z("7z", Set.of(
            Map.of(0, new byte[]{(byte) 0x37, (byte) 0x7A, (byte) 0xBC, (byte) 0xAF, (byte) 0x27, (byte) 0x1C})
    )),
    GZ("gz", Set.of(
            Map.of(0, new byte[]{(byte) 0x1F, (byte) 0x8B})
    )),
    XZ("xz", Set.of(
            Map.of(0, new byte[]{(byte) 0xFD, (byte) 0x37, (byte) 0x7A, (byte) 0x58, (byte) 0x5A, (byte) 0x00})
    ));

    private final String extension;
    private final Set<Map<Integer, byte[]>> signatures;


    ArchiveType(String extension, Set<Map<Integer, byte[]>> signatures) {
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
