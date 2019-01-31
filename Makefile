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

update:
	# update local.properties and project.properties with sdk/ndk paths for current user
	# this works with sdk tools <= 25.3.0
	android update project --path . -t android-$(SDKAPI_MIN)

	# update sources
	git submodule init
	git submodule sync
	git submodule update

build-native:
	# build luajit, lzma and native activity for desired arch (armeabi-v7a, x86, x86_64)
	./mk-luajit.sh $(ANDROID_FULL_ARCH)
	ndk-build ANDROID_FULL_ARCH=$(ANDROID_FULL_ARCH)

debug: update build-native
	ant debug
	cp -pv bin/NativeActivity-debug.apk NativeActivity.apk
	@echo "application was built, type: debug (signed)"

release: update build-native
	ant release
	cp -pv bin/NativeActivity-release-unsigned.apk NativeActivity.apk
	@echo "application was built, type: release (unsigned)"
	@echo "You'll need to sign this application to be able to install it"

clean:
	./mk-luajit.sh clean
	rm -rf bin obj libs gen jni/luajit-build local.properties assets/module
