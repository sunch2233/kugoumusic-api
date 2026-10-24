package com.kugou.musicplayer.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * HTTP工具类（兼容Android 2.3，使用HttpURLConnection）
 */
public class HttpUtil {
    private static final String TAG = "HttpUtil";
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 15000;

    private HttpUtil() {
    }

    /**
     * 发送GET请求
     */
    public static String doGet(String urlStr) {
        HttpURLConnection conn = null;
        InputStream is = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setRequestProperty("User-Agent", "KugouMusic/6.0 Android");
            conn.setRequestProperty("Accept", "*/*");

            int code = conn.getResponseCode();
            if (code == 200) {
                is = conn.getInputStream();
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuffer sb = new StringBuffer();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            } else {
                Log.e(TAG, "HTTP error code: " + code + " url: " + urlStr);
            }
        } catch (Exception e) {
            Log.e(TAG, "doGet error: " + urlStr, e);
        } finally {
            try {
                if (reader != null) reader.close();
                if (is != null) is.close();
                if (conn != null) conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "close error", e);
            }
        }
        return null;
    }

    /**
     * 下载文件到指定路径
     */
    public static boolean downloadFile(String urlStr, String savePath, DownloadProgressListener listener) {
        HttpURLConnection conn = null;
        InputStream is = null;
        java.io.FileOutputStream fos = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(30000);

            int code = conn.getResponseCode();
            if (code == 200) {
                int total = conn.getContentLength();
                is = conn.getInputStream();
                fos = new java.io.FileOutputStream(savePath);
                byte[] buffer = new byte[4096];
                int len;
                int downloaded = 0;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                    downloaded += len;
                    if (listener != null && total > 0) {
                        listener.onProgress(downloaded, total);
                    }
                }
                fos.flush();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "downloadFile error", e);
        } finally {
            try {
                if (fos != null) fos.close();
                if (is != null) is.close();
                if (conn != null) conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "close error", e);
            }
        }
        return false;
    }

    /**
     * 下载进度回调接口
     */
    public interface DownloadProgressListener {
        void onProgress(int downloaded, int total);
    }
}
