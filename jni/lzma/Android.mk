LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := 7z

lzma_SOURCES := \
	7zAlloc.c 7zArcIn.c 7zBuf.c 7zBuf2.c \
	7zCrc.c 7zCrcOpt.c 7zDec.c 7zFile.c \
	7zStream.c Bcj2.c Bra.c Bra86.c BraIA64.c \
	CpuArch.c Delta.c Lzma2Dec.c LzmaDec.c \
	Ppmd7.c Ppmd7Dec.c

un7zip_SOURCES := \
	7zAssetFile.cpp \
	7zExtractor.cpp \
	7zFunctions.cpp

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/7z/C \
    $(LOCAL_PATH)/un7zip

LOCAL_SRC_FILES := \
	$(addprefix 7z/C/, $(lzma_SOURCES)) \
	$(addprefix un7zip/, $(un7zip_SOURCES)) \
	un7z.cpp

LOCAL_CFLAGS += -Wno-enum-conversion -ffunction-sections -fdata-sections
LOCAL_CXXFLAGS += -std=c++11 -fexceptions -ffunction-sections -fdata-sections
LOCAL_LDFLAGS += -Wl,--gc-sections

LOCAL_LDLIBS := -llog -landroid

include $(BUILD_SHARED_LIBRARY)
