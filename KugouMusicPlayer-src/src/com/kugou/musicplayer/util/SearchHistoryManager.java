package com.kugou.musicplayer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索历史管理
 * 使用SharedPreferences保存搜索记录
 */
public class SearchHistoryManager {
    private static final String TAG = "SearchHistoryManager";
    private static final String PREF_NAME = "kugou_search_history";
    private static final String KEY_HISTORY = "history";
    private static final int MAX_HISTORY = 20;

    private static SearchHistoryManager instance;
    private SharedPreferences prefs;
    private List historyList;

    private SearchHistoryManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        historyList = loadHistory();
    }

    public static synchronized SearchHistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new SearchHistoryManager(context);
        }
        return instance;
    }

    /**
     * 加载搜索历史
     */
    private List loadHistory() {
        List list = new ArrayList();
        String jsonStr = prefs.getString(KEY_HISTORY, null);
        if (!TextUtils.isEmpty(jsonStr)) {
            try {
                JSONArray arr = new JSONArray(jsonStr);
                for (int i = 0; i < arr.length(); i++) {
                    list.add(arr.getString(i));
                }
            } catch (Exception e) {
                Log.e(TAG, "loadHistory error", e);
            }
        }
        return list;
    }

    /**
     * 保存搜索历史
     */
    private void saveHistory() {
        try {
            JSONArray arr = new JSONArray();
            for (int i = 0; i < historyList.size(); i++) {
                arr.put(historyList.get(i));
            }
            prefs.edit().putString(KEY_HISTORY, arr.toString()).commit();
        } catch (Exception e) {
            Log.e(TAG, "saveHistory error", e);
        }
    }

    /**
     * 添加搜索记录
     */
    public void addHistory(String keyword) {
        if (TextUtils.isEmpty(keyword)) return;
        String kw = keyword.trim();
        if (kw.length() == 0) return;

        // 去重：如果已存在则先移除
        for (int i = 0; i < historyList.size(); i++) {
            if (kw.equals(historyList.get(i))) {
                historyList.remove(i);
                break;
            }
        }
        // 添加到最前面
        historyList.add(0, kw);
        // 限制最大数量
        while (historyList.size() > MAX_HISTORY) {
            historyList.remove(historyList.size() - 1);
        }
        saveHistory();
    }

    /**
     * 获取搜索历史
     */
    public List getHistory() {
        return new ArrayList(historyList);
    }

    /**
     * 清除搜索历史
     */
    public void clearHistory() {
        historyList.clear();
        saveHistory();
    }

    /**
     * 删除单条历史
     */
    public void removeHistory(String keyword) {
        for (int i = 0; i < historyList.size(); i++) {
            if (keyword.equals(historyList.get(i))) {
                historyList.remove(i);
                saveHistory();
                break;
            }
        }
    }

    /**
     * 获取历史数量
     */
    public int getHistoryCount() {
        return historyList.size();
    }
}
