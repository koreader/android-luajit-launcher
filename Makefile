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

# API19 is used by the docker build image.
# It's also required to use View.VERSION_CODES.KITKAT.
TARGET_API=19

ifdef NDKABI
	# Update target API if is higher than current target
	SDKAPI=$(shell [ ${NDKABI} -gt ${TARGET_API} ] && echo -n ${NDKABI} || echo -n ${TARGET_API})
endif

SDKAPI?=$(TARGET_API)

# override android:versionName="string"
ifdef ANDROID_NAME
	NAME?=$(ANDROID_NAME)
endif

# override android:versionCode="integer"
ifdef ANDROID_VERSION
	VERSION?=$(ANDROID_VERSION)
endif

# support different flavors
ifdef ANDROID_FLAVOR
	FLAVOR?=$(ANDROID_FLAVOR)
endif

# Defaults
NAME?=1.5
VERSION?=5
FLAVOR?="rocks"

update:
	# update local.properties and project.properties with sdk/ndk paths for current user
	# this works with sdk tools <= 25.3.0
	android update project --path . -t android-$(SDKAPI)

	# update sources
	git submodule init
	git submodule sync
	git submodule update

build-native:
	# build luajit, lzma and native activity for desired arch (armeabi-v7a, x86, x86_64)
	./mk-luajit.sh $(ANDROID_FULL_ARCH)
	ndk-build ANDROID_FULL_ARCH=$(ANDROID_FULL_ARCH)

debug: update build-native
	# build signed debug apk, with version code and version name
	ant -Dname=$(NAME) -Dcode=$(VERSION) -Dflavor=$(FLAVOR) debug
	cp -pv bin/NativeActivity-debug.apk bin/NativeActivity.apk
	@echo "application was built, type: debug (signed)"

release: update build-native
        # build unsigned release apk, with version code and version name
	@echo "Building release APK, Version $(NAME), release $(VERSION)"
	ant -Dname=$(NAME) -Dcode=$(VERSION) -Dflavor=$(FLAVOR) release
	cp -pv bin/NativeActivity-release-unsigned.apk bin/NativeActivity.apk
	@echo "application was built, type: release (unsigned), flavor: $(FLAVOR), version: $(NAME), release $(VERSION), api $(SDKAPI)"
	@echo "WARNING: You'll need to sign this application to be able to install it"

clean:
	ndk-build ANDROID_FULL_ARCH=$(ANDROID_FULL_ARCH) clean
	./mk-luajit.sh clean
	rm -rf bin obj libs gen jni/luajit-build local.properties assets/module
