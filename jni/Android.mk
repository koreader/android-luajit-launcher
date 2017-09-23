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

DEFLATE7_DIR := $(LOCAL_PATH)/compress-deflate7
PATCH_OUTPUT := $(shell cd $(DEFLATE7_DIR) && patch -N -p1 < ../7zMain.patch)
lzma_SOURCES := \
        7zStream.c 7zFile.c Ppmd7Dec.c Ppmd7.c Bcj2.c \
        Bra86.c Bra.c Lzma2Dec.c LzmaDec.c 7zIn.c 7zDec.c \
        7zCrcOpt.c 7zCrc.c 7zBuf2.c 7zBuf.c 7zAlloc.c \
        Util/7z/7zMain.c

LOCAL_MODULE := lzma
LOCAL_SRC_FILES := $(addprefix compress-deflate7/7zip/C/, $(lzma_SOURCES))
LOCAL_CFLAGS := -O2 -I$(DEFLATE7_DIR)/7zip/C -D_7ZIP_PPMD_SUPPPORT

include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    := luajit
LOCAL_SRC_FILES := android-main.c
# remember to add libraries here that you plan to use via FFI:
LOCAL_LDLIBS    := -lm -llog -landroid
LOCAL_STATIC_LIBRARIES := android_native_app_glue
LOCAL_WHOLE_STATIC_LIBRARIES += libluajit-prebuilt

include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/native_app_glue)
