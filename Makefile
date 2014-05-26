
dev:
	git submodule init
	git submodule sync
	git submodule update
	android update project --path .
	./mk-luajit.sh armeabi-v7a
	ndk-build
	ant dev
