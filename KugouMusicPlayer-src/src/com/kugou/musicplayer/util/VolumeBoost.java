package com.kugou.musicplayer.util;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

/**
 * 音量增强管理器
 *
 * 原APK实现: 通过JNI调用 libkgmedia.so 中的 native_setVolumeBoost() 方法
 * 类路径: com.kugou.framework.player 中的 VolumeBoost
 *
 * 本应用兼容方案:
 * - Android 2.3 不支持 AudioEffect / Visualizer 等高级音频API
 * - 使用 AudioManager.setStreamVolume 将系统媒体音量最大化
 * - 同时设置 MediaPlayer 的 volume 为最大值
 * - 提供软件增益提示
 */
public class VolumeBoost {
    private static final String TAG = "VolumeBoost";

    private static VolumeBoost instance;
    private Context context;
    private AudioManager audioManager;
    private boolean boostEnabled;
    private int originalVolume;
    private int maxVolume;

    private VolumeBoost(Context context) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
        this.boostEnabled = false;
        this.maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public static synchronized VolumeBoost getInstance(Context context) {
        if (instance == null) {
            instance = new VolumeBoost(context);
        }
        return instance;
    }

    /**
     * 开启音量增强
     * 原APK: native_setVolumeBoost(true) → libkgmedia.so
     * 兼容方案: 系统音量最大化
     */
    public boolean enableBoost() {
        if (boostEnabled) {
            return true;
        }
        try {
            // 保存原始音量
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            // 设置为最大音量
            audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    maxVolume,
                    AudioManager.FLAG_SHOW_UI);
            boostEnabled = true;
            Log.d(TAG, "Volume boost enabled, max volume: " + maxVolume);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "enableBoost error", e);
            return false;
        }
    }

    /**
     * 关闭音量增强
     */
    public boolean disableBoost() {
        if (!boostEnabled) {
            return true;
        }
        try {
            // 恢复原始音量
            audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    originalVolume,
                    AudioManager.FLAG_SHOW_UI);
            boostEnabled = false;
            Log.d(TAG, "Volume boost disabled, restored volume: " + originalVolume);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "disableBoost error", e);
            return false;
        }
    }

    /**
     * 切换音量增强状态
     */
    public boolean toggleBoost() {
        if (boostEnabled) {
            disableBoost();
            return false;
        } else {
            enableBoost();
            return true;
        }
    }

    public boolean isBoostEnabled() {
        return boostEnabled;
    }

    /**
     * 获取当前音量（0-100）
     */
    public int getCurrentVolume() {
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        return maxVolume > 0 ? (current * 100 / maxVolume) : 0;
    }

    /**
     * 设置音量（0-100）
     */
    public void setVolume(int percent) {
        if (boostEnabled) {
            // 增强模式下保持最大音量
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
            return;
        }
        int target = percent * maxVolume / 100;
        target = Math.max(0, Math.min(maxVolume, target));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0);
    }

    /**
     * 获取最大音量
     */
    public int getMaxVolume() {
        return maxVolume;
    }
}
