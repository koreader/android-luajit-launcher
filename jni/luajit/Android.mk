# Add LuaJIT prebuilt shared library.

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := luajit
LOCAL_SRC_FILES := build/$(TARGET_ARCH_ABI)/lib/libluajit.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/build/$(TARGET_ARCH_ABI)/include

include $(PREBUILT_SHARED_LIBRARY)
