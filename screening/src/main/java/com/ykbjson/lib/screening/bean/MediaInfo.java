package com.ykbjson.lib.screening.bean;

/**
 * Description：多媒体信息
 * <BR/>
 * Creator：yankebin
 * <BR/>
 * CreatedAt：2019-07-09
 */
public class MediaInfo {
    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_VIDEO = 2;
    public static final int TYPE_AUDIO = 3;
    public static final int TYPE_MIRROR = 4;

    private String mediaName;
    private String mediaId;
    private int mediaType = TYPE_UNKNOWN;
    private String uri;
    private String filePath;
    private long duration;
    private String bulbulName;
    private String theAlbumName;
    private int index;

    public String getMediaName() {
        return this.mediaName;
    }

    public void setMediaName(String mediaName) {
        this.mediaName = mediaName;
    }

    public String getMediaId() {
        return this.mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public int getMediaType() {
        return this.mediaType;
    }

    public void setMediaType(int mediaType) {
        this.mediaType = mediaType;
    }

    public String getUri() {
        return this.uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getDuration() {
        return this.duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getBulbulName() {
        return this.bulbulName;
    }

    public void setBulbulName(String bulbulName) {
        this.bulbulName = bulbulName;
    }

    public String getTheAlbumName() {
        return this.theAlbumName;
    }

    public void setTheAlbumName(String theAlbumName) {
        this.theAlbumName = theAlbumName;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}