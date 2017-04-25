LOCAL_PATH:= $(call my-dir)

# We need to build this for both the device (as a shared library)
# and the host (as a static library for tools to use).

common_SRC_FILES := \
    png.c \
    pngerror.c \
    pngget.c \
    pngmem.c \
    pngpread.c \
    pngread.c \
    pngrio.c \
    pngrtran.c \
    pngrutil.c \
    pngset.c \
    pngtrans.c \
    pngwio.c \
    pngwrite.c \
    pngwtran.c \
    pngwutil.c \

ifeq ($(ARCH_ARM_HAVE_NEON),true)
my_cflags_arm := -DPNG_ARM_NEON_OPT=2
endif

my_cflags_arm64 := -DPNG_ARM_NEON_OPT=2

my_src_files_arm := \
    arm/arm_init.c \
    arm/filter_neon.S \
    arm/filter_neon_intrinsics.c

my_cflags_intel := -DPNG_INTEL_SSE_OPT=1

my_src_files_intel := \
    contrib/intel/intel_init.c \
    contrib/intel/filter_sse2_intrinsics.c

common_CFLAGS := -std=gnu89 -Wno-unused-parameter


# For the device (static) for platform (retains fortify support)
# =====================================================

include $(CLEAR_VARS)

LOCAL_CLANG := true
LOCAL_SRC_FILES := $(common_SRC_FILES)
LOCAL_CFLAGS += $(common_CFLAGS) -ftrapv
LOCAL_ASFLAGS += $(common_ASFLAGS)
LOCAL_SANITIZE := never
LOCAL_EXPORT_C_INCLUDE_DIRS := $(LOCAL_PATH)
LOCAL_SHARED_LIBRARIES := libz
LOCAL_MODULE:= libpng

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_SRC_FILES += $(my_src_files_arm)
LOCAL_CFLAGS += $(my_cflags_arm)
endif
ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_SRC_FILES += $(my_src_files_arm)
LOCAL_CFLAGS += $(my_cflags_arm64)
endif
ifeq ($(TARGET_ARCH_ABI),x86)
LOCAL_SRC_FILES += $(my_src_files_intel)
LOCAL_CFLAGS += $(my_cflags_intel)
endif
ifeq ($(TARGET_ARCH_ABI),x86_64)
LOCAL_SRC_FILES += $(my_src_files_intel)
LOCAL_CFLAGS += $(my_cflags_intel)
endif


include $(BUILD_STATIC_LIBRARY)

