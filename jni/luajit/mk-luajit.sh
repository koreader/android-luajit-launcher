#!/bin/bash
# see http://luajit.org/install.html for details
# there, a call like one of the following is recommended

set -eo pipefail

# We use NDK r15c for all architectures.

# For 32 bits (armeabi-v7a and x86) we built against platform-14 (ICS)
# For 64 bits (arm64-v8a and x86_64) we built against platform-21 (Lollipop)

DEST="$(cd "$(dirname "$0")" && pwd)/build/$1"
# might be linux-x86_64 or darwin-x86-64
HOST_ARCH="*"

function check_NDK() {
    for v in ANDROID_NDK_HOME ANDROID_NDK_ROOT; do
      ANDROID_NDK_HOME="${!v}"
      if [[ -n "${!v}" ]]; then
        break
      fi
    done
    [[ -n "${ANDROID_NDK_HOME}" ]] || ANDROID_NDK_HOME=/opt/android-ndk
    if [ ! -d "${ANDROID_NDK_HOME}" ]; then
        echo 'NDK not found. Please set ANDROID_NDK_HOME environment variable and have it point to the NDK dir.'
        exit 1
    fi
    export ANDROID_NDK_HOME

    echo "Using NDKABI ${NDKABI}."

    NDKVER=$(grep -oP 'r\K([0-9]+)(?=[a-z])' ${ANDROID_NDK_ROOT}/CHANGELOG.md | head -1)
    echo "Detected NDK version ${NDKVER}..."
    if [ "$NDKVER" -lt 15 ]; then
        echo 'NDK not of the right version, please update to NDK version 15 or higher.'
        exit 1
    fi
}

check_NDK
export PATH="${ANDROID_NDK_ROOT}/toolchains/llvm/prebuilt/linux-x86_64/bin:${PATH}"

makecmd=(
  make -C "${DEST}"
)
declare -A makeenv
patches=()

if [[ -z "${USE_NO_CCACHE}" ]] && which ccache 2>/dev/null; then
  ccache='ccache'
else
  ccache=''
fi

makeenv[HOST_CC]="${ccache}${ccache:+ } clang"
makeenv[HOST_CFLAGS]='-O2 -pipe -mtune=generic'
makeenv[HOST_LDFLAGS]=''

makeenv[CROSS]="${ccache}"
makeenv[CC]='clang'
makeenv[CFLAGS]='-O2 -pipe'
makeenv[LDFLAGS]=''

makeenv[TARGET_AR]='llvm-ar rcus'
makeenv[TARGET_RANLIB]='llvm-ranlib'
makeenv[TARGET_STRIP]='llvm-strip'
makeenv[TARGET_SYS]='Linux'

makeenv[TARGET_FLAGS]=''
makeenv[TARGET_CFLAGS]="${makeenv[CFLAGS]}"
makeenv[TARGET_LDFLAGS]="${makeenv[LDFLAGS]}"
makeenv[TARGET_LIBS]=''
makeenv[TARGET_SONAME]='libluajit.so'

makeenv[INSTALL_SONAME]='libluajit.so'

makeenv[DESTDIR]="${DEST}"
makeenv[PREFIX]=''

## NOTE: Since https://github.com/koreader/koreader-base/pull/1133, we append -DLUAJIT_SECURITY_STRHASH=0 -DLUAJIT_SECURITY_STRID=0 to TARGET_CFLAGS on !Android platforms.
##       Here, we leave it at the defaults, because we have much less control over the environment on Android, so, better be safe than sorry ;).
case "$1" in
    armeabi-v7a)
        # Android/ARM, armeabi-v7a (ARMv7 VFP)
        makeenv[CROSS]+=" armv7a-linux-androideabi${NDKABI:-18}-"
        makeenv[TARGET_FLAGS]="-march=armv7-a -mfloat-abi=softfp"
        makeenv[HOST_CC]+=' -m32'
        ;;
    arm64-v8a)
        # Android/ARM, arm64-v8a (ARM64 VFP4, NEON)
        makeenv[CROSS]+=" aarch64-linux-android${NDKABI:-21}-"
        ;;
    x86)
        # Android/x86, x86 (i686 SSE3)
        makeenv[CROSS]+=" i686-linux-android${NDKABI:-18}-"
        makeenv[HOST_CC]+=' -m32'
        ;;
    x86_64)
        # Android/x86_64, x86_64
        makeenv[CROSS]="x86_64-linux-android${NDKABI:-21}-"
        ;;
    *)
        echo 'specify one of "armeabi-v7a", "arm64-v8a", "x86", "x86_64" or "clean" as first argument'
        exit 1
        ;;
esac

case "$2" in
  clean)
    rm -rf "${DEST}"
    exit
    ;;
  debug)
    makeenv[TARGET_LIBS]="-landroid -llog"
    ;&
  '')
    patches+=(
      koreader-luajit-makefile-tweaks.patch
      koreader-luajit-enable-table_pack.patch
      koreader-luajit-mcode-reserve-hack.patch
      luajit-revert-b7a8c7c.patch
    )
    makecmd+=(amalg install)
    ;;
esac

for k in "${!makeenv[@]}"; do
  makecmd+=("${k}=${makeenv[${k}]}")
done

if ! [[ -d "${DEST}" ]]; then
  # Poor man's lndir…
  find "$PWD/luajit" \
    -name .git -prune \
    -o -type d -printf "mkdir -p '${DEST}/%P'; " \
    -o -type f -printf "ln -s %p '${DEST}/%P'; " \
    | sh -e
  for p in "${patches[@]}"; do
    patch --follow-symlinks --directory="${DEST}" --strip=1 --forward <"${p}"
  done
fi

printf '%q ' "${makecmd[@]}"
printf '\n'

exec "${makecmd[@]}"
