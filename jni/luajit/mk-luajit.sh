#!/bin/bash
# see http://luajit.org/install.html for details
# there, a call like one of the following is recommended

# NDKABI=21  # Android 5.0+
# NDKABI=19  # Android 4.4+
NDKABI=${NDKABI:-14} # Android 4.0+
#NDKABI=${NDKABI:-9} # Android 2.3+
DEST=$(cd "$(dirname "$0")" && pwd)/build/$1
# might be linux-x86_64 or darwin-x86-64
HOST_ARCH="*"

# Patch luajit like in koreader-base
# Script pilfered from patch-wrapper in koreader-base
PATCH_FILE=koreader-luajit-makefile-tweaks.patch

# Reverse patch will succeed if the patch is already applied.
# In case of failure, it means we should try to apply the patch.
if ! patch -R -p1 -N --dry-run <"${PATCH_FILE}" >/dev/null 2>&1; then
    # Now patch for real.
    if ! patch -p1 -N <"${PATCH_FILE}"; then
        exit $?
    fi
fi

function check_NDK() {
    [[ -n $NDK ]] || export NDK=/opt/android-ndk
    if [ ! -d "$NDK" ]; then
        echo 'NDK not found. Please set NDK environment variable and have it point to the NDK dir.'
        exit 1
    fi

    echo "Using NDKABI ${NDKABI}."

    NDKVER=$(grep -oP 'r\K([0-9]+)(?=[a-z])' ${NDK}/CHANGELOG.md | head -1)
    echo "Detected NDK version ${NDKVER}..."
    if [ "$NDKVER" -lt 15 ]; then
        echo 'NDK not of the right version, please update to NDK version 15 or higher.'
        exit 1
    fi
}

## NOTE: Since https://github.com/koreader/koreader-base/pull/1133, we append -DLUAJIT_SECURITY_STRHASH=0 -DLUAJIT_SECURITY_STRID=0 to TARGET_CFLAGS on !Android platforms.
##       Here, we leave it at the defaults, because we have much less control over the environment on Android, so, better be safe than sorry ;).
case "$1" in
    clean)
        make -C luajit-2.0 clean
        ;;
    armeabi)
        # Android/ARM, armeabi (ARMv5TE soft-float)
        check_NDK
        TCVER=("${NDK}"/toolchains/arm-linux-androideabi-4.*)
        NDKP=${TCVER[0]}/prebuilt/${HOST_ARCH}/bin/arm-linux-androideabi-
        NDKF="--sysroot ${NDK}/platforms/android-${NDKABI}/arch-arm"
        rm -rf "$DEST"
        make -C luajit-2.0 install HOST_CC="gcc -m32" CFLAGS="-O2 -pipe" HOST_CFLAGS="-O2 -pipe -mtune=generic" LDFLAGS="" HOST_LDFLAGS="" TARGET_CFLAGS="${CFLAGS}" TARGET_LDFLAGS="${LDFLAGS}" CROSS="$NDKP" TARGET_FLAGS="$NDKF" TARGET_SYS=Linux DESTDIR="$DEST" PREFIX=
        ;;
    armeabi-v7a)
        # Android/ARM, armeabi-v7a (ARMv7 VFP)
        check_NDK
        TCVER=("${NDK}"/toolchains/arm-linux-androideabi-4.*)
        NDKP=${TCVER[0]}/prebuilt/${HOST_ARCH}/bin/arm-linux-androideabi-
        NDKF="--sysroot ${NDK}/platforms/android-${NDKABI}/arch-arm"
        NDKARCH="-march=armv7-a -mfloat-abi=softfp -Wl,--fix-cortex-a8"
        make -C luajit-2.0 install HOST_CC="gcc -m32" CFLAGS="-O2 -pipe" HOST_CFLAGS="-O2 -pipe -mtune=generic" LDFLAGS="" HOST_LDFLAGS="" TARGET_CFLAGS="${CFLAGS}" TARGET_LDFLAGS="${LDFLAGS}" CROSS="$NDKP" TARGET_FLAGS="${NDKF} ${NDKARCH}" TARGET_SYS=Linux DESTDIR="$DEST" PREFIX=
        ;;
    mips)
        # Android/MIPS, mips (MIPS32R1 hard-float)
        check_NDK
        TCVER=("${NDK}"/toolchains/mipsel-linux-android-4.*)
        NDKP=${TCVER[0]}/prebuilt/${HOST_ARCH}/bin/mipsel-linux-android-
        NDKF="--sysroot ${NDK}/platforms/android-${NDKABI}/arch-mips"
        make -C luajit-2.0 install HOST_CC="gcc -m32" CFLAGS="-O2 -pipe" HOST_CFLAGS="-O2 -pipe -mtune=generic" LDFLAGS="" HOST_LDFLAGS="" TARGET_CFLAGS="${CFLAGS}" TARGET_LDFLAGS="${LDFLAGS}" CROSS="${NDKP}" TARGET_FLAGS="$NDKF" TARGET_SYS=Linux DESTDIR="$DEST" PREFIX=
        ;;
    x86)
        # Android/x86, x86 (i686 SSE3)
        check_NDK
        TCVER=("${NDK}"/toolchains/x86-4.*)
        NDKP=${TCVER[0]}/prebuilt/${HOST_ARCH}/bin/i686-linux-android-
        NDKF="--sysroot ${NDK}/platforms/android-${NDKABI}/arch-x86"
        make -C luajit-2.0 install HOST_CC="gcc -m32" CFLAGS="-O2 -pipe" HOST_CFLAGS="-O2 -pipe -mtune=generic" LDFLAGS="" HOST_LDFLAGS="" TARGET_CFLAGS="${CFLAGS}" TARGET_LDFLAGS="${LDFLAGS}" CROSS="$NDKP" TARGET_FLAGS="$NDKF" TARGET_SYS=Linux DESTDIR="$DEST" PREFIX=
        ;;
    *)
        echo 'specify one of "armeabi", "armeabi-v7a", "mips", "x86" or "clean" as first argument'
        exit 1
        ;;
esac
