package com.kugou.musicplayer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.kugou.musicplayer.adapter.MVAdapter;
import com.kugou.musicplayer.adapter.SongAdapter;
import com.kugou.musicplayer.api.KugouApi;
import com.kugou.musicplayer.model.MV;
import com.kugou.musicplayer.model.Song;
import com.kugou.musicplayer.util.DownloadManager;
import com.kugou.musicplayer.util.FavoriteManager;
import com.kugou.musicplayer.util.MiniPlayer;
import com.kugou.musicplayer.util.SearchHistoryManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索界面 - 同时搜索音乐和MV，保留搜索历史
 * API: mobilecdn.kugou.com/api/v3/search/song 和 /api/v3/search/mv
 */
public class SearchActivity extends Activity {

    private EditText etSearch;
    private Button btnSearch;
    private RadioGroup rgSearchType;
    private ListView lvResults, lvHistory;
    private TextView tvEmpty, tvClearHistory;
    private View llHistory, llResult;
    private ProgressDialog progressDialog;

    private SongAdapter songAdapter;
    private MVAdapter mvAdapter;
    private ArrayAdapter historyAdapter;
    private List songList;
    private List mvList;
    private Handler handler;
    private String searchType = "song";
    private boolean hasSearched = false;
    private MiniPlayer miniPlayer;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_search);
            handler = new Handler(Looper.getMainLooper());
            songList = new ArrayList();
            mvList = new ArrayList();
            initViews();
            setupListeners();
            loadHistory();
            // 底部迷你播放栏
            miniPlayer = new MiniPlayer(this);
        } catch (Exception e) {
            Toast.makeText(this, "搜索初始化失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        etSearch = (EditText) findViewById(R.id.etSearch);
        btnSearch = (Button) findViewById(R.id.btnSearch);
        rgSearchType = (RadioGroup) findViewById(R.id.rgSearchType);
        lvResults = (ListView) findViewById(R.id.lvResults);
        lvHistory = (ListView) findViewById(R.id.lvHistory);
        tvEmpty = (TextView) findViewById(R.id.tvEmpty);
        tvClearHistory = (TextView) findViewById(R.id.tvClearHistory);
        llHistory = findViewById(R.id.llHistory);
        llResult = findViewById(R.id.llResult);

        songAdapter = new SongAdapter(this, songList);
        mvAdapter = new MVAdapter(this, mvList);
        lvResults.setAdapter(songAdapter);

        // 搜索历史Adapter
        historyAdapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_1,
                SearchHistoryManager.getInstance(this).getHistory()) {
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view.findViewById(android.R.id.text1);
                tv.setTextColor(0xFFCCCCCC);
                tv.setTextSize(14);
                tv.setPadding(20, 16, 20, 16);
                return view;
            }
        };
        lvHistory.setAdapter(historyAdapter);
    }

    private void setupListeners() {
        btnSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doSearch();
            }
        });

        etSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    doSearch();
                    return true;
                }
                return false;
            }
        });

        rgSearchType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbMusic) {
                    searchType = "song";
                    lvResults.setAdapter(songAdapter);
                } else {
                    searchType = "mv";
                    lvResults.setAdapter(mvAdapter);
                }
                updateEmptyView();
            }
        });

        // 搜索历史点击
        lvHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String keyword = (String) historyAdapter.getItem(position);
                etSearch.setText(keyword);
                etSearch.setSelection(keyword.length());
                doSearch();
            }
        });

        // 清空历史
        tvClearHistory.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SearchHistoryManager.getInstance(SearchActivity.this).clearHistory();
                loadHistory();
                Toast.makeText(SearchActivity.this, "搜索历史已清空", Toast.LENGTH_SHORT).show();
            }
        });

        songAdapter.setOnSongActionListener(new SongAdapter.OnSongActionListener() {
            public void onPlay(Song song, int position) {
                Intent intent = new Intent(SearchActivity.this, PlayerActivity.class);
                intent.putExtra("song", song);
                intent.putExtra("playlist", new ArrayList(songList));
                intent.putExtra("position", position);
                startActivity(intent);
            }

            public void onFavorite(Song song) {
                boolean added = FavoriteManager.getInstance(SearchActivity.this).toggleFavorite(song);
                Toast.makeText(SearchActivity.this,
                        added ? R.string.favorite_added : R.string.favorite_removed,
                        Toast.LENGTH_SHORT).show();
            }

            public void onDownload(Song song) {
                DownloadManager.getInstance(SearchActivity.this).downloadSong(song);
                Toast.makeText(SearchActivity.this, R.string.download_start, Toast.LENGTH_SHORT).show();
            }
        });

        mvAdapter.setOnMVActionListener(new MVAdapter.OnMVActionListener() {
            public void onPlayMV(MV mv) {
                Intent intent = new Intent(SearchActivity.this, MVPlayerActivity.class);
                intent.putExtra("mv", mv);
                startActivity(intent);
            }

            public void onDownloadMV(MV mv) {
                DownloadManager.getInstance(SearchActivity.this).downloadMV(mv);
                Toast.makeText(SearchActivity.this, R.string.download_start, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 加载搜索历史
     */
    private void loadHistory() {
        List history = SearchHistoryManager.getInstance(this).getHistory();
        historyAdapter.clear();
        for (int i = 0; i < history.size(); i++) {
            historyAdapter.add(history.get(i));
        }
        historyAdapter.notifyDataSetChanged();

        if (history.isEmpty()) {
            tvClearHistory.setVisibility(View.GONE);
        } else {
            tvClearHistory.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 同时搜索音乐和MV
     */
    private void doSearch() {
        final String keyword = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) {
            Toast.makeText(this, R.string.search_hint, Toast.LENGTH_SHORT).show();
            return;
        }

        // 保存搜索历史
        SearchHistoryManager.getInstance(this).addHistory(keyword);
        loadHistory();

        // 切换到结果视图
        llHistory.setVisibility(View.GONE);
        llResult.setVisibility(View.VISIBLE);
        hasSearched = true;

        progressDialog = ProgressDialog.show(this, "", getString(R.string.searching), true, false);

        // 同时搜索音乐和MV（两个线程并行）
        new Thread(new Runnable() {
            public void run() {
                try {
                    // 搜索音乐
                    final List songResults = KugouApi.getInstance().searchSongs(keyword, 1, 20);
                    handler.post(new Runnable() {
                        public void run() {
                            songList.clear();
                            if (songResults != null) {
                                songList.addAll(songResults);
                            }
                            songAdapter.notifyDataSetChanged();
                            updateEmptyView();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(new Runnable() {
            public void run() {
                try {
                    // 搜索MV
                    final List mvResults = KugouApi.getInstance().searchMV(keyword, 1, 20);
                    handler.post(new Runnable() {
                        public void run() {
                            mvList.clear();
                            if (mvResults != null) {
                                mvList.addAll(mvResults);
                            }
                            mvAdapter.notifyDataSetChanged();
                            updateEmptyView();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            if (progressDialog != null && progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                        }
                    }, 500);
                }
            }
        }).start();
    }

    private void updateEmptyView() {
        if (!hasSearched) return;
        int count = "song".equals(searchType) ? songList.size() : mvList.size();
        if (count == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            lvResults.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            lvResults.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 返回键：如果在结果视图，回到历史视图
     */
    public void onBackPressed() {
        if (hasSearched && llResult.getVisibility() == View.VISIBLE) {
            llResult.setVisibility(View.GONE);
            llHistory.setVisibility(View.VISIBLE);
            hasSearched = false;
            etSearch.setText("");
        } else {
            super.onBackPressed();
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        if (miniPlayer != null) {
            miniPlayer.detach();
        }
    }
}
