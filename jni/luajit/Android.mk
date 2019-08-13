# Add LuaJIT prebuilt static library.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libluajit-prebuilt
LOCAL_SRC_FILES := build/$(TARGET_ARCH_ABI)/lib/libluajit-5.1.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/build/$(TARGET_ARCH_ABI)/include

include $(PREBUILT_STATIC_LIBRARY)
