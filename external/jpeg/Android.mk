#ndk-build -B LOCAL_ARM_NEON=true ARCH_ARM_HAVE_NEON=true APP_ABI="armeabi-v7a arm64-v8a x86 x86_64"

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_ARM_MODE := arm

LOCAL_SRC_FILES := \
    jcapimin.c jcapistd.c jccoefct.c jccolor.c jcdctmgr.c jchuff.c \
    jcinit.c jcmainct.c jcmarker.c jcmaster.c jcomapi.c jcparam.c \
    jcphuff.c jcprepct.c jcsample.c jctrans.c jdapimin.c jdapistd.c \
    jdatadst.c jdatasrc.c jdcoefct.c jdcolor.c jddctmgr.c jdhuff.c \
    jdinput.c jdmainct.c jdmarker.c jdmaster.c jdmerge.c jdphuff.c \
    jdpostct.c jdsample.c jdtrans.c jerror.c jfdctflt.c jfdctfst.c \
    jfdctint.c jidctflt.c jidctfst.c jidctint.c jidctred.c jquant1.c \
    jquant2.c jutils.c jmemmgr.c jmemnobs.c

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
	LOCAL_SRC_FILES += armv6_idct.S
endif

ifneq (,$(TARGET_BUILD_APPS))
# unbundled branch, built against NDK.
LOCAL_SDK_VERSION := 17
endif

LOCAL_CFLAGS += -DAVOID_TABLES
LOCAL_CFLAGS += -O3 -fstrict-aliasing -fprefetch-loop-arrays
LOCAL_CFLAGS += -Wno-unused-parameter

# enable tile based decode
LOCAL_CFLAGS += -DANDROID_TILE_BASED_DECODE

ifeq ($(TARGET_ARCH_ABI),x86)
	LOCAL_CFLAGS += -DANDROID_INTELSSE2_IDCT
	LOCAL_SRC_FILES += jidctintelsse.c
endif
# LOCAL_CFLAGS_x86 += -DANDROID_INTELSSE2_IDCT
# LOCAL_SRC_FILES_x86 += jidctintelsse.c

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
LOCAL_SRC_FILES += \
        jsimd_arm64_neon.S \
        jsimd_neon.c
endif

ifeq ($(ARCH_ARM_HAVE_NEON),true)
  #use NEON accelerations
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_CFLAGS += -DNV_ARM_NEON -D__ARM_HAVE_NEON
LOCAL_SRC_FILES += \
	jsimd_arm_neon.S \
	jsimd_neon.c
endif
else
  # enable armv6 idct assembly
 ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
  LOCAL_CFLAGS_arm += -DANDROID_ARMV6_IDCT
 endif
endif

# use mips assembler IDCT implementation if MIPS DSP-ASE is present
ifeq ($(strip $(ARCH_MIPS_HAS_DSP)),true)
LOCAL_CFLAGS_mips += -DANDROID_MIPS_IDCT
LOCAL_SRC_FILES_mips += \
    mips_jidctfst.c \
    mips_idct_le.S
endif

LOCAL_MODULE := libjpeg_static

LOCAL_EXPORT_C_INCLUDE_DIRS := $(LOCAL_PATH)

include $(BUILD_STATIC_LIBRARY)

