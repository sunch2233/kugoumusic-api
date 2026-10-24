package com.kugou.musicplayer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kugou.musicplayer.api.KugouApi;
import com.kugou.musicplayer.model.MV;
import com.kugou.musicplayer.util.DownloadManager;
import com.kugou.musicplayer.util.LocalMediaScanner;

import java.util.ArrayList;
import java.util.List;

/**
 * MV播放器界面（音频模式）
 *
 * 酷狗MV为MKV视频格式，Android系统播放器在多数设备上可解出音频轨。
 * 本界面仅播放音频（保留搜索MV、在线收听、下载、本地列表功能）。
 */
public class MVPlayerActivity extends Activity {

    private TextView tvMVTitle, tvMVArtist, tvMVCount, tvNoLocalMV, tvFormatTip, tvMVStatus;
    private Button btnMVPlay, btnMVDownload, btnMVBack;
    private ListView lvLocalMV;
    private View llLocalMV, llMVPlayer;
    private ProgressDialog progressDialog;
    private MV currentMV;
    private Handler handler;
    private ArrayAdapter localMVAdapter;
    private List localMVList;

    private MediaPlayer mediaPlayer;
    private boolean isPrepared = false;
    private boolean userPaused = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_mv);
            handler = new Handler(Looper.getMainLooper());
            localMVList = new ArrayList();
            initViews();
            parseIntent();
            setupListeners();
        } catch (Exception e) {
            Toast.makeText(this, "初始化失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        tvMVTitle = (TextView) findViewById(R.id.tvMVTitle);
        tvMVArtist = (TextView) findViewById(R.id.tvMVArtist);
        tvMVCount = (TextView) findViewById(R.id.tvMVCount);
        tvNoLocalMV = (TextView) findViewById(R.id.tvNoLocalMV);
        tvFormatTip = (TextView) findViewById(R.id.tvFormatTip);
        tvMVStatus = (TextView) findViewById(R.id.tvMVStatus);
        btnMVPlay = (Button) findViewById(R.id.btnMVPlay);
        btnMVDownload = (Button) findViewById(R.id.btnMVDownload);
        btnMVBack = (Button) findViewById(R.id.btnMVBack);
        lvLocalMV = (ListView) findViewById(R.id.lvLocalMV);
        llLocalMV = findViewById(R.id.llLocalMV);
        llMVPlayer = findViewById(R.id.llMVPlayer);

        localMVAdapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                localMVList) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                try {
                    MV mv = (MV) localMVList.get(position);
                    TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                    TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                    text1.setText(mv.getTitle());
                    text1.setTextColor(0xFFEEEEEE);
                    text1.setTextSize(14);
                    text2.setText(mv.getSinger() + "  |  本地文件");
                    text2.setTextColor(0xFF999999);
                    text2.setTextSize(11);
                } catch (Exception e) {
                    // ignore
                }
                return view;
            }
        };
        lvLocalMV.setAdapter(localMVAdapter);
    }

    private void parseIntent() {
        try {
            currentMV = (MV) getIntent().getSerializableExtra("mv");
        } catch (Exception e) {
            currentMV = null;
        }
        if (currentMV != null) {
            showPlayer();
            tvMVTitle.setText(currentMV.getTitle());
            tvMVArtist.setText(currentMV.getSinger());
            loadMV();
        } else {
            showLocalList();
            scanLocalMV();
        }
    }

    private void setupListeners() {
        btnMVPlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    togglePlayPause();
                } catch (Exception e) {
                    Toast.makeText(MVPlayerActivity.this, "播放控制失败", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnMVDownload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (currentMV != null) {
                    try {
                        DownloadManager.getInstance(MVPlayerActivity.this).downloadMV(currentMV);
                        Toast.makeText(MVPlayerActivity.this, "开始下载，完成后在本地MV列表查看", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(MVPlayerActivity.this, "下载失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        btnMVBack.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    releasePlayer();
                } catch (Exception e) {
                    // ignore
                }
                showLocalList();
                scanLocalMV();
            }
        });

        lvLocalMV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    currentMV = (MV) localMVList.get(position);
                    showPlayer();
                    tvMVTitle.setText(currentMV.getTitle());
                    tvMVArtist.setText(currentMV.getSinger());
                    playSource(currentMV.getPlayUrl());
                } catch (Exception e) {
                    Toast.makeText(MVPlayerActivity.this, "播放失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // ========== 播放控制 ==========

    private void togglePlayPause() {
        if (mediaPlayer == null) return;
        if (userPaused) {
            mediaPlayer.start();
            userPaused = false;
            btnMVPlay.setText("❚❚");
            tvMVStatus.setText("播放中...");
        } else if (isPrepared && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            userPaused = true;
            btnMVPlay.setText("▶");
            tvMVStatus.setText("已暂停");
        }
    }

    private void releasePlayer() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        } catch (Exception e) {
            // ignore
        }
        mediaPlayer = null;
        isPrepared = false;
        userPaused = false;
    }

    /**
     * 播放指定地址（在线URL或本地文件），仅音频
     */
    private void playSource(final String url) {
        if (url == null || url.length() == 0) {
            Toast.makeText(this, "播放地址无效", Toast.LENGTH_SHORT).show();
            return;
        }

        releasePlayer();
        tvFormatTip.setVisibility(View.GONE);
        tvMVStatus.setText("正在缓冲...");
        progressDialog = ProgressDialog.show(this, "", "正在缓冲播放...", true, false);

        try {
            final MediaPlayer mp = new MediaPlayer();
            mediaPlayer = mp;
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer player) {
                    isPrepared = true;
                    try {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        player.start();
                        btnMVPlay.setText("❚❚");
                        int dur = player.getDuration();
                        if (dur > 0) {
                            tvMVStatus.setText("播放中  " + (dur / 60000) + ":" + ((dur % 60000) / 1000));
                        } else {
                            tvMVStatus.setText("播放中...");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer player) {
                    isPrepared = false;
                    btnMVPlay.setText("▶");
                    tvMVStatus.setText("播放完成");
                }
            });

            mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer player, int what, int extra) {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    isPrepared = false;
                    btnMVPlay.setText("▶");
                    tvMVStatus.setText("播放失败");
                    return true;
                }
            });

            mp.setDataSource(url);
            mp.prepareAsync();

        } catch (final Exception e) {
            e.printStackTrace();
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            tvMVStatus.setText("无法播放该视频");
        }
    }

    // ========== 界面切换 ==========

    private void showLocalList() {
        llLocalMV.setVisibility(View.VISIBLE);
        llMVPlayer.setVisibility(View.GONE);
    }

    private void showPlayer() {
        llLocalMV.setVisibility(View.GONE);
        llMVPlayer.setVisibility(View.VISIBLE);
    }

    private void scanLocalMV() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    final List mvs = LocalMediaScanner.scanMVFiles(MVPlayerActivity.this);
                    handler.post(new Runnable() {
                        public void run() {
                            localMVList.clear();
                            if (mvs != null) {
                                localMVList.addAll(mvs);
                            }
                            localMVAdapter.notifyDataSetChanged();
                            tvMVCount.setText("共 " + localMVList.size() + " 个");
                            if (localMVList.isEmpty()) {
                                lvLocalMV.setVisibility(View.GONE);
                                tvNoLocalMV.setVisibility(View.VISIBLE);
                            } else {
                                lvLocalMV.setVisibility(View.VISIBLE);
                                tvNoLocalMV.setVisibility(View.GONE);
                            }
                        }
                    });
                } catch (Exception e) {
                    // ignore
                }
            }
        }).start();
    }

    /**
     * 加载在线MV并直接流式播放音频（无需下载）
     */
    private void loadMV() {
        if (currentMV == null) return;

        // 本地文件直接播放
        if (currentMV.getPlayUrl() != null && LocalMediaScanner.isLocalFile(currentMV.getPlayUrl())) {
            playSource(currentMV.getPlayUrl());
            return;
        }

        progressDialog = ProgressDialog.show(this, "", "获取MV地址...", true, false);

        new Thread(new Runnable() {
            public void run() {
                try {
                    String playUrl = currentMV.getPlayUrl();
                    if (playUrl == null || playUrl.length() == 0) {
                        playUrl = KugouApi.getInstance().getMVPlayUrl(
                                currentMV.getMvhash(), currentMV.getPath());
                        currentMV.setPlayUrl(playUrl);
                    }

                    final String url = playUrl;
                    handler.post(new Runnable() {
                        public void run() {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            if (url != null && url.length() > 0) {
                                // 直接在线流式播放音频，无需下载
                                playSource(url);
                            } else {
                                Toast.makeText(MVPlayerActivity.this, "获取MV地址失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (final Exception e) {
                    handler.post(new Runnable() {
                        public void run() {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Toast.makeText(MVPlayerActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    protected void onDestroy() {
        super.onDestroy();
        try {
            releasePlayer();
        } catch (Exception e) {
            // ignore
        }
    }
}
