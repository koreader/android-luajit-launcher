#!/bin/bash
# see http://luajit.org/install.html for details
# there, a call like one of the following is recommended

[[ -v NDK ]] || export NDK=/opt/android-ndk
if [ ! -d "$NDK" ]; then
    echo 'NDK not found. Please set NDK environment variable and have it point to the NDK dir.'
    exit 1
fi

# NDKABI=21  # Android 5.0+
# NDKABI=19  # Android 4.4+
#NDKABI=${NDKABI:-14} # Android 4.0+
NDKABI=${NDKABI:-9} # Android 2.3+
BUILD_ARCH=linux-$(uname -m)
DEST=$(cd "$(dirname "$0")" && pwd)/jni/luajit-build/$1

echo "Using NDKABI ${NDKABI}."

NDKVER=$(grep -oP 'r\K([0-9]+)(?=[a-z])' ${NDK}/CHANGELOG.md)
echo "Detected NDK version ${NDKVER}..."
if [ "$NDKVER" -lt 11 ]; then
    echo 'NDK not of the right version, please update to NDK version 11 or higher.'
    exit 1
fi

case "$1" in
    clean)
        make -C luajit-2.0 clean
        ;;
    armeabi)
        # Android/ARM, armeabi (ARMv5TE soft-float)
        TCVER=($NDK/toolchains/arm-linux-androideabi-4.*)
        NDKP=${TCVER[0]}/prebuilt/$BUILD_ARCH/bin/arm-linux-androideabi-
        NDKF="--sysroot $NDK/platforms/android-$NDKABI/arch-arm"
        rm -rf "$DEST"
        make -C luajit-2.0 install HOST_CC="gcc -m32" CROSS="$NDKP" TARGET_FLAGS="$NDKF" DESTDIR="$DEST" PREFIX=
        ;;
    armeabi-v7a)
        # Android/ARM, armeabi-v7a (ARMv7 VFP)
        TCVER=($NDK/toolchains/arm-linux-androideabi-4.*)
        NDKP=${TCVER[0]}/prebuilt/$BUILD_ARCH/bin/arm-linux-androideabi-
        NDKF="--sysroot $NDK/platforms/android-$NDKABI/arch-arm"
        NDKARCH="-march=armv7-a -mfloat-abi=softfp -Wl,--fix-cortex-a8"
        make -C luajit-2.0 install HOST_CC="gcc -m32" CROSS="$NDKP" TARGET_FLAGS="$NDKF $NDKARCH" DESTDIR="$DEST" PREFIX=
        ;;
    mips)
        # Android/MIPS, mips (MIPS32R1 hard-float)
        TCVER=($NDK/toolchains/mipsel-linux-android-4.*)
        NDKP=${TCVER[0]}/prebuilt/$BUILD_ARCH/bin/mipsel-linux-android-
        NDKF="--sysroot $NDK/platforms/android-$NDKABI/arch-mips"
        make -C luajit-2.0 install HOST_CC="gcc -m32" CROSS="$NDKP" TARGET_FLAGS="$NDKF" DESTDIR="$DEST" PREFIX=
        ;;
    x86)
        # Android/x86, x86 (i686 SSE3)
        TCVER=($NDK/toolchains/x86-4.*)
        NDKP=${TCVER[0]}/prebuilt/$BUILD_ARCH/bin/i686-linux-android-
        NDKF="--sysroot $NDK/platforms/android-$NDKABI/arch-x86"
        make -C luajit-2.0 install HOST_CC="gcc -m32" CROSS="$NDKP" TARGET_FLAGS="$NDKF" DESTDIR="$DEST" PREFIX=
        ;;
    *)
        echo 'specify one of "armeabi", "armeabi-v7a", "mips", "x86" or "clean" as first argument'
        exit 1
        ;;
esac
