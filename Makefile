ifdef ANDROID_ARCH
	ifeq ($(ANDROID_ARCH), x86)
		ANDROID_FULL_ARCH?=$(ANDROID_ARCH)
		GRADLE_TASK?=assembleX86
	endif
endif


# Default is build for arm
ANDROID_FULL_ARCH?=armeabi-v7a
GRADLE_TASK?=assembleArm

# find the path where the SDK is installed
ifdef SDK
        ANDROID_SDK_FULLPATH?=$(SDK)
else
        ifdef ANDROID_HOME
                ANDROID_SDK_FULLPATH?=$(ANDROID_HOME)
        else
                ANDROID_SDK_FULLPATH?=`realpath ../../../base/toolchain/android-sdk-linux`
        endif
endif

# find the path where the NDK is installed
ifdef NDK
	ANDROID_NDK_FULLPATH?=$(NDK)
else
	ifdef ANDROID_NDK_HOME
		ANDROID_NDK_FULLPATH?=$(ANDROID_NDK_HOME)
	else
		ANDROID_NDK_FULLPATH?=`realpath ../../../base/toolchain/android-ndk-r15c`
	endif
endif

# override android:versionName="string"
ifdef ANDROID_NAME
	NAME?=$(ANDROID_NAME)
endif

# override android:versionCode="integer"
ifdef ANDROID_VERSION
	VERSION?=$(ANDROID_VERSION)
endif

# support different app names
ifdef ANDROID_APPNAME
	APPNAME?=$(ANDROID_APPNAME)
endif

# support different build flavors
ifdef ANDROID_FLAVOR
	FLAVOR?=$(ANDROID_FLAVOR)
endif

# Defaults, overriding fallback values in gradle.properties
NAME?=1.5
VERSION?=5
APPNAME?="luajit-launcher"
FLAVOR?="rocks"

update:
	# update sources
	git submodule init
	git submodule sync
	git submodule update
	@echo "#define LOGGER_NAME \"$(APPNAME)\"" > jni/logger.h
	@echo "sdk.dir=$(ANDROID_SDK_FULLPATH)" > local.properties
	@echo "ndk.dir=$(ANDROID_NDK_FULLPATH)" >> local.properties
	@echo "using sdk in path $(ANDROID_SDK_FULLPATH)"
	@echo "using ndk in path $(ANDROID_NDK_FULLPATH)"

build-luajit:
	# build luajit
	./mk-luajit.sh $(ANDROID_FULL_ARCH)

prepare: update
	# for Android Studio users. Build luajit for all supported abis
	./mk-luajit.sh clean
	./mk-luajit.sh x86
	./mk-luajit.sh clean
	./mk-luajit.sh armeabi-v7a
	@echo "project dependencies were built. Now you can build the project in Android Studio"

debug: update build-luajit
	# build signed debug apk
	./gradlew -PversName=$(NAME) -PversCode=$(VERSION) -PprojectName=$(APPNAME) -PprojectFlavor=$(FLAVOR) $(GRADLE_TASK)Debug
	@echo "application was built, type: debug (signed), flavor: $(FLAVOR), version: $(NAME), release $(VERSION)"
	mkdir -p bin/
	find launcher/build/outputs/apk/ -type f -name '*.apk' -exec mv -v {} bin/ \;

release: update build-luajit
	# build unsigned release apk, with version code and version name
	@echo "Building release APK, Version $(NAME), release $(VERSION)"
	./gradlew -PversName=$(NAME) -PversCode=$(VERSION) -PprojectName=$(APPNAME) -PprojectFlavor=$(FLAVOR) $(GRADLE_TASK)Release
	@echo "application $(APPNAME) was built, type: release (unsigned), flavor: $(FLAVOR), version: $(NAME), release $(VERSION)"
	@echo "WARNING: You'll need to sign this application to be able to install it"
	mkdir -p bin/
	find launcher/build/outputs/apk/ -type f -name '*.apk' -exec mv -v {} bin/ \;

clean:
	# clean luajit build tree and remove binaries (assets and apks)
	./mk-luajit.sh clean
	rm -rf assets/module/ bin/

mrproper: clean
	# deep clean, it will fail on non-built variants, so continue
	# without doubt and finally remove luajit libraries
	-./gradlew clean --continue
	-rm -rf jni/luajit-build/
