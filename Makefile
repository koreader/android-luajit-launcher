# Supported Android ABIs: armeabi-v7a, x86 and x86_64

ifdef ANDROID_ARCH
	ifeq ($(ANDROID_ARCH), arm)
		ANDROID_FULL_ARCH?=armeabi-v7a
	else
		ANDROID_FULL_ARCH?=$(ANDROID_ARCH)
	endif
endif

# Default is build for arm
ANDROID_FULL_ARCH?=armeabi-v7a

# Minimum SDK API is 19, required to use View.VERSION_CODES.KITKAT
SDKAPI_MIN=$(shell [ ${NDKABI} -ge 19 ] && echo -n ${NDKABI} || echo -n 19)

apk: local.properties project.properties
	git submodule init
	git submodule sync
	git submodule update
	./mk-luajit.sh $(ANDROID_FULL_ARCH)
	ndk-build ANDROID_FULL_ARCH=$(ANDROID_FULL_ARCH)
	ant debug
        #gradle debug

local.properties project.properties:
	android update project --path . -t android-$(SDKAPI_MIN}

clean:
	-ndk-build clean
	./mk-luajit.sh clean
	rm -rf bin obj libs gen jni/luajit-build local.properties assets/module

dev: apk
	ant dev
	#gradle dev
