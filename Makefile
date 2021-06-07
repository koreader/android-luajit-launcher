ifdef ANDROID_ARCH
	ifeq ($(ANDROID_ARCH), arm64)
		ANDROID_FULL_ARCH?=arm64-v8a
		ARCH?=Arm64
	else ifeq ($(ANDROID_ARCH), x86)
		ANDROID_FULL_ARCH?=$(ANDROID_ARCH)
		ARCH?=X86
	else ifeq ($(ANDROID_ARCH), x86_64)
		ANDROID_FULL_ARCH?=$(ANDROID_ARCH)
		ARCH?=X86_64
	endif
endif

ifdef ANDROID_FLAVOR
	ifeq ($(ANDROID_FLAVOR), fdroid)
		FLAVOR?=Fdroid
	endif
endif

# Default is build for arm
ANDROID_FULL_ARCH?=armeabi-v7a
ARCH?=Arm
FLAVOR?=Rocks
BUILD_TASK?=assemble$(ARCH)$(FLAVOR)
LINT_TASK?=lint$(ARCH)$(FLAVOR)

# find the path where the SDK is installed
ifdef SDK
        ANDROID_SDK_FULLPATH?=$(SDK)
else
        ifdef ANDROID_HOME
                ANDROID_SDK_FULLPATH?=$(ANDROID_HOME)
        else
                ANDROID_SDK_FULLPATH?=$(shell realpath ../../../base/toolchain/android-sdk-linux)
        endif
endif

# find the path where the NDK is installed
ifdef NDK
	ANDROID_NDK_FULLPATH?=$(NDK)
else
	ifdef ANDROID_NDK_HOME
		ANDROID_NDK_FULLPATH?=$(ANDROID_NDK_HOME)
	else
		ANDROID_NDK_FULLPATH?=$(shell realpath ../../../base/toolchain/android-ndk-r15c)
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

# Defaults, overriding fallback values in gradle.properties
NAME?=1.0
VERSION?=1
APPNAME?="luajit-launcher"

update:
	@echo "Updating sources"
	git submodule init
	git submodule sync
	git submodule update
	@echo "sdk.dir=$(ANDROID_SDK_FULLPATH)" > local.properties
	@echo "using sdk in path $(ANDROID_SDK_FULLPATH)"
	@echo "using ndk in path $(ANDROID_NDK_FULLPATH)"

build-luajit-debug:
	@echo "Building LuaJIT for $(ANDROID_FULL_ARCH) (debug)"
	cd jni/luajit && \
		./mk-luajit.sh "$(ANDROID_FULL_ARCH)" "debug"

build-luajit:
	@echo "Building LuaJIT for $(ANDROID_FULL_ARCH)"
	cd jni/luajit && \
		./mk-luajit.sh "$(ANDROID_FULL_ARCH)"

prepare: update
	@echo "Building LuaJIT for all supported ABIs"
	cd jni/luajit &&  \
		./mk-luajit.sh clean && \
		./mk-luajit.sh x86 && \
		./mk-luajit.sh clean && \
		./mk-luajit.sh armeabi-v7a

debug: update build-luajit-debug
	@echo "Building $(APPNAME) debug APK: Version $(NAME), release $(VERSION), flavor $(FLAVOR)"
	./gradlew -q -PversName=$(NAME) -PversCode=$(VERSION) -PprojectName=$(APPNAME) \
		-PndkCustomPath=$(ANDROID_NDK_FULLPATH) app:$(BUILD_TASK)Debug
	mkdir -p bin/
	find app/build/outputs/apk/ -type f -name '*.apk' -exec mv -v {} bin/ \;
	@echo "Application $(APPNAME) was built, type: debug (signed), flavor: $(FLAVOR), version: $(NAME), release $(VERSION)"

release: update build-luajit
	@echo "Building $(APPNAME) release APK: Version $(NAME), release $(VERSION), flavor $(FLAVOR)"
	./gradlew -q -PversName=$(NAME) -PversCode=$(VERSION) -PprojectName=$(APPNAME) \
		-PndkCustomPath=$(ANDROID_NDK_FULLPATH) app:$(BUILD_TASK)Release
	mkdir -p bin/
	find app/build/outputs/apk/ -type f -name '*.apk' -exec mv -v {} bin/ \;
	@echo "Application $(APPNAME) was built, type: release (unsigned), flavor: $(FLAVOR), version: $(NAME), release $(VERSION)"
	@echo "WARNING: You'll need to sign this application to be able to install it"

example: update clean build-luajit
	@echo "Building HelloWorld example"
	mkdir -p assets/module/
	cp -pv examples/helloWorld/*.lua assets/module/
	./gradlew -q -PversName=1.0 -PversCode=1 -PprojectName=HelloFromLua \
		-PndkCustomPath=$(ANDROID_NDK_FULLPATH) app:$(BUILD_TASK)Debug
	mkdir -p bin/
	find app/build/outputs/apk/ -type f -name '*.apk' -exec mv -v {} bin/ \;

lint:
	./gradlew -PndkCustomPath=$(ANDROID_NDK_FULLPATH) $(LINT_TASK)Release

clean:
	@echo "Cleaning binaries, assets and LuaJIT build"
	rm -rf assets/module/ bin/ jni/luajit/build
	cd jni/luajit && \
		./mk-luajit.sh clean

mrproper: clean
	-./gradlew clean --continue
