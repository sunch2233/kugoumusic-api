package com.kugou.musicplayer.api;

import android.text.TextUtils;
import android.util.Log;

import com.kugou.musicplayer.model.MV;
import com.kugou.musicplayer.model.Song;
import com.kugou.musicplayer.util.HttpUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * 酷狗音乐API封装
 * 接口从酷狗APK逆向提取并验证可用性
 *
 * 已验证可用的API端点：
 * 1. 搜索音乐: mobilecdn.kugou.com/api/v3/search/song
 * 2. 播放地址: m.kugou.com/api/v1/song/get_song_info (backup_url)
 * 3. 搜索MV: mobilecdn.kugou.com/api/v3/search/mv
 * 4. MV地址: m.kugou.com/app/i/mv.php?cmd=100 (mvdata.sd.downurl)
 * 5. 歌曲信息: mobilecdn.kugou.com/api/v3/song/info
 */
public class KugouApi {
    private static final String TAG = "KugouApi";

    // API基础地址（已验证可用）
    private static final String BASE_SEARCH = "http://mobilecdn.kugou.com/api/v3/search";
    private static final String BASE_SONG_INFO = "http://m.kugou.com/api/v1/song";
    private static final String BASE_MV_INFO = "http://m.kugou.com/app/i";
    private static final String BASE_SONG_DETAIL = "http://mobilecdn.kugou.com/api/v3/song";

    // 单例
    private static KugouApi instance;

    private KugouApi() {
    }

    public static synchronized KugouApi getInstance() {
        if (instance == null) {
            instance = new KugouApi();
        }
        return instance;
    }

    /**
     * 搜索音乐
     * API: GET /api/v3/search/song?keyword=xxx&page=1&pagesize=20
     * 返回: data.info[] - hash, songname, singername, album_id, duration, extname
     */
    public List searchSongs(String keyword, int page, int pagesize) {
        List result = new ArrayList();
        if (TextUtils.isEmpty(keyword)) {
            return result;
        }
        try {
            String url = BASE_SEARCH + "/song"
                    + "?keyword=" + URLEncoder.encode(keyword, "UTF-8")
                    + "&page=" + page
                    + "&pagesize=" + pagesize;

            Log.d(TAG, "searchSongs url: " + url);
            String response = HttpUtil.doGet(url);
            if (!TextUtils.isEmpty(response)) {
                result = parseSongSearchResult(response);
            }
        } catch (Exception e) {
            Log.e(TAG, "searchSongs error", e);
        }
        return result;
    }

    /**
     * 搜索MV
     * API: GET /api/v3/search/mv?keyword=xxx&page=1&pagesize=20
     * 返回: data.info[] - hash, filename, singername, duration, imgurl
     */
    public List searchMV(String keyword, int page, int pagesize) {
        List result = new ArrayList();
        if (TextUtils.isEmpty(keyword)) {
            return result;
        }
        try {
            String url = BASE_SEARCH + "/mv"
                    + "?keyword=" + URLEncoder.encode(keyword, "UTF-8")
                    + "&page=" + page
                    + "&pagesize=" + pagesize;

            Log.d(TAG, "searchMV url: " + url);
            String response = HttpUtil.doGet(url);
            if (!TextUtils.isEmpty(response)) {
                result = parseMVSearchResult(response);
            }
        } catch (Exception e) {
            Log.e(TAG, "searchMV error", e);
        }
        return result;
    }

    /**
     * 获取歌曲播放地址
     * API: GET m.kugou.com/api/v1/song/get_song_info?hash=xxx&cmd=playInfo
     * 返回: backup_url[] - 可直接播放的音频URL
     */
    public String getPlayUrl(String hash, String ext) {
        if (TextUtils.isEmpty(hash)) {
            return null;
        }
        try {
            String url = BASE_SONG_INFO + "/get_song_info"
                    + "?hash=" + hash
                    + "&cmd=playInfo";

            Log.d(TAG, "getPlayUrl url: " + url);
            String response = HttpUtil.doGet(url);
            if (!TextUtils.isEmpty(response)) {
                return parsePlayUrl(response);
            }
        } catch (Exception e) {
            Log.e(TAG, "getPlayUrl error", e);
        }
        return null;
    }

    /**
     * 获取歌曲详细信息
     * API: GET /api/v3/song/info?hash=xxx
     */
    public Song getSongInfo(String hash) {
        if (TextUtils.isEmpty(hash)) {
            return null;
        }
        try {
            String url = BASE_SONG_DETAIL + "/info?hash=" + hash;
            Log.d(TAG, "getSongInfo url: " + url);
            String response = HttpUtil.doGet(url);
            if (!TextUtils.isEmpty(response)) {
                return parseSongInfo(response, hash);
            }
        } catch (Exception e) {
            Log.e(TAG, "getSongInfo error", e);
        }
        return null;
    }

    /**
     * 获取MV播放地址
     * API: GET m.kugou.com/app/i/mv.php?cmd=100&hash=xxx
     * 返回: mvdata.sd.downurl 或 mvdata.hd.downurl
     */
    public String getMVPlayUrl(String mvhash, String path) {
        if (TextUtils.isEmpty(mvhash)) {
            return null;
        }
        try {
            String url = BASE_MV_INFO + "/mv.php"
                    + "?cmd=100"
                    + "&hash=" + mvhash;

            Log.d(TAG, "getMVPlayUrl url: " + url);
            String response = HttpUtil.doGet(url);
            if (!TextUtils.isEmpty(response)) {
                return parseMVPlayUrl(response);
            }
        } catch (Exception e) {
            Log.e(TAG, "getMVPlayUrl error", e);
        }
        return null;
    }

    /**
     * 获取歌词
     * API: GET mobilecdn.kugou.com/new/app/i/krc.php?cmd=200
     */
    public String getLyric(String songName, String hash, int timelength) {
        try {
            String url = "http://mobilecdn.kugou.com/new/app/i/krc.php"
                    + "?cmd=200"
                    + "&songName=" + URLEncoder.encode(songName, "UTF-8")
                    + "&hash=" + hash
                    + "&timelength=" + timelength;

            Log.d(TAG, "getLyric url: " + url);
            return HttpUtil.doGet(url);
        } catch (Exception e) {
            Log.e(TAG, "getLyric error", e);
        }
        return null;
    }

    // ===== JSON解析方法 =====

    /**
     * 解析搜索音乐结果
     * 格式: {"status":1,"data":{"info":[{hash, songname, singername, album_id, duration, extname, bitrate}]}}
     */
    private List parseSongSearchResult(String response) {
        List list = new ArrayList();
        try {
            JSONObject json = new JSONObject(response);
            int status = json.optInt("status", 0);
            if (status != 1) {
                Log.w(TAG, "search status != 1: " + response);
                return list;
            }
            JSONObject data = json.optJSONObject("data");
            if (data == null) {
                return list;
            }
            JSONArray arr = data.optJSONArray("info");
            if (arr == null) {
                return list;
            }
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Song song = new Song();
                song.setHash(obj.optString("hash", ""));
                song.setSongname(obj.optString("songname", ""));
                song.setSingername(obj.optString("singername", ""));
                song.setAlbumId(obj.optString("album_id", ""));
                song.setAlbumName(obj.optString("album_name", ""));
                song.setDuration(obj.optInt("duration", 0));
                song.setFilesize(obj.optLong("filesize", 0));
                song.setExt(obj.optString("extname", "mp3"));
                song.setBitrate(obj.optInt("bitrate", 0));
                list.add(song);
            }
        } catch (Exception e) {
            Log.e(TAG, "parseSongSearchResult error", e);
        }
        return list;
    }

    /**
     * 解析搜索MV结果
     * 格式: {"status":1,"data":{"info":[{hash, filename, singername, duration, imgurl}]}}
     */
    private List parseMVSearchResult(String response) {
        List list = new ArrayList();
        try {
            JSONObject json = new JSONObject(response);
            int status = json.optInt("status", 0);
            if (status != 1) {
                Log.w(TAG, "mv search status != 1");
                return list;
            }
            JSONObject data = json.optJSONObject("data");
            if (data == null) {
                return list;
            }
            JSONArray arr = data.optJSONArray("info");
            if (arr == null) {
                return list;
            }
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                MV mv = new MV();
                mv.setMvhash(obj.optString("hash", ""));
                mv.setTitle(obj.optString("filename", ""));
                mv.setSinger(obj.optString("singername", ""));
                mv.setImgUrl(obj.optString("imgurl", ""));
                mv.setDuration(obj.optInt("duration", 0));
                list.add(mv);
            }
        } catch (Exception e) {
            Log.e(TAG, "parseMVSearchResult error", e);
        }
        return list;
    }

    /**
     * 解析播放地址
     * 格式: {"backup_url":["https://sharefs.tx.kugou.com/...mp3"], ...}
     */
    private String parsePlayUrl(String response) {
        try {
            JSONObject json = new JSONObject(response);
            int status = json.optInt("status", 0);
            if (status != 1) {
                Log.w(TAG, "play url status != 1: " + response);
                return null;
            }
            // 优先取 backup_url
            JSONArray backupUrls = json.optJSONArray("backup_url");
            if (backupUrls != null && backupUrls.length() > 0) {
                String url = backupUrls.optString(0, "");
                if (!TextUtils.isEmpty(url)) {
                    return url;
                }
            }
            // 备用: url字段
            String url = json.optString("url", "");
            if (!TextUtils.isEmpty(url)) {
                return url;
            }
            // 备用: play_url字段
            url = json.optString("play_url", "");
            return url;
        } catch (Exception e) {
            Log.e(TAG, "parsePlayUrl error", e);
        }
        return null;
    }

    /**
     * 解析歌曲信息
     * 格式: {"data":{"filename","songname","singername","timeLength","bitrate",...}}
     */
    private Song parseSongInfo(String response, String hash) {
        try {
            JSONObject json = new JSONObject(response);
            JSONObject data = json.optJSONObject("data");
            if (data == null) {
                return null;
            }
            Song song = new Song();
            song.setHash(hash);
            song.setSongname(data.optString("songname", ""));
            song.setSingername(data.optString("singername", ""));
            song.setAlbumName(data.optString("album_name", ""));
            song.setDuration(data.optInt("duration", 0));
            song.setFilesize(data.optLong("filesize", 0));
            song.setExt(data.optString("extname", "mp3"));
            song.setBitrate(data.optInt("bitrate", 0));
            return song;
        } catch (Exception e) {
            Log.e(TAG, "parseSongInfo error", e);
        }
        return null;
    }

    /**
     * 解析MV播放地址
     * 格式: {"mvdata":{"sd":{"downurl":"http://...","backupdownurl":[...]}, "hd":{...}}}
     */
    private String parseMVPlayUrl(String response) {
        try {
            JSONObject json = new JSONObject(response);
            JSONObject mvdata = json.optJSONObject("mvdata");
            if (mvdata == null) {
                return null;
            }
            // 优先取标清
            JSONObject sd = mvdata.optJSONObject("sd");
            if (sd != null) {
                String url = sd.optString("downurl", "");
                if (!TextUtils.isEmpty(url)) {
                    return url;
                }
                JSONArray backup = sd.optJSONArray("backupdownurl");
                if (backup != null && backup.length() > 0) {
                    return backup.optString(0, "");
                }
            }
            // 备用: 高清
            JSONObject hd = mvdata.optJSONObject("hd");
            if (hd != null) {
                String url = hd.optString("downurl", "");
                if (!TextUtils.isEmpty(url)) {
                    return url;
                }
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "parseMVPlayUrl error", e);
        }
        return null;
    }
}
