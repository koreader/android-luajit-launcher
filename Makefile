
apk: local.properties
	git submodule init
	git submodule sync
	git submodule update
	./mk-luajit.sh armeabi-v7a
	ndk-build
	ant debug

local.properties:
	android update project --path .

dev: apk
	ant dev
