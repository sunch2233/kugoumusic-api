package com.kugou.musicplayer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.kugou.musicplayer.model.Song;
import com.kugou.musicplayer.util.DownloadManager;
import com.kugou.musicplayer.util.MiniPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 下载管理界面
 * 扫描 /sdcard/KugouMusic/music/ 和 /sdcard/KugouMusic/mv/ 目录
 * 点击音乐直接播放，点击MV用系统播放器打开
 */
public class DownloadActivity extends Activity {

    private ListView lvDownload;
    private TextView tvDlEmpty, tvDlPath;
    private RadioGroup rgDlType;
    private ArrayAdapter fileAdapter;
    private List fileList;
    private Handler handler;
    private String currentType = "music";
    private boolean isRunning = true;
    private MiniPlayer miniPlayer;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_download);
            handler = new Handler(Looper.getMainLooper());
            fileList = new ArrayList();
            initViews();
            setupListeners();
            loadFiles();
            startAutoRefresh();
            // 底部迷你播放栏
            miniPlayer = new MiniPlayer(this);
        } catch (Exception e) {
            Toast.makeText(this, "下载管理初始化失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        lvDownload = (ListView) findViewById(R.id.lvDownload);
        tvDlEmpty = (TextView) findViewById(R.id.tvDlEmpty);
        tvDlPath = (TextView) findViewById(R.id.tvDlPath);
        rgDlType = (RadioGroup) findViewById(R.id.rgDlType);

        // 显示下载目录
        String musicDir = DownloadManager.getInstance(this).getMusicDir();
        tvDlPath.setText(musicDir);

        fileAdapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                fileList) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                File file = (File) fileList.get(position);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                text1.setText(file.getName());
                text1.setTextColor(0xFFEEEEEE);
                text1.setTextSize(14);
                String size = formatFileSize(file.length());
                text2.setText(size + "  |  点击播放");
                text2.setTextColor(0xFF999999);
                text2.setTextSize(11);
                return view;
            }
        };
        lvDownload.setAdapter(fileAdapter);
    }

    private void setupListeners() {
        rgDlType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbDlMusic) {
                    currentType = "music";
                    tvDlPath.setText(DownloadManager.getInstance(DownloadActivity.this).getMusicDir());
                } else {
                    currentType = "mv";
                    tvDlPath.setText(DownloadManager.getInstance(DownloadActivity.this).getMVDir());
                }
                loadFiles();
            }
        });

        lvDownload.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File file = (File) fileList.get(position);
                if (!file.exists()) {
                    Toast.makeText(DownloadActivity.this, "文件不存在", Toast.LENGTH_SHORT).show();
                    loadFiles();
                    return;
                }

                if ("music".equals(currentType)) {
                    // 音乐：用内置播放器播放
                    playLocalMusic(file);
                } else {
                    // MV：用系统播放器打开（MKV格式内置播放器不支持视频）
                    playWithSystemPlayer(file);
                }
            }
        });
    }

    /**
     * 播放本地音乐
     */
    private void playLocalMusic(File file) {
        Song song = new Song();
        song.setHash("local_" + file.getAbsolutePath().hashCode());
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            name = name.substring(0, dotIndex);
        }
        song.setSongname(name);
        song.setSingername("本地音乐");
        song.setPlayUrl(file.getAbsolutePath());

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("song", song);
        List playlist = new ArrayList();
        // 添加当前目录所有音乐到播放列表
        for (int i = 0; i < fileList.size(); i++) {
            File f = (File) fileList.get(i);
            Song s = new Song();
            s.setHash("local_" + f.getAbsolutePath().hashCode());
            String n = f.getName();
            int d = n.lastIndexOf('.');
            if (d > 0) n = n.substring(0, d);
            s.setSongname(n);
            s.setSingername("本地音乐");
            s.setPlayUrl(f.getAbsolutePath());
            playlist.add(s);
        }
        intent.putExtra("playlist", new ArrayList(playlist));
        // 找到当前歌曲在列表中的位置
        int pos = 0;
        for (int i = 0; i < playlist.size(); i++) {
            Song s = (Song) playlist.get(i);
            if (s.getPlayUrl().equals(file.getAbsolutePath())) {
                pos = i;
                break;
            }
        }
        intent.putExtra("position", pos);
        startActivity(intent);
    }

    /**
     * 用系统播放器打开MV文件
     */
    private void playWithSystemPlayer(File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(file);
            // 根据扩展名设置MIME类型
            String name = file.getName().toLowerCase();
            if (name.endsWith(".mkv")) {
                intent.setDataAndType(uri, "video/x-matroska");
            } else if (name.endsWith(".mp4")) {
                intent.setDataAndType(uri, "video/mp4");
            } else if (name.endsWith(".avi")) {
                intent.setDataAndType(uri, "video/x-msvideo");
            } else {
                intent.setDataAndType(uri, "video/*");
            }
            startActivity(Intent.createChooser(intent, "选择播放器"));
        } catch (Exception e) {
            Toast.makeText(this, "没有可用的视频播放器，请安装MX Player等支持MKV的播放器", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 加载文件列表
     */
    private void loadFiles() {
        new Thread(new Runnable() {
            public void run() {
                final List files;
                if ("music".equals(currentType)) {
                    files = DownloadManager.getInstance(DownloadActivity.this).scanDownloadedMusic();
                } else {
                    files = DownloadManager.getInstance(DownloadActivity.this).scanDownloadedMV();
                }
                handler.post(new Runnable() {
                    public void run() {
                        fileList.clear();
                        if (files != null) {
                            fileList.addAll(files);
                        }
                        fileAdapter.notifyDataSetChanged();

                        if (fileList.isEmpty()) {
                            tvDlEmpty.setVisibility(View.VISIBLE);
                            lvDownload.setVisibility(View.GONE);
                            if ("music".equals(currentType)) {
                                tvDlEmpty.setText("暂无下载音乐\n去搜索页面下载吧");
                            } else {
                                tvDlEmpty.setText("暂无下载MV\n去搜索页面下载MV吧\n（MKV格式建议用MX Player播放）");
                            }
                        } else {
                            tvDlEmpty.setVisibility(View.GONE);
                            lvDownload.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return (size / 1024) + " KB";
        } else {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
    }

    /**
     * 自动刷新（检测新下载的文件）
     */
    private void startAutoRefresh() {
        new Thread(new Runnable() {
            public void run() {
                while (isRunning) {
                    try {
                        Thread.sleep(3000);
                        handler.post(new Runnable() {
                            public void run() {
                                loadFiles();
                            }
                        });
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }).start();
    }

    protected void onResume() {
        super.onResume();
        loadFiles();
    }

    protected void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (miniPlayer != null) {
            miniPlayer.detach();
        }
    }
}
