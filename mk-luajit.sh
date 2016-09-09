# see http://luajit.org/install.html for details
# there, a call like one of the following is recommended

if [ "x$NDK" == "x" ]; then
	NDK=/opt/android-ndk
fi
if [ ! -d "$NDK" ]; then
	echo 'NDK not found. Please set NDK environment variable and have it point to the NDK dir.'
	exit 1
fi

BUILD_ARCH=linux-$(uname -m)
DEST=$(cd $(dirname $0) && pwd)/jni/luajit-build/$1

case "$1" in
clean)
	make -C luajit-2.0 clean
	;;
armeabi)
	# Android/ARM, armeabi (ARMv5TE soft-float), Android 2.2+ (Froyo)
	NDKABI=8
	NDKVER=$NDK/toolchains/arm-linux-androideabi-4.9
	if [ ! -d "$NDKVER" ]; then
	    echo 'NDK not of the right version, please update to NDK version 11 or higher.'
	    exit 1
	fi
	NDKP=$NDKVER/prebuilt/$BUILD_ARCH/bin/arm-linux-androideabi-
	NDKF="--sysroot $NDK/platforms/android-$NDKABI/arch-arm"
	rm -rf "$DEST"
	make -C luajit-2.0 install HOST_CC="gcc -m32" CROSS=$NDKP TARGET_FLAGS="$NDKF" DESTDIR="$DEST" PREFIX=
	;;
armeabi-v7a)
	# Android/ARM, armeabi-v7a (ARMv7 VFP), Android 4.0+ (ICS)
	NDKABI=14
	NDKVER=$NDK/toolchains/arm-linux-androideabi-4.9
	if [ ! -d "$NDKVER" ]; then
	    echo 'NDK not of the right version, please update to NDK version 11 or higher.'
	    exit 1
	fi
	NDKP=$NDKVER/prebuilt/$BUILD_ARCH/bin/arm-linux-androideabi-
	NDKF="--sysroot $NDK/platforms/android-$NDKABI/arch-arm"
	NDKARCH="-march=armv7-a -mfloat-abi=softfp -Wl,--fix-cortex-a8"
	make -C luajit-2.0 install HOST_CC="gcc -m32" CROSS=$NDKP TARGET_FLAGS="$NDKF $NDKARCH" DESTDIR="$DEST" PREFIX=
	;;
mips)
	# Android/MIPS, mips (MIPS32R1 hard-float), Android 4.0+ (ICS)
	NDKABI=14
	NDKVER=$NDK/toolchains/mipsel-linux-android-4.9
	if [ ! -d "$NDKVER" ]; then
	    echo 'NDK not of the right version, please update to NDK version 11 or higher.'
	    exit 1
	fi
	NDKP=$NDKVER/prebuilt/$BUILD_ARCH/bin/mipsel-linux-android-
	NDKF="--sysroot $NDK/platforms/android-$NDKABI/arch-mips"
	make -C luajit-2.0 install HOST_CC="gcc -m32" CROSS=$NDKP TARGET_FLAGS="$NDKF" DESTDIR="$DEST" PREFIX=
	;;
x86)
	# Android/x86, x86 (i686 SSE3), Android 4.0+ (ICS)
	NDKABI=14
	NDKVER=$NDK/toolchains/x86-4.9
	if [ ! -d "$NDKVER" ]; then
	    echo 'NDK not of the right version, please update to NDK version 11 or higher.'
	    exit 1
	fi
	NDKP=$NDKVER/prebuilt/$BUILD_ARCH/bin/i686-linux-android-
	NDKF="--sysroot $NDK/platforms/android-$NDKABI/arch-x86"
	make -C luajit-2.0 install HOST_CC="gcc -m32" CROSS=$NDKP TARGET_FLAGS="$NDKF" DESTDIR="$DEST" PREFIX=
	;;
*)
	echo 'specify one of "armeabi", "armeabi-v7a", "mips", "x86" or "clean" as first argument'
	exit 1
	;;
esac
