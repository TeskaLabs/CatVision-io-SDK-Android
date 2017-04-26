LOCAL_PATH:= $(call my-dir)
PROJECT_PATH:= $(LOCAL_PATH)/../../../..


include $(CLEAR_VARS)
LOCAL_MODULE := libvncserver
LOCAL_SRC_FILES := $(PROJECT_PATH)/external/lib/$(TARGET_ARCH_ABI)/libvncserver.a
include $(PREBUILT_STATIC_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE := libjpeg_static
LOCAL_SRC_FILES := $(PROJECT_PATH)/external/lib/$(TARGET_ARCH_ABI)/libjpeg_static.a
include $(PREBUILT_STATIC_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE := libpng
LOCAL_SRC_FILES := $(PROJECT_PATH)/external/lib/$(TARGET_ARCH_ABI)/libpng.a
include $(PREBUILT_STATIC_LIBRARY)


include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	cviojni.c

WITH_WEBSOCKETS:=0

LOCAL_C_INCLUDES := $(PROJECT_PATH)/external/include


LOCAL_LDLIBS:=-llog -lz
LOCAL_STATIC_LIBRARIES := libvncserver libjpeg_static libpng
LOCAL_MODULE:= cviojni


include $(BUILD_SHARED_LIBRARY)
