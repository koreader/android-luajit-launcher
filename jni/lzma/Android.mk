# Build lzma shared library.

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

DEFLATE7_DIR := $(LOCAL_PATH)/7z
PATCH_OUTPUT := $(shell cd $(DEFLATE7_DIR) && patch -N -p1 < ../7zMain.patch)
lzma_SOURCES := \
        7zStream.c 7zFile.c Ppmd7Dec.c Ppmd7.c Bcj2.c \
        Bra86.c BraIA64.c Bra.c CpuArch.c Lzma2Dec.c LzmaDec.c 7zDec.c \
        7zCrcOpt.c 7zCrc.c 7zBuf2.c 7zBuf.c 7zAlloc.c \
        7zArcIn.c \
        Delta.c \
        Util/7z/7zMain.c

LOCAL_MODULE := lzma
LOCAL_SRC_FILES := $(addprefix 7z/C/, $(lzma_SOURCES))
LOCAL_CFLAGS := -O2 -I$(DEFLATE7_DIR)/C -D_7ZIP_PPMD_SUPPPORT -Wno-enum-conversion

include $(BUILD_SHARED_LIBRARY)
