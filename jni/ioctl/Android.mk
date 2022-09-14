LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := ioctl
LOCAL_SRC_FILES := ioctl.cpp
LOCAL_CFLAGS += -Wno-enum-conversion -ffunction-sections -fdata-sections
LOCAL_CXXFLAGS += -std=c++11 -fexceptions -ffunction-sections -fdata-sections
LOCAL_LDFLAGS += -Wl,--gc-sections

LOCAL_LDLIBS := -llog -landroid

include $(BUILD_SHARED_LIBRARY)
