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

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE	:= libluajit-prebuilt
LOCAL_SRC_FILES	:= luajit-build/$(TARGET_ARCH_ABI)/lib/libluajit-5.1.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/luajit-build/$(TARGET_ARCH_ABI)/include

include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    := native-luajit-activity
LOCAL_SRC_FILES := android-main.c
# remember to add libraries here that you plan to use via FFI:
LOCAL_LDLIBS    := -lm -llog -landroid
LOCAL_STATIC_LIBRARIES := android_native_app_glue
LOCAL_STATIC_LIBRARIES += libluajit-prebuilt

include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/native_app_glue)
