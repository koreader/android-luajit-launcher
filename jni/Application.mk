# The ARMv7 is significanly faster due to the use of the hardware FPU

# since luajit is built separately, build one after the other, each
# with an according LuaJIT build
#APP_ABI := armeabi armeabi-v7a
APP_ABI := armeabi-v7a
APP_PLATFORM := android-10
