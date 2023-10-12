package org.baiyu.fuckshare.filetype;

public enum ImageType implements FileType {
    JPEG(true, "jpg"),
    PNG(true, "png"),
    WEBP(true, "webp"),
    GIF(false, "gif");

    private final boolean supportMetadata;

    private final String extension;

    ImageType(boolean supportMetadata, String extension) {
        this.supportMetadata = supportMetadata;
        this.extension = extension;
    }

    @Override
    public String getExtension() {
        return extension;
    }

    public boolean isSupportMetadata() {
        return supportMetadata;
    }
}
