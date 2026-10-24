# 酷狗音乐播放器（API 提取版）

一个基于酷狗音乐公开 Web 接口、纯手写 Android 原生实现的音乐播放器。**无 Gradle、无第三方库、无原生库（.so）**，APK 仅约 82KB，兼容 Android 2.3（API 9）+。

> 本项目仅用于学习与个人娱乐，接口与内容版权归酷狗音乐所有，请勿用于商业用途。

---

## 功能特性

| 功能 | 说明 |
|---|---|
| 搜索音乐 | 酷狗搜索接口，返回歌曲名 / 歌手 / 时长 / hash |
| 搜索 MV | 音乐与 MV 同时搜索，一键切换结果标签 |
| 在线播放 | 搜索即播，无需下载；播放服务常驻，切页不断歌 |
| 播放控制 | 播放 / 暂停 / 上一首 / 下一首 / 进度拖动 |
| 一键增强音量 | 软件增益 + 系统音量最大化，可在播放页音量条旁开启 |
| 收藏音乐 | 本地持久化收藏列表，可清空 |
| 下载音乐 / MV | 后台下载，下载列表管理，自动扫描本地音频 / MV |
| 搜索历史 | 本地保存，点击直达，可清空 |
| 本地音乐 | 自动扫描手机内全部音频文件并播放 |
| 底部迷你播放栏 | 酷狗式常驻底栏：无播放时显示"酷狗音乐"占位，播放后显示歌名 / 歌手，可点播控制 |
| MV 播放 | MV 为 MKV 视频格式，本应用仅解出音频轨在线收听（无画面），保留下载功能 |
| API 文档页 | 应用内可查看本项目使用的全部接口 |

---

## 技术栈与环境

- **语言**：Java 6 语法（`-source 1.6 -target 1.6`），Android SDK 28 编译
- **最低系统**：Android 2.3（minSdk 9），targetSdk 10
- **构建工具**：Android SDK Build-Tools 28.0.3（aapt / javac / dx / zipalign / apksigner）
- **依赖**：零第三方依赖（不含 Gradle、不含原生库、不含 AAR）

---

## 项目结构

```
KugouMusicPlayer-src/
├── AndroidManifest.xml          # 清单：权限、7个Activity + 常驻播放服务
├── build.sh                     # 一键构建脚本（输出 bin/KugouMusic.apk）
├── res/
│   ├── drawable/                # 按钮/输入框/进度条/图标（纯 XML 矢量）
│   ├── layout/                  # 各界面与列表项布局（含 view_mini_player.xml 迷你播放栏）
│   └── values/                  # 颜色 / 文案 / 样式
└── src/com/kugou/musicplayer/
    ├── MainActivity.java        # 主页：功能网格 + 迷你播放栏
    ├── SearchActivity.java      # 搜索页：音乐+MV 同时搜索、历史、结果切换
    ├── PlayerActivity.java      # 完整播放器：进度/音量/增强/收藏/下载/上一首下一首
    ├── MVPlayerActivity.java    # MV 页：在线音频播放 / 本地 MV 列表 / 下载
    ├── DownloadActivity.java    # 下载管理
    ├── FavoriteActivity.java    # 收藏列表
    ├── ApiDocActivity.java      # 应用内 API 文档
    ├── api/KugouApi.java        # ★ 全部酷狗接口封装（JSON 解析 + HTTP 请求）
    ├── model/                   # Song / MV / DownloadTask 数据模型
    ├── adapter/                 # 各列表适配器
    ├── service/MusicPlayService.java  # ★ 常驻播放服务（全局单例，迷你栏读状态）
    └── util/                    # 下载/收藏/历史/本地扫描/音量增强/迷你播放栏等
```

---

## 使用的酷狗接口（KugouApi.java）

| 用途 | 接口 |
|---|---|
| 搜索歌曲 | `GET http://mobilecdn.kugou.com/api/v3/search/song?keyword={kw}&page=1&pagesize=20` |
| 歌曲播放地址 | `GET http://m.kugou.com/api/v1/song/get_song_info?hash={hash}&cmd=playInfo`（backup_url，HTTPS MP3） |
| 搜索 MV | `GET http://mobilecdn.kugou.com/api/v3/search/mv?keyword={kw}&page=1&pagesize=20` |
| MV 播放地址 | `GET http://m.kugou.com/app/i/mv.php?cmd=100&hash={hash}`（mvdata.sd.downurl，MKV） |

> 接口均为酷狗公开 Web 端所使用，本项目为接口学习 / 调用的示例实现。

---

## 构建方法

### 1. 环境准备

```bash
# JDK 8（便携版解压即可）
export JAVA_HOME=/path/to/jdk8u392-b08

# Android SDK：platforms/android-28 + build-tools/28.0.3
export ANDROID_HOME=/path/to/android-sdk
```

### 2. 构建

```bash
chmod +x build.sh
./build.sh
```

### 3. 产物

- `bin/KugouMusic.apk` —— 已签名（v1）可直接安装
- `bin/KugouMusic-unsigned.apk` —— 未签名
- 首次构建自动生成 `debug.keystore`（alias=debug / 密码 android）

### 4. 手动构建流程（等价于 build.sh）

```bash
# ① 生成 R.java
aapt package -f -m -J gen -S res -I $ANDROID_HOME/platforms/android-28/android.jar -M AndroidManifest.xml

# ② 编译（Java 6）
find src gen -name "*.java" > sources.txt
javac -source 1.6 -target 1.6 -encoding UTF-8 -bootclasspath $PLATFORM -d bin/classes @sources.txt

# ③ dex
dx --dex --output=bin/classes.dex bin/classes

# ④ 打包资源
aapt package -f -M AndroidManifest.xml -S res -I $PLATFORM -F bin/KugouMusic-unsigned.apk

# ⑤ 注入 dex + 对齐
(cd bin && jar uf KugouMusic-unsigned.apk classes.dex)
zipalign -f 4 bin/KugouMusic-unsigned.apk bin/KugouMusic-aligned.apk

# ⑥ 签名（Android 2.3 必须 v1 签名）
apksigner sign --ks debug.keystore --ks-key-alias debug \
  --ks-pass pass:android --key-pass pass:android \
  --v1-signing-enabled true --v2-signing-enabled false \
  --out bin/KugouMusic.apk bin/KugouMusic-aligned.apk
```

---

## 关键实现说明

### 播放服务常驻（MusicPlayService）

`PlayerActivity` 先 `startService()` 再 `bindService()`，服务持有全局单例
`MusicPlayService.sInstance`。Activity 销毁后仅 unbind，音乐继续播放；
底部迷你播放栏通过该单例实时读取「当前歌曲 / 是否播放中」并刷新 UI。

### 底部迷你播放栏（MiniPlayer + view_mini_player.xml）

- 无播放 → 标题占位「酷狗音乐」，副标题「搜索你想听的歌」，按钮 ▶
- 播放中 → 歌名 / 歌手，按钮 ❚❚（播放中）/ ▶（暂停）
- 点栏身 → 打开完整播放器并恢复当前歌曲（不会重头播）
- 每 500ms 刷新一次状态，Activity 销毁时 detach 停止刷新

### MV 播放策略

酷狗 MV 均为 MKV（H.264 + Vorbis），Android 2.3 原生解码器无 MKV 视频解码能力，
方案权衡后采用**纯音频路线**：用系统 MediaPlayer 在线解出音频轨播放（有声无画面），
保留搜索 / 下载 / 本地 MV 列表。曾尝试 Vitamio、FFmpegMediaPlayer、ijkplayer，
均因体积膨胀或仍无法渲染而弃用（见 git 历史 / 文档）。

### 一键增强音量（VolumeBoost）

- 优先调用 `AudioManager` 音频处理（API 9+）做软件增益
- 不支持时回退为将系统媒体音量拉满 + `MediaPlayer.setVolume`
- 播放页音量条（0-100）映射到实际增益

### 本地媒体扫描（LocalMediaScanner）

- 音频：遍历 `/sdcard/` 常见音频扩展名（mp3/m4a/aac/wav/flac/ogg/amr）
- MV：扩展名 mkv/mp4/avi，下载目录固定为 `KugouMusic/Download/`
- 下载完成后下次打开下载管理自动读取该目录

---

## 权限说明

| 权限 | 用途 |
|---|---|
| INTERNET | 搜索与在线播放 |
| READ/WRITE_EXTERNAL_STORAGE | 扫描本地音乐、保存下载文件 |
| ACCESS_NETWORK_STATE | 网络状态判断 |
| WAKE_LOCK | 播放时保持唤醒 |
| MODIFY_AUDIO_SETTINGS | 音量增强 |

---

## 已知限制

1. **MV 无画面**：仅音频轨可播（MKV 视频解码缺失），详见上文
2. 播放服务为进程级常驻：`START_NOT_STICKY`，进程被系统杀掉后不自动重启（如需可改 `START_STICKY`）
3. 接口为酷狗 Web 端公开接口，如接口调整需同步更新 `KugouApi.java`
4. 仅支持竖屏（`screenOrientation="portrait"`）

---

## License

MIT License —— 本仓库代码可自由使用、修改、分发，但**请遵守接口方（酷狗音乐）的服务条款**，仅供学习交流。

Copyright (c) 2026
