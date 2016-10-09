NDK_VER=$(shell grep -E 'NDKABI=[0-9]+' ./mk-luajit.sh | cut -d= -f2)

apk: local.properties
	git submodule init
	git submodule sync
	git submodule update
	./mk-luajit.sh armeabi-v7a
	ndk-build
	ant debug

local.properties:
	android update project --path . -t android-19

clean:
	-ndk-build clean
	rm -rf bin obj libs gen jni/luajit-build local.properties assets/module

dev: apk
	ant dev
