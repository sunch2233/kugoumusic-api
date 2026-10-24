package com.kugou.musicplayer.model;

import java.io.Serializable;

/**
 * 下载任务数据模型
 */
public class DownloadTask implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final int STATUS_WAITING = 0;
    public static final int STATUS_DOWNLOADING = 1;
    public static final int STATUS_DONE = 2;
    public static final int STATUS_FAILED = 3;

    private int id;
    private String name;
    private String url;
    private String savePath;
    private int progress;
    private int status;
    private long totalSize;
    private long downloadedSize;
    private String type; // "music" or "mv"
    private long createTime;

    public DownloadTask() {
        this.createTime = System.currentTimeMillis();
        this.status = STATUS_WAITING;
        this.progress = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getDownloadedSize() {
        return downloadedSize;
    }

    public void setDownloadedSize(long downloadedSize) {
        this.downloadedSize = downloadedSize;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getStatusText() {
        switch (status) {
            case STATUS_WAITING:
                return "等待中";
            case STATUS_DOWNLOADING:
                return progress + "%";
            case STATUS_DONE:
                return "已完成";
            case STATUS_FAILED:
                return "失败";
            default:
                return "未知";
        }
    }
}
