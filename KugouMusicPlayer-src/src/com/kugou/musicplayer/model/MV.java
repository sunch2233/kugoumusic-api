package com.kugou.musicplayer.model;

import java.io.Serializable;

/**
 * MV数据模型
 * 从酷狗APK逆向提取的字段结构
 */
public class MV implements Serializable {
    private static final long serialVersionUID = 1L;

    private String mvhash;
    private String title;
    private String singer;
    private String playUrl;
    private String imgUrl;
    private String path;
    private int duration;
    private long filesize;
    private String format;

    public MV() {
    }

    public MV(String mvhash, String title, String singer) {
        this.mvhash = mvhash;
        this.title = title;
        this.singer = singer;
    }

    public String getMvhash() {
        return mvhash;
    }

    public void setMvhash(String mvhash) {
        this.mvhash = mvhash;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSinger() {
        return singer;
    }

    public void setSinger(String singer) {
        this.singer = singer;
    }

    public String getPlayUrl() {
        return playUrl;
    }

    public void setPlayUrl(String playUrl) {
        this.playUrl = playUrl;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public long getFilesize() {
        return filesize;
    }

    public void setFilesize(long filesize) {
        this.filesize = filesize;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MV mv = (MV) o;
        return mvhash != null ? mvhash.equals(mv.mvhash) : mv.mvhash == null;
    }

    public int hashCode() {
        return mvhash != null ? mvhash.hashCode() : 0;
    }
}
