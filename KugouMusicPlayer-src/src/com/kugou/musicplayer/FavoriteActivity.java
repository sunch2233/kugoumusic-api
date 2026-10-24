package com.kugou.musicplayer;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.kugou.musicplayer.adapter.SongAdapter;
import com.kugou.musicplayer.model.Song;
import com.kugou.musicplayer.util.DownloadManager;
import com.kugou.musicplayer.util.FavoriteManager;
import com.kugou.musicplayer.util.MiniPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 收藏列表界面
 * 对应原APK中的 yzone.url.songcollect / yzone.url.songfavorite
 * 使用SharedPreferences本地存储实现
 */
public class FavoriteActivity extends Activity {

    private ListView lvFavorite;
    private TextView tvFavCount, tvFavEmpty;
    private Button btnClearFav;
    private SongAdapter adapter;
    private List favoriteList;
    private MiniPlayer miniPlayer;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        initViews();
        setupListeners();
        loadFavorites();
        // 底部迷你播放栏
        miniPlayer = new MiniPlayer(this);
    }

    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void initViews() {
        lvFavorite = (ListView) findViewById(R.id.lvFavorite);
        tvFavCount = (TextView) findViewById(R.id.tvFavCount);
        tvFavEmpty = (TextView) findViewById(R.id.tvFavEmpty);
        btnClearFav = (Button) findViewById(R.id.btnClearFav);

        favoriteList = new ArrayList();
        adapter = new SongAdapter(this, favoriteList);
        lvFavorite.setAdapter(adapter);
    }

    private void setupListeners() {
        btnClearFav.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (favoriteList.isEmpty()) return;
                new android.app.AlertDialog.Builder(FavoriteActivity.this)
                        .setMessage(R.string.confirm_clear_favorite)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                FavoriteManager.getInstance(FavoriteActivity.this).clearFavorites();
                                loadFavorites();
                                Toast.makeText(FavoriteActivity.this, "收藏已清空", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });

        adapter.setOnSongActionListener(new SongAdapter.OnSongActionListener() {
            public void onPlay(Song song, int position) {
                Intent intent = new Intent(FavoriteActivity.this, PlayerActivity.class);
                intent.putExtra("song", song);
                intent.putExtra("playlist", new ArrayList(favoriteList));
                intent.putExtra("position", position);
                startActivity(intent);
            }

            public void onFavorite(Song song) {
                FavoriteManager.getInstance(FavoriteActivity.this).toggleFavorite(song);
                loadFavorites();
            }

            public void onDownload(Song song) {
                DownloadManager.getInstance(FavoriteActivity.this).downloadSong(song);
                Toast.makeText(FavoriteActivity.this, R.string.download_start, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFavorites() {
        favoriteList.clear();
        List list = FavoriteManager.getInstance(this).getFavorites();
        if (list != null) {
            favoriteList.addAll(list);
        }
        adapter.notifyDataSetChanged();

        int count = favoriteList.size();
        tvFavCount.setText(getString(R.string.total_favorite, count));

        if (count == 0) {
            tvFavEmpty.setVisibility(View.VISIBLE);
            lvFavorite.setVisibility(View.GONE);
        } else {
            tvFavEmpty.setVisibility(View.GONE);
            lvFavorite.setVisibility(View.VISIBLE);
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        if (miniPlayer != null) {
            miniPlayer.detach();
        }
    }
}
