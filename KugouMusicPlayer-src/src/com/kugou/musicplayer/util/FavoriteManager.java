package com.kugou.musicplayer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.kugou.musicplayer.model.Song;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 收藏管理（使用SharedPreferences本地存储）
 * 对应原APK中的 yzone.url.songcollect / yzone.url.songfavorite 接口
 * 由于收藏需要登录态，这里使用本地存储实现离线收藏
 */
public class FavoriteManager {
    private static final String TAG = "FavoriteManager";
    private static final String PREF_NAME = "kugou_favorite";
    private static final String KEY_FAVORITES = "favorite_songs";

    private static FavoriteManager instance;
    private SharedPreferences prefs;
    private List favoriteList;

    private FavoriteManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        favoriteList = loadFavorites();
    }

    public static synchronized FavoriteManager getInstance(Context context) {
        if (instance == null) {
            instance = new FavoriteManager(context);
        }
        return instance;
    }

    /**
     * 从SharedPreferences加载收藏列表
     */
    private List loadFavorites() {
        List list = new ArrayList();
        String jsonStr = prefs.getString(KEY_FAVORITES, null);
        if (!TextUtils.isEmpty(jsonStr)) {
            try {
                JSONArray arr = new JSONArray(jsonStr);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    Song song = new Song();
                    song.setHash(obj.optString("hash", ""));
                    song.setSongname(obj.optString("songname", ""));
                    song.setSingername(obj.optString("singername", ""));
                    song.setAlbumId(obj.optString("albumId", ""));
                    song.setDuration(obj.optInt("duration", 0));
                    list.add(song);
                }
            } catch (Exception e) {
                Log.e(TAG, "loadFavorites error", e);
            }
        }
        return list;
    }

    /**
     * 保存收藏列表到SharedPreferences
     */
    private void saveFavorites() {
        try {
            JSONArray arr = new JSONArray();
            for (int i = 0; i < favoriteList.size(); i++) {
                Song song = (Song) favoriteList.get(i);
                JSONObject obj = new JSONObject();
                obj.put("hash", song.getHash());
                obj.put("songname", song.getSongname());
                obj.put("singername", song.getSingername());
                obj.put("albumId", song.getAlbumId());
                obj.put("duration", song.getDuration());
                arr.put(obj);
            }
            prefs.edit().putString(KEY_FAVORITES, arr.toString()).commit();
        } catch (Exception e) {
            Log.e(TAG, "saveFavorites error", e);
        }
    }

    /**
     * 添加收藏
     */
    public boolean addFavorite(Song song) {
        if (song == null || TextUtils.isEmpty(song.getHash())) {
            return false;
        }
        if (isFavorite(song.getHash())) {
            return false;
        }
        favoriteList.add(0, song);
        saveFavorites();
        return true;
    }

    /**
     * 移除收藏
     */
    public boolean removeFavorite(String hash) {
        if (TextUtils.isEmpty(hash)) {
            return false;
        }
        for (int i = 0; i < favoriteList.size(); i++) {
            Song song = (Song) favoriteList.get(i);
            if (hash.equals(song.getHash())) {
                favoriteList.remove(i);
                saveFavorites();
                return true;
            }
        }
        return false;
    }

    /**
     * 切换收藏状态
     */
    public boolean toggleFavorite(Song song) {
        if (song == null) return false;
        if (isFavorite(song.getHash())) {
            removeFavorite(song.getHash());
            return false;
        } else {
            addFavorite(song);
            return true;
        }
    }

    /**
     * 判断是否已收藏
     */
    public boolean isFavorite(String hash) {
        if (TextUtils.isEmpty(hash)) return false;
        for (int i = 0; i < favoriteList.size(); i++) {
            Song song = (Song) favoriteList.get(i);
            if (hash.equals(song.getHash())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取收藏列表
     */
    public List getFavorites() {
        return new ArrayList(favoriteList);
    }

    /**
     * 获取收藏数量
     */
    public int getFavoriteCount() {
        return favoriteList.size();
    }

    /**
     * 清空收藏
     */
    public void clearFavorites() {
        favoriteList.clear();
        saveFavorites();
    }
}
