package com.kugou.musicplayer.service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.kugou.musicplayer.api.KugouApi;
import com.kugou.musicplayer.model.Song;

import java.util.ArrayList;
import java.util.List;

/**
 * 音乐播放服务
 * 对应原APK中的 KugouPlaybackService
 */
public class MusicPlayService extends Service {
    private static final String TAG = "MusicPlayService";

    /**
     * 全局单例：供迷你播放栏等界面读取当前播放状态
     * （服务常驻期间非空）
     */
    public static MusicPlayService sInstance;

    private MediaPlayer mediaPlayer;
    private List playlist;
    private int currentIndex;
    private Song currentSong;
    private boolean isPreparing;
    private PlaybackListener listener;

    private final IBinder binder = new MusicBinder();

    public class MusicBinder extends Binder {
        public MusicPlayService getService() {
            return MusicPlayService.this;
        }
    }

    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        playlist = new ArrayList();
        currentIndex = -1;

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                Log.d(TAG, "onCompletion");
                playNext();
                if (listener != null) {
                    listener.onCompletion();
                }
            }
        });

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                isPreparing = false;
                mp.start();
                if (listener != null) {
                    listener.onPrepared();
                }
            }
        });

        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(TAG, "onError what=" + what + " extra=" + extra);
                // 关键修复：如果已经在播放，忽略此错误
                // HTTPS网络流在Android 2.3上可能触发误报错误但实际播放正常
                try {
                    if (mp.isPlaying()) {
                        Log.w(TAG, "error received but still playing, ignore");
                        return true;
                    }
                } catch (Exception e) {
                    // ignore
                }
                isPreparing = false;
                if (listener != null) {
                    listener.onError(what, extra);
                }
                return true;
            }
        });
    }

    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * 以startService方式启动的服务在unbind后不销毁，音乐可后台持续播放
     */
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    /**
     * 设置播放列表
     */
    public void setPlaylist(List songs) {
        if (songs != null) {
            this.playlist = new ArrayList(songs);
        }
    }

    /**
     * 播放指定歌曲
     */
    public void playSong(Song song) {
        if (song == null) return;
        currentSong = song;
        // 在播放列表中查找索引
        for (int i = 0; i < playlist.size(); i++) {
            Song s = (Song) playlist.get(i);
            if (s.getHash() != null && s.getHash().equals(song.getHash())) {
                currentIndex = i;
                break;
            }
        }
        startPlayback();
    }

    /**
     * 播放指定索引的歌曲
     */
    public void playAt(int index) {
        if (index < 0 || index >= playlist.size()) return;
        currentIndex = index;
        currentSong = (Song) playlist.get(index);
        startPlayback();
    }

    /**
     * 开始播放（获取播放地址并播放）
     */
    private void startPlayback() {
        if (currentSong == null || isPreparing) return;

        isPreparing = true;
        if (listener != null) {
            listener.onStartPrepare(currentSong);
        }

        // 在子线程获取播放地址
        new Thread(new Runnable() {
            public void run() {
                try {
                    String playUrl = currentSong.getPlayUrl();
                    if (playUrl == null || playUrl.length() == 0) {
                        playUrl = KugouApi.getInstance().getPlayUrl(currentSong.getHash(), "m4a");
                        currentSong.setPlayUrl(playUrl);
                    }

                    if (playUrl != null && playUrl.length() > 0) {
                        mediaPlayer.reset();
                        mediaPlayer.setDataSource(playUrl);
                        mediaPlayer.prepareAsync();
                    } else {
                        isPreparing = false;
                        if (listener != null) {
                            listener.onError(-1, 0);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "startPlayback error", e);
                    isPreparing = false;
                    if (listener != null) {
                        listener.onError(-1, 0);
                    }
                }
            }
        }).start();
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying() && !isPreparing) {
            mediaPlayer.start();
        }
    }

    public void playNext() {
        if (playlist.isEmpty()) return;
        currentIndex = (currentIndex + 1) % playlist.size();
        currentSong = (Song) playlist.get(currentIndex);
        startPlayback();
    }

    public void playPrev() {
        if (playlist.isEmpty()) return;
        currentIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
        currentSong = (Song) playlist.get(currentIndex);
        startPlayback();
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }

    public void setVolume(float left, float right) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(left, right);
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null ? mediaPlayer.getDuration() : 0;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public List getPlaylist() {
        return playlist;
    }

    public void setPlaybackListener(PlaybackListener listener) {
        this.listener = listener;
    }

    public void onDestroy() {
        super.onDestroy();
        sInstance = null;
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * 播放状态回调
     */
    public interface PlaybackListener {
        void onStartPrepare(Song song);
        void onPrepared();
        void onCompletion();
        void onError(int what, int extra);
    }
}
