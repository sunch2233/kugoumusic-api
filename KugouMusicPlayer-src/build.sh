#!/bin/bash
# ============================================================
# 酷狗音乐播放器（API提取版）构建脚本
# 纯手工构建：aapt -> javac -> dx -> aapt打包 -> jar加dex -> zipalign -> apksigner
# 无需 Gradle，支持 Android 2.3+（minSdk 9）
#
# 前置环境：
#   JAVA_HOME   : JDK 8（便携版即可）
#   ANDROID_HOME: Android SDK（需含 platforms/android-28 与 build-tools/28.0.3）
#
# 用法：./build.sh
# 输出：bin/KugouMusic.apk
# ============================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

: "${JAVA_HOME:?请设置 JAVA_HOME 环境变量（JDK 8）}"
: "${ANDROID_HOME:?请设置 ANDROID_HOME 环境变量（Android SDK）}"

BUILD_TOOLS="$ANDROID_HOME/build-tools/28.0.3"
PLATFORM="$ANDROID_HOME/platforms/android-28/android.jar"
AAPT="$BUILD_TOOLS/aapt"
DX="$BUILD_TOOLS/dx"
ZIPALIGN="$BUILD_TOOLS/zipalign"
APKSIGNER="$BUILD_TOOLS/apksigner"

# 检查工具
for tool in "$AAPT" "$DX" "$ZIPALIGN" "$APKSIGNER"; do
    if [ ! -f "$tool" ]; then
        echo "缺少构建工具: $tool"
        exit 1
    fi
done

echo "==> 1/6 生成 R.java"
rm -rf gen bin
mkdir -p gen bin/classes
"$AAPT" package -f -m -J gen -S res -I "$PLATFORM" -M AndroidManifest.xml

echo "==> 2/6 编译 Java 源码（Java 6 语法）"
find src gen -name "*.java" > sources.txt
"$JAVA_HOME/bin/javac" -source 1.6 -target 1.6 -encoding UTF-8 \
    -bootclasspath "$PLATFORM" -classpath "$PLATFORM" \
    -d bin/classes @sources.txt

echo "==> 3/6 生成 classes.dex"
"$DX" --dex --output=bin/classes.dex bin/classes

echo "==> 4/6 打包 APK（资源+清单）"
"$AAPT" package -f -M AndroidManifest.xml -S res -I "$PLATFORM" \
    -F bin/KugouMusic-unsigned.apk

echo "==> 5/6 注入 classes.dex 并 zipalign"
cd bin
"$JAVA_HOME/bin/jar" uf KugouMusic-unsigned.apk classes.dex
cd ..
"$ZIPALIGN" -f 4 bin/KugouMusic-unsigned.apk bin/KugouMusic-aligned.apk

echo "==> 6/6 签名（v1）"
if [ ! -f debug.keystore ]; then
    echo "生成调试签名密钥..."
    "$JAVA_HOME/bin/keytool" -genkeypair -v \
        -keystore debug.keystore -alias debug \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass android -keypass android \
        -dname "CN=KugouMusicPlayer, OU=Dev, O=Dev, L=City, S=State, C=CN"
fi
"$APKSIGNER" sign \
    --ks debug.keystore --ks-key-alias debug \
    --ks-pass pass:android --key-pass pass:android \
    --v1-signing-enabled true --v2-signing-enabled false \
    --out bin/KugouMusic.apk bin/KugouMusic-aligned.apk

echo "=========================================="
echo "构建完成: $SCRIPT_DIR/bin/KugouMusic.apk"
ls -lh bin/KugouMusic.apk
"$APKSIGNER" verify bin/KugouMusic.apk
echo "签名验证通过"
