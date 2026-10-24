package com.kugou.musicplayer.util;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.kugou.musicplayer.PlayerActivity;
import com.kugou.musicplayer.R;
import com.kugou.musicplayer.model.Song;
import com.kugou.musicplayer.service.MusicPlayService;

import java.util.ArrayList;

/**
 * 底部迷你播放栏
 *
 * 无播放时显示"酷狗音乐"占位；有播放时显示歌名/歌手和播放状态。
 * 点击栏身打开完整播放器，点击按钮播放/暂停。
 * 通过 MusicPlayService.sInstance 读取全局播放状态（服务常驻期间有效）。
 */
public class MiniPlayer {

    private final Activity activity;
    private final View bar;
    private final TextView tvTitle, tvArtist;
    private final Button btnPlay;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean attached = true;

    private final Runnable ticker = new Runnable() {
        public void run() {
            if (!attached) return;
            refresh();
            handler.postDelayed(this, 500);
        }
    };

    /**
     * 绑定迷你播放栏到Activity（布局中需 include view_mini_player.xml）
     */
    public MiniPlayer(Activity activity) {
        this.activity = activity;
        bar = activity.findViewById(R.id.miniPlayerBar);
        tvTitle = (TextView) activity.findViewById(R.id.tvMiniTitle);
        tvArtist = (TextView) activity.findViewById(R.id.tvMiniArtist);
        btnPlay = (Button) activity.findViewById(R.id.btnMiniPlay);

        if (bar == null) {
            attached = false;
            return;
        }

        bar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                openPlayer();
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                togglePlay();
            }
        });

        refresh();
        handler.postDelayed(ticker, 500);
    }

    /**
     * 打开完整播放器
     */
    private void openPlayer() {
        try {
            Intent intent = new Intent(activity, PlayerActivity.class);
            MusicPlayService svc = MusicPlayService.sInstance;
            if (svc != null && svc.getCurrentSong() != null) {
                Song song = svc.getCurrentSong();
                intent.putExtra("song", song);
                intent.putExtra("playlist", new ArrayList(svc.getPlaylist()));
                intent.putExtra("position", svc.getCurrentIndex());
            }
            activity.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(activity, "打开播放器失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 播放/暂停
     */
    private void togglePlay() {
        try {
            MusicPlayService svc = MusicPlayService.sInstance;
            if (svc == null || svc.getCurrentSong() == null) {
                // 无播放内容：打开播放器
                openPlayer();
                return;
            }
            if (svc.isPlaying()) {
                svc.pause();
                btnPlay.setText("▶");
            } else {
                svc.resume();
                btnPlay.setText("❚❚");
            }
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * 刷新显示
     */
    private void refresh() {
        try {
            MusicPlayService svc = MusicPlayService.sInstance;
            if (svc != null && svc.getCurrentSong() != null) {
                Song song = svc.getCurrentSong();
                tvTitle.setText(song.getSongname());
                tvArtist.setText(song.getSingername());
                btnPlay.setText(svc.isPlaying() ? "❚❚" : "▶");
            } else {
                // 无播放：占位显示应用名
                tvTitle.setText("酷狗音乐");
                tvArtist.setText("搜索你想听的歌");
                btnPlay.setText("▶");
            }
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * 界面销毁时停止刷新（在Activity.onDestroy中调用）
     */
    public void detach() {
        attached = false;
        handler.removeCallbacks(ticker);
    }
}
