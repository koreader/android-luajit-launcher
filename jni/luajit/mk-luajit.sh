#!/bin/bash
# see http://luajit.org/install.html for details
# there, a call like one of the following is recommended

# We use NDK r15c for all architectures.

# For 32 bits (armeabi-v7a and x86) we built against platform-14 (ICS)
# For 64 bits (arm64-v8a and x86_64) we built against platform-21 (Lollipop)

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
        make -C luajit clean
        ;;
    armeabi-v7a)
        # Android/ARM, armeabi-v7a (ARMv7 VFP)
        NDKABI=${NDKABI:-14}
        check_NDK
        TCVER=("${NDK}"/toolchains/arm-linux-androideabi-4.*)
        NDKP=${TCVER[0]}/prebuilt/${HOST_ARCH}/bin/arm-linux-androideabi-
        NDKF="--sysroot ${NDK}/platforms/android-${NDKABI}/arch-arm"
        NDKARCH="-march=armv7-a -mfloat-abi=softfp -Wl,--fix-cortex-a8"
        make -C luajit install HOST_CC="gcc -m32" CFLAGS="-O2 -pipe" HOST_CFLAGS="-O2 -pipe -mtune=generic" LDFLAGS="" HOST_LDFLAGS="" TARGET_CFLAGS="${CFLAGS}" TARGET_LDFLAGS="${LDFLAGS}" CROSS="$NDKP" TARGET_FLAGS="${NDKF} ${NDKARCH}" TARGET_SYS=Linux DESTDIR="$DEST" PREFIX=
        ;;
    arm64-v8a)
        # Android/ARM, arm64-v8a (ARM64 VFP4, NEON)
        NDKABI=${NDKABI:-21}
        check_NDK
        TCVER=("${NDK}"/toolchains/aarch64-linux-android-4.*)
        NDKP=${TCVER[0]}/prebuilt/${HOST_ARCH}/bin/aarch64-linux-android-
        NDKF="--sysroot ${NDK}/platforms/android-${NDKABI}/arch-arm64"
        make -C luajit install HOST_CC="gcc" CFLAGS="-O2 -pipe" HOST_CFLAGS="-O2 -pipe -mtune=generic" LDFLAGS="" HOST_LDFLAGS="" TARGET_CFLAGS="${CFLAGS}" TARGET_LDFLAGS="${LDFLAGS}" CROSS="$NDKP" TARGET_FLAGS="${NDKF}" TARGET_SYS=Linux DESTDIR="$DEST" PREFIX=
        ;;
    x86)
        # Android/x86, x86 (i686 SSE3)
        NDKABI=${NDKABI:-14}
        check_NDK
        TCVER=("${NDK}"/toolchains/x86-4.*)
        NDKP=${TCVER[0]}/prebuilt/${HOST_ARCH}/bin/i686-linux-android-
        NDKF="--sysroot ${NDK}/platforms/android-${NDKABI}/arch-x86"
        make -C luajit install HOST_CC="gcc -m32" CFLAGS="-O2 -pipe" HOST_CFLAGS="-O2 -pipe -mtune=generic" LDFLAGS="" HOST_LDFLAGS="" TARGET_CFLAGS="${CFLAGS}" TARGET_LDFLAGS="${LDFLAGS}" CROSS="$NDKP" TARGET_FLAGS="$NDKF" TARGET_SYS=Linux DESTDIR="$DEST" PREFIX=
        ;;
    x86_64)
        # Android/x86_64, x86_64
        NDKABI=${NDKABI:-21}
        check_NDK
        TCVER=("${NDK}"/toolchains/x86_64-4.*)
        NDKP=${TCVER[0]}/prebuilt/${HOST_ARCH}/bin/x86_64-linux-android-
        NDKF="--sysroot ${NDK}/platforms/android-${NDKABI}/arch-x86_64"
        make -C luajit install HOST_CC="gcc" CFLAGS="-O2 -pipe" HOST_CFLAGS="-O2 -pipe -mtune=generic" LDFLAGS="" HOST_LDFLAGS="" TARGET_CFLAGS="${CFLAGS}" TARGET_LDFLAGS="${LDFLAGS}" CROSS="$NDKP" TARGET_FLAGS="${NDKF}" TARGET_SYS=Linux DESTDIR="$DEST" PREFIX=
        ;;
    *)
        echo 'specify one of "armeabi-v7a", "arm64-v8a", "x86", "x86_64" or "clean" as first argument'
        exit 1
        ;;
esac
