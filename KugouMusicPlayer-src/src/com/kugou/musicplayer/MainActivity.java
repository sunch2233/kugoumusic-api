package com.kugou.musicplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.kugou.musicplayer.adapter.MenuAdapter;
import com.kugou.musicplayer.util.MiniPlayer;

/**
 * 主界面 - 功能导航
 */
public class MainActivity extends Activity {

    private GridView gridMenu;
    private MiniPlayer miniPlayer;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 底部迷你播放栏
        miniPlayer = new MiniPlayer(this);

        gridMenu = (GridView) findViewById(R.id.gridMenu);
        gridMenu.setAdapter(new MenuAdapter(this));

        gridMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        startActivity(new Intent(MainActivity.this, SearchActivity.class));
                        break;
                    case 1:
                        startActivity(new Intent(MainActivity.this, PlayerActivity.class));
                        break;
                    case 2:
                        startActivity(new Intent(MainActivity.this, MVPlayerActivity.class));
                        break;
                    case 3:
                        startActivity(new Intent(MainActivity.this, FavoriteActivity.class));
                        break;
                    case 4:
                        startActivity(new Intent(MainActivity.this, DownloadActivity.class));
                        break;
                    case 5:
                        startActivity(new Intent(MainActivity.this, ApiDocActivity.class));
                        break;
                }
            }
        });
    }

    protected void onDestroy() {
        super.onDestroy();
        if (miniPlayer != null) {
            miniPlayer.detach();
        }
    }
}
