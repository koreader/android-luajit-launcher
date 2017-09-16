NDK_VER=$(shell grep -E 'NDKABI=[0-9]+' ./mk-luajit.sh | cut -d= -f2)

# required for View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LOW_PROFILE
NDKABI_MIN_16=$(shell [ ${NDKABI} -ge 16 ] && echo -n ${NDKABI} || echo -n 16)

apk: local.properties project.properties
	git submodule init
	git submodule sync
	git submodule update
	./mk-luajit.sh armeabi-v7a
	ndk-build
	ant debug

local.properties project.properties:
	android update project --path . -t android-$(NDKABI_MIN_16)

clean:
	-ndk-build clean
	rm -rf bin obj libs gen jni/luajit-build local.properties assets/module

dev: apk
	ant dev
