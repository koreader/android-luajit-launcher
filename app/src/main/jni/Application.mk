# The ARMv7 is significanly faster due to the use of the hardware FPU

# since luajit is built separately, build one after the other, each
# with an according LuaJIT build
#APP_ABI := armeabi armeabi-v7a
APP_ABI := $(ANDROID_FULL_ARCH)
APP_PLATFORM := android-$(NDKABI)

#bug: https://github.com/android-ndk/ndk/issues/372 fixed in ndk-16, workaroud:
#https://stackoverflow.com/questions/17131691/non-numeric-second-argument-to-wordlist
