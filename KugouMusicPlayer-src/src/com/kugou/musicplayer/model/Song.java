package com.kugou.musicplayer.model;

import java.io.Serializable;

/**
 * 歌曲数据模型
 * 从酷狗APK逆向提取的字段结构
 */
public class Song implements Serializable {
    private static final long serialVersionUID = 1L;

    private String hash;
    private String songname;
    private String singername;
    private String albumId;
    private String albumName;
    private int duration;
    private long filesize;
    private String ext;
    private String playUrl;
    private String imgUrl;
    private String lyricUrl;
    private int bitrate;

    public Song() {
    }

    public Song(String hash, String songname, String singername, int duration) {
        this.hash = hash;
        this.songname = songname;
        this.singername = singername;
        this.duration = duration;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getSongname() {
        return songname;
    }

    public void setSongname(String songname) {
        this.songname = songname;
    }

    public String getSingername() {
        return singername;
    }

    public void setSingername(String singername) {
        this.singername = singername;
    }

    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
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

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
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

    public String getLyricUrl() {
        return lyricUrl;
    }

    public void setLyricUrl(String lyricUrl) {
        this.lyricUrl = lyricUrl;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    /**
     * 格式化时长为 mm:ss
     */
    public String getFormatDuration() {
        if (duration <= 0) {
            return "00:00";
        }
        int m = duration / 60;
        int s = duration % 60;
        return (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Song song = (Song) o;
        return hash != null ? hash.equals(song.hash) : song.hash == null;
    }

    public int hashCode() {
        return hash != null ? hash.hashCode() : 0;
    }
}
