package org.baiyu.fuckshare.filetype;

public enum VideoType implements FileType {
    MP4("mp4"),
    AVI("avi");

    private final String extension;

    VideoType(String extension) {
        this.extension = extension;
    }

    @Override
    public String getExtension() {
        return extension;
    }
}
