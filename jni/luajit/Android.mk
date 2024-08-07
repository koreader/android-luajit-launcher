# Add LuaJIT prebuilt shared library.

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := luajit
ifeq (,$(and $(LUAJIT_INC),$(LUAJIT_LIB)))
LOCAL_SRC_FILES := build/$(TARGET_ARCH_ABI)/lib/libluajit.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/build/$(TARGET_ARCH_ABI)/include/luajit-2.1
else
LOCAL_SRC_FILES := $(LUAJIT_LIB)
LOCAL_EXPORT_C_INCLUDES := $(LUAJIT_INC)
endif

include $(PREBUILT_SHARED_LIBRARY)
