package org.baiyu.fuckshare.filetype;

public enum OtherType implements FileType {
    UNKNOWN(null);

    private final String extension;

    OtherType(String extension) {
        this.extension = extension;
    }

    @Override
    public String getExtension() {
        return extension;
    }
}
