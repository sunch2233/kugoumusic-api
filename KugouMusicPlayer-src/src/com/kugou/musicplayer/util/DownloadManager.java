package com.kugou.musicplayer.util;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.kugou.musicplayer.api.KugouApi;
import com.kugou.musicplayer.model.DownloadTask;
import com.kugou.musicplayer.model.MV;
import com.kugou.musicplayer.model.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 下载管理器
 * 支持音乐和MV下载
 * 统一目录：/sdcard/KugouMusic/music/ 和 /sdcard/KugouMusic/mv/
 */
public class DownloadManager {
    private static final String TAG = "DownloadManager";
    private static final String BASE_DIR = "/KugouMusic/";
    private static final String DIR_MUSIC = "music/";
    private static final String DIR_MV = "mv/";

    // 支持的音频扩展名
    private static final String[] AUDIO_EXTS = {".mp3", ".m4a", ".wav", ".flac", ".ogg", ".aac", ".wma"};
    // 支持的视频扩展名
    private static final String[] VIDEO_EXTS = {".mp4", ".mkv", ".avi", ".flv", ".wmv", ".3gp", ".mov"};

    private static DownloadManager instance;
    private Context context;
    private List taskList;
    private Handler handler;
    private DownloadListener listener;

    private DownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.taskList = new ArrayList();
        this.handler = new Handler(Looper.getMainLooper());
    }

    public static synchronized DownloadManager getInstance(Context context) {
        if (instance == null) {
            instance = new DownloadManager(context);
        }
        return instance;
    }

    public void setDownloadListener(DownloadListener listener) {
        this.listener = listener;
    }

    /**
     * 下载音乐
     */
    public void downloadSong(final Song song) {
        if (song == null) return;

        final DownloadTask task = new DownloadTask();
        task.setId((int) System.currentTimeMillis());
        task.setName(song.getSongname() + " - " + song.getSingername() + ".mp3");
        task.setType("music");
        task.setStatus(DownloadTask.STATUS_WAITING);
        taskList.add(0, task);
        notifyUpdate();

        new Thread(new Runnable() {
            public void run() {
                try {
                    task.setStatus(DownloadTask.STATUS_DOWNLOADING);
                    notifyUpdate();

                    // 1. 获取播放地址
                    String playUrl = KugouApi.getInstance().getPlayUrl(song.getHash(), "mp3");
                    if (playUrl == null || playUrl.length() == 0) {
                        task.setStatus(DownloadTask.STATUS_FAILED);
                        notifyUpdate();
                        return;
                    }
                    task.setUrl(playUrl);

                    // 2. 根据URL扩展名确定文件名
                    String ext = getUrlExtension(playUrl, ".mp3");
                    String fileName = sanitizeFileName(song.getSongname() + " - " + song.getSingername()) + ext;
                    task.setName(fileName);

                    // 3. 下载文件
                    String savePath = getMusicDir() + fileName;
                    task.setSavePath(savePath);
                    boolean success = HttpUtil.downloadFile(playUrl, savePath,
                            new HttpUtil.DownloadProgressListener() {
                                public void onProgress(int downloaded, int total) {
                                    if (total > 0) {
                                        task.setProgress(downloaded * 100 / total);
                                        task.setDownloadedSize(downloaded);
                                        task.setTotalSize(total);
                                        notifyUpdate();
                                    }
                                }
                            });

                    task.setStatus(success ? DownloadTask.STATUS_DONE : DownloadTask.STATUS_FAILED);
                    task.setProgress(success ? 100 : task.getProgress());
                    notifyUpdate();
                } catch (Exception e) {
                    Log.e(TAG, "downloadSong error", e);
                    task.setStatus(DownloadTask.STATUS_FAILED);
                    notifyUpdate();
                }
            }
        }).start();
    }

    /**
     * 下载MV
     */
    public void downloadMV(final MV mv) {
        if (mv == null) return;

        final DownloadTask task = new DownloadTask();
        task.setId((int) System.currentTimeMillis());
        task.setName(mv.getTitle() + ".mkv");
        task.setType("mv");
        task.setStatus(DownloadTask.STATUS_WAITING);
        taskList.add(0, task);
        notifyUpdate();

        new Thread(new Runnable() {
            public void run() {
                try {
                    task.setStatus(DownloadTask.STATUS_DOWNLOADING);
                    notifyUpdate();

                    // 1. 获取MV播放地址
                    String playUrl = KugouApi.getInstance().getMVPlayUrl(mv.getMvhash(), mv.getPath());
                    if (playUrl == null || playUrl.length() == 0) {
                        task.setStatus(DownloadTask.STATUS_FAILED);
                        notifyUpdate();
                        return;
                    }
                    task.setUrl(playUrl);

                    // 2. 根据URL扩展名确定文件名（酷狗MV是mkv格式）
                    String ext = getUrlExtension(playUrl, ".mkv");
                    String fileName = sanitizeFileName(mv.getTitle()) + ext;
                    task.setName(fileName);

                    // 3. 下载文件
                    String savePath = getMVDir() + fileName;
                    task.setSavePath(savePath);
                    boolean success = HttpUtil.downloadFile(playUrl, savePath,
                            new HttpUtil.DownloadProgressListener() {
                                public void onProgress(int downloaded, int total) {
                                    if (total > 0) {
                                        task.setProgress(downloaded * 100 / total);
                                        task.setDownloadedSize(downloaded);
                                        task.setTotalSize(total);
                                        notifyUpdate();
                                    }
                                }
                            });

                    task.setStatus(success ? DownloadTask.STATUS_DONE : DownloadTask.STATUS_FAILED);
                    task.setProgress(success ? 100 : task.getProgress());
                    notifyUpdate();
                } catch (Exception e) {
                    Log.e(TAG, "downloadMV error", e);
                    task.setStatus(DownloadTask.STATUS_FAILED);
                    notifyUpdate();
                }
            }
        }).start();
    }

    /**
     * 获取URL的扩展名
     */
    private String getUrlExtension(String url, String defaultExt) {
        if (url == null) return defaultExt;
        int queryIndex = url.indexOf('?');
        if (queryIndex > 0) {
            url = url.substring(0, queryIndex);
        }
        int dotIndex = url.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < url.length() - 1) {
            String ext = url.substring(dotIndex).toLowerCase();
            if (ext.length() <= 5) {
                return ext;
            }
        }
        return defaultExt;
    }

    /**
     * 清理文件名中的非法字符
     */
    private String sanitizeFileName(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    /**
     * 获取音乐下载目录
     */
    public String getMusicDir() {
        String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + BASE_DIR + DIR_MUSIC;
        File file = new File(dir);
        if (!file.exists()) {
            file.mkdirs();
        }
        return dir;
    }

    /**
     * 获取MV下载目录
     */
    public String getMVDir() {
        String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + BASE_DIR + DIR_MV;
        File file = new File(dir);
        if (!file.exists()) {
            file.mkdirs();
        }
        return dir;
    }

    /**
     * 扫描已下载的音乐文件
     */
    public List scanDownloadedMusic() {
        List list = new ArrayList();
        File dir = new File(getMusicDir());
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && isAudioFile(file.getName())) {
                        list.add(file);
                    }
                }
            }
        }
        return list;
    }

    /**
     * 扫描已下载的MV文件
     */
    public List scanDownloadedMV() {
        List list = new ArrayList();
        File dir = new File(getMVDir());
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && isVideoFile(file.getName())) {
                        list.add(file);
                    }
                }
            }
        }
        return list;
    }

    private boolean isAudioFile(String name) {
        String lower = name.toLowerCase();
        for (String ext : AUDIO_EXTS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    private boolean isVideoFile(String name) {
        String lower = name.toLowerCase();
        for (String ext : VIDEO_EXTS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    public List getDownloadTasks() {
        return new ArrayList(taskList);
    }

    public int getDownloadCount() {
        return taskList.size();
    }

    private void notifyUpdate() {
        handler.post(new Runnable() {
            public void run() {
                if (listener != null) {
                    listener.onDownloadUpdate();
                }
            }
        });
    }

    public interface DownloadListener {
        void onDownloadUpdate();
    }
}
