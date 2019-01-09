NDK_VER=$(shell grep -E 'NDKABI=[0-9]+' ./mk-luajit.sh | cut -d= -f2)

# Android is kind of bizarre. Sometimes you need specific stuff.
# Other times you need just `arm` or else…
# For `x86` and `x86_64` it's all good.
ifdef ANDROID_ARCH
	ifeq ($(ANDROID_ARCH), arm)
		ANDROID_FULL_ARCH?=armeabi-v7a
	else
		ANDROID_FULL_ARCH?=$(ANDROID_ARCH)
	endif
endif
ANDROID_FULL_ARCH?=armeabi-v7a

# at least 16 is required to create a build with View.SYSTEM_UI_FLAG_FULLSCREEN
# and View.SYSTEM_UI_FLAG_LOW_PROFILE
# however, default to 19 because that's what the nightly build server uses
NDKABI_MIN_16=$(shell [ ${NDKABI} -ge 16 ] && echo -n ${NDKABI} || echo -n 19)

apk: local.properties project.properties
	git submodule init
	git submodule sync
	git submodule update
	./mk-luajit.sh $(ANDROID_FULL_ARCH)
	ndk-build ANDROID_FULL_ARCH=$(ANDROID_FULL_ARCH)
	ant debug
        #gradle debug

local.properties project.properties:
	android update project --path . -t android-$(NDKABI_MIN_16)

clean:
	-ndk-build clean
	./mk-luajit.sh clean
	rm -rf bin obj libs gen jni/luajit-build local.properties assets/module

dev: apk
	ant dev
	#gradle dev
