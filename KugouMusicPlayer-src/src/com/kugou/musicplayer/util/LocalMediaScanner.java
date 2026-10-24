package com.kugou.musicplayer.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.kugou.musicplayer.model.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地媒体扫描工具
 * 扫描手机中的音频文件和已下载的MV文件
 */
public class LocalMediaScanner {
    private static final String TAG = "LocalMediaScanner";

    // 支持的音频格式
    private static final String[] AUDIO_EXTENSIONS = {
        ".mp3", ".m4a", ".wav", ".wma", ".flac", ".ogg", ".aac", ".ape"
    };

    // 支持的视频格式
    private static final String[] VIDEO_EXTENSIONS = {
        ".mp4", ".mkv", ".avi", ".flv", ".wmv", ".3gp", ".mov"
    };

    private LocalMediaScanner() {
    }

    /**
     * 扫描手机中的所有音频文件（使用MediaStore，兼容Android 2.3）
     */
    public static List scanAudioFiles(Context context) {
        List list = new ArrayList();
        try {
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.SIZE
            };
            String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
            String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

            Cursor cursor = context.getContentResolver().query(uri, projection, selection, null, sortOrder);
            if (cursor != null) {
                try {
                    int titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                    int artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                    int albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                    int durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                    int dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);

                    while (cursor.moveToNext()) {
                        Song song = new Song();
                        song.setHash("local_" + cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
                        song.setSongname(cursor.getString(titleCol));
                        song.setSingername(cursor.getString(artistCol));
                        song.setAlbumName(cursor.getString(albumCol));
                        int duration = cursor.getInt(durationCol);
                        song.setDuration(duration > 0 ? duration / 1000 : 0);
                        song.setPlayUrl(cursor.getString(dataCol)); // 本地文件路径作为播放地址
                        list.add(song);
                    }
                } finally {
                    cursor.close();
                }
            }
            Log.d(TAG, "scanAudioFiles found: " + list.size());
        } catch (Exception e) {
            Log.e(TAG, "scanAudioFiles error", e);
            // MediaStore失败时，用文件扫描兜底
            list = scanAudioByFile(context);
        }
        return list;
    }

    /**
     * 通过文件扫描兜底（扫描常用音乐目录）
     */
    private static List scanAudioByFile(Context context) {
        List list = new ArrayList();
        try {
            String[] dirs = {
                android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/Music",
                android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download",
                android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/KugouMusic/download",
                android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/酷狗音乐"
            };
            for (String dirPath : dirs) {
                File dir = new File(dirPath);
                if (dir.exists() && dir.isDirectory()) {
                    scanAudioDir(dir, list);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "scanAudioByFile error", e);
        }
        return list;
    }

    private static void scanAudioDir(File dir, List list) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                scanAudioDir(file, list);
            } else {
                String name = file.getName().toLowerCase();
                for (String ext : AUDIO_EXTENSIONS) {
                    if (name.endsWith(ext)) {
                        Song song = new Song();
                        song.setHash("local_" + file.getAbsolutePath().hashCode());
                        song.setSongname(file.getName().substring(0, file.getName().lastIndexOf('.')));
                        song.setSingername("本地音乐");
                        song.setPlayUrl(file.getAbsolutePath());
                        list.add(song);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 扫描已下载的MV文件
     */
    public static List scanMVFiles(Context context) {
        List list = new ArrayList();
        try {
            // 扫描酷狗下载目录
            String mvDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/KugouMusic/mv";
            File dir = new File(mvDir);
            if (dir.exists() && dir.isDirectory()) {
                scanMVDir(dir, list);
            }
            // 同时扫描Download目录
            String dlDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/Download";
            File dl = new File(dlDir);
            if (dl.exists() && dl.isDirectory()) {
                scanMVDir(dl, list);
            }
            Log.d(TAG, "scanMVFiles found: " + list.size());
        } catch (Exception e) {
            Log.e(TAG, "scanMVFiles error", e);
        }
        return list;
    }

    private static void scanMVDir(File dir, List list) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                scanMVDir(file, list);
            } else {
                String name = file.getName().toLowerCase();
                for (String ext : VIDEO_EXTENSIONS) {
                    if (name.endsWith(ext)) {
                        com.kugou.musicplayer.model.MV mv = new com.kugou.musicplayer.model.MV();
                        mv.setMvhash("local_" + file.getAbsolutePath().hashCode());
                        mv.setTitle(file.getName().substring(0, file.getName().lastIndexOf('.')));
                        mv.setSinger("本地MV");
                        mv.setPlayUrl(file.getAbsolutePath());
                        mv.setDuration((int) (file.length() / 1024 / 1024)); // 用大小近似
                        list.add(mv);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 判断是否为本地文件
     */
    public static boolean isLocalFile(String path) {
        return path != null && !path.startsWith("http://") && !path.startsWith("https://");
    }
}
