# Copyright (C) 2010 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

BASE_PATH := $(call my-dir)

# Android Native App Glue
LOCAL_PATH := $(BASE_PATH)
include $(LOCAL_PATH)/android_native_app_glue/Android.mk
include $(CLEAR_VARS)

# LuaJIT shared library
LOCAL_PATH := $(BASE_PATH)
include $(LOCAL_PATH)/luajit/Android.mk
include $(CLEAR_VARS)

# lib7z shared library
LOCAL_PATH := $(BASE_PATH)
include $(LOCAL_PATH)/lzma/Android.mk
include $(CLEAR_VARS)

# Dummy target that will ensure we ship the LuaJIT shared library
# NOTE: Only necessary if we want to dlopen LuaJIT, which isn't the default ;).
#LOCAL_PATH := $(BASE_PATH)
#LOCAL_MODULE := dummy
#LOCAL_SRC_FILES := dummy.c
#LOCAL_SHARED_LIBRARIES := luajit
#include $(BUILD_SHARED_LIBRARY)
#include $(CLEAR_VARS)

# final shared library to load via the NativeActivity framework.
LOCAL_PATH := $(BASE_PATH)
LOCAL_MODULE := luajit-launcher
LOCAL_SRC_FILES := main.c
LOCAL_STATIC_LIBRARIES := android_native_app_glue
# NOTE: By default, we link against the shared LuaJIT library directly.
LOCAL_SHARED_LIBRARIES := luajit
# But, we can also dlopen it, as a remnant of mcode_alloc experimentations...
#LOCAL_C_INCLUDES := $(LOCAL_PATH)/luajit/build/$(TARGET_ARCH_ABI)/include
#LOCAL_CFLAGS += -DKO_DLOPEN_LUAJIT

# Adding libraries that we plan to use via FFI here isn't strictly necessary anymore
LOCAL_EXPORT_LDLIBS := -lm -ldl -llog -landroid

include $(BUILD_SHARED_LIBRARY)
