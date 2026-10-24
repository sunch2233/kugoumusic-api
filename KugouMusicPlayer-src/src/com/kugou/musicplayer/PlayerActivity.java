package com.kugou.musicplayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kugou.musicplayer.model.Song;
import com.kugou.musicplayer.service.MusicPlayService;
import com.kugou.musicplayer.util.DownloadManager;
import com.kugou.musicplayer.util.FavoriteManager;
import com.kugou.musicplayer.util.LocalMediaScanner;
import com.kugou.musicplayer.util.VolumeBoost;

import java.util.ArrayList;
import java.util.List;

/**
 * 音乐播放器界面
 * 对应原APK中的 KugouPlaybackService + PlayerFragment
 */
public class PlayerActivity extends Activity {

    private ImageView ivAlbum;
    private TextView tvSongName, tvArtist;
    private SeekBar sbProgress, sbVolume;
    private TextView tvCurrentTime, tvTotalTime;
    private Button btnPrev, btnPlay, btnNext;
    private Button btnFavorite, btnDownload, btnBoost;

    private MusicPlayService playService;
    private boolean isBound = false;
    private boolean resumeFromMini = false;
    private Handler handler;
    private boolean isTracking = false;
    private VolumeBoost volumeBoost;
    private Song currentSong;
    private List playlist;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayService.MusicBinder binder = (MusicPlayService.MusicBinder) service;
            playService = binder.getService();
            isBound = true;
            setupPlaybackListener();

            // 如果有传入歌曲，开始播放
            if (currentSong != null) {
                if (resumeFromMini
                        && playService.getCurrentSong() != null
                        && playService.getCurrentSong().getHash() != null
                        && playService.getCurrentSong().getHash().equals(currentSong.getHash())) {
                    // 恢复模式：服务已在播放同一首歌，不重新开始
                    if (playService.isPlaying()) {
                        btnPlay.setText("❚❚");
                    }
                } else {
                    if (playlist != null && !playlist.isEmpty()) {
                        playService.setPlaylist(playlist);
                    }
                    playService.playSong(currentSong);
                }
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_player);
            handler = new Handler(Looper.getMainLooper());
            volumeBoost = VolumeBoost.getInstance(this);
            initViews();
            parseIntent();
            setupListeners();
            bindService();
            startProgressUpdate();
        } catch (Exception e) {
            Toast.makeText(this, "播放器初始化失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        ivAlbum = (ImageView) findViewById(R.id.ivAlbum);
        tvSongName = (TextView) findViewById(R.id.tvSongName);
        tvArtist = (TextView) findViewById(R.id.tvArtist);
        sbProgress = (SeekBar) findViewById(R.id.sbProgress);
        sbVolume = (SeekBar) findViewById(R.id.sbVolume);
        tvCurrentTime = (TextView) findViewById(R.id.tvCurrentTime);
        tvTotalTime = (TextView) findViewById(R.id.tvTotalTime);
        btnPrev = (Button) findViewById(R.id.btnPrev);
        btnPlay = (Button) findViewById(R.id.btnPlay);
        btnNext = (Button) findViewById(R.id.btnNext);
        btnFavorite = (Button) findViewById(R.id.btnFavorite);
        btnDownload = (Button) findViewById(R.id.btnDownload);
        btnBoost = (Button) findViewById(R.id.btnBoost);

        sbVolume.setProgress(volumeBoost.getCurrentVolume());
    }

    @SuppressWarnings("unchecked")
    private void parseIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            currentSong = (Song) intent.getSerializableExtra("song");
            playlist = (List) intent.getSerializableExtra("playlist");
        }
        if (playlist == null) {
            playlist = new ArrayList();
        }

        if (currentSong != null) {
            tvSongName.setText(currentSong.getSongname());
            tvArtist.setText(currentSong.getSingername());
            updateFavoriteButton();
        } else if (MusicPlayService.sInstance != null
                && MusicPlayService.sInstance.getCurrentSong() != null) {
            // 从迷你播放栏/主界面进入：恢复当前正在播放的歌曲
            resumeFromMini = true;
            currentSong = MusicPlayService.sInstance.getCurrentSong();
            List svcPlaylist = MusicPlayService.sInstance.getPlaylist();
            if (svcPlaylist != null && !svcPlaylist.isEmpty()) {
                playlist.clear();
                playlist.addAll(svcPlaylist);
            }
            tvSongName.setText(currentSong.getSongname());
            tvArtist.setText(currentSong.getSingername());
            updateFavoriteButton();
        } else {
            // 没有传入歌曲时，自动扫描本地音乐
            scanLocalMusic();
        }
    }

    /**
     * 扫描本地音乐文件
     */
    private void scanLocalMusic() {
        tvSongName.setText("正在扫描本地音乐...");
        tvArtist.setText("");
        new Thread(new Runnable() {
            public void run() {
                final List localSongs = LocalMediaScanner.scanAudioFiles(PlayerActivity.this);
                handler.post(new Runnable() {
                    public void run() {
                        if (localSongs != null && !localSongs.isEmpty()) {
                            playlist.clear();
                            playlist.addAll(localSongs);
                            // 自动播放第一首
                            currentSong = (Song) localSongs.get(0);
                            tvSongName.setText(currentSong.getSongname());
                            tvArtist.setText(currentSong.getSingername());
                            updateFavoriteButton();
                            if (playService != null && isBound) {
                                playService.setPlaylist(playlist);
                                playService.playSong(currentSong);
                            }
                            Toast.makeText(PlayerActivity.this,
                                    "已扫描到 " + localSongs.size() + " 首本地音乐",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            tvSongName.setText("未找到本地音乐");
                            tvArtist.setText("请先下载音乐或使用搜索功能");
                        }
                    }
                });
            }
        }).start();
    }

    private void setupListeners() {
        btnPlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (playService != null) {
                    if (playService.isPlaying()) {
                        playService.pause();
                        btnPlay.setText("▶");
                    } else {
                        playService.resume();
                        btnPlay.setText("❚❚");
                    }
                }
            }
        });

        btnPrev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (playService != null) {
                    playService.playPrev();
                }
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (playService != null) {
                    playService.playNext();
                }
            }
        });

        btnFavorite.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (currentSong != null) {
                    boolean added = FavoriteManager.getInstance(PlayerActivity.this).toggleFavorite(currentSong);
                    Toast.makeText(PlayerActivity.this,
                            added ? R.string.favorite_added : R.string.favorite_removed,
                            Toast.LENGTH_SHORT).show();
                    updateFavoriteButton();
                }
            }
        });

        btnDownload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (currentSong != null) {
                    DownloadManager.getInstance(PlayerActivity.this).downloadSong(currentSong);
                    Toast.makeText(PlayerActivity.this, R.string.download_start, Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnBoost.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean enabled = volumeBoost.toggleBoost();
                if (enabled) {
                    btnBoost.setBackgroundColor(0xFFE91E63);
                    btnBoost.setText("增强中");
                    btnBoost.setTextColor(0xFFFFFFFF);
                    Toast.makeText(PlayerActivity.this, R.string.boost_enabled, Toast.LENGTH_SHORT).show();
                } else {
                    btnBoost.setBackgroundResource(R.drawable.bg_circle_button);
                    btnBoost.setText("增强");
                    btnBoost.setTextColor(0xFFAAAAAA);
                    Toast.makeText(PlayerActivity.this, R.string.boost_disabled, Toast.LENGTH_SHORT).show();
                }
            }
        });

        sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && playService != null) {
                    int duration = playService.getDuration();
                    if (duration > 0) {
                        playService.seekTo(progress * duration / 100);
                    }
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTracking = true;
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTracking = false;
            }
        });

        sbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    volumeBoost.setVolume(progress);
                }
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupPlaybackListener() {
        if (playService != null) {
            playService.setPlaybackListener(new MusicPlayService.PlaybackListener() {
                public void onStartPrepare(Song song) {
                    currentSong = song;
                    handler.post(new Runnable() {
                        public void run() {
                            tvSongName.setText(currentSong.getSongname());
                            tvArtist.setText(currentSong.getSingername());
                            btnPlay.setText("❚❚");
                            updateFavoriteButton();
                        }
                    });
                }

                public void onPrepared() {
                    handler.post(new Runnable() {
                        public void run() {
                            btnPlay.setText("❚❚");
                        }
                    });
                }

                public void onCompletion() {
                    handler.post(new Runnable() {
                        public void run() {
                            btnPlay.setText("▶");
                        }
                    });
                }

                public void onError(final int what, final int extra) {
                    // 延迟500ms确认：如果此时仍未在播放，才算真正失败
                    // 避免HTTPS流误报错误
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            if (playService != null && !playService.isPlaying()) {
                                btnPlay.setText("▶");
                                Toast.makeText(PlayerActivity.this, "播放失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, 500);
                }
            });
        }
    }

    private void updateFavoriteButton() {
        if (currentSong != null && FavoriteManager.getInstance(this).isFavorite(currentSong.getHash())) {
            btnFavorite.setTextColor(0xFFE91E63);
        } else {
            btnFavorite.setTextColor(0xFF4FC3F7);
        }
    }

    private void bindService() {
        Intent intent = new Intent(this, MusicPlayService.class);
        // 先startService保证服务常驻（unbind后音乐继续播放，供迷你栏读取状态）
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void startProgressUpdate() {
        handler.postDelayed(new Runnable() {
            public void run() {
                if (playService != null && !isTracking) {
                    int current = playService.getCurrentPosition();
                    int duration = playService.getDuration();
                    if (duration > 0) {
                        sbProgress.setProgress(current * 100 / duration);
                        tvCurrentTime.setText(formatTime(current / 1000));
                        tvTotalTime.setText(formatTime(duration / 1000));
                    }
                }
                handler.postDelayed(this, 500);
            }
        }, 500);
    }

    private String formatTime(int seconds) {
        if (seconds < 0) seconds = 0;
        int m = seconds / 60;
        int s = seconds % 60;
        return (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
    }

    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}
