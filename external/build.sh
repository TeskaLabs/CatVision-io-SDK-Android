#!/bin/bash -e

# On Mac OSX: $ ANDROID_NDK="${HOME}/Library/Android/sdk/ndk-bundle" ./build.sh

BASEDIR=$(dirname "$0")

APP_ABI="armeabi-v7a arm64-v8a x86 x86_64"
BUILD_CMD="ndk-build -B LOCAL_ARM_NEON=true ARCH_ARM_HAVE_NEON=true"

# Clear
rm -rf $BASEDIR/jpeg/obj
rm -rf $BASEDIR/libpng/obj
rm -rf $BASEDIR/libvncserver/obj
rm -rf $BASEDIR/lib
rm -rf $BASEDIR/include

# Replace makefiles and configs
cp $BASEDIR/jpeg/Android.mk $BASEDIR/jpeg/jni
cp $BASEDIR/libpng/Android.mk $BASEDIR/libpng/jni

cp $BASEDIR/libvncserver/Android.mk $BASEDIR/libvncserver/jni
cp $BASEDIR/libvncserver/rfbconfig.h $BASEDIR/libvncserver/jni/rfb/
cp $BASEDIR/libvncserver/rfbserver.c $BASEDIR/libvncserver/jni/libvncserver/
cp $BASEDIR/libvncserver/sockets.c $BASEDIR/libvncserver/jni/libvncserver/

# Build
(cd $BASEDIR/jpeg/jni && ${ANDROID_NDK}/${BUILD_CMD} APP_ABI="${APP_ABI}")
(cd $BASEDIR/libpng/jni && ${ANDROID_NDK}/${BUILD_CMD} APP_ABI="${APP_ABI}")
(cd $BASEDIR/libvncserver/jni && ${ANDROID_NDK}/${BUILD_CMD} APP_ABI="${APP_ABI}")

# Copy libraries
for ARCH in ${APP_ABI}
do
    mkdir -p $BASEDIR/lib/$ARCH

    cp $BASEDIR/jpeg/obj/local/$ARCH/libjpeg_static.a $BASEDIR/lib/$ARCH/
    cp $BASEDIR/libpng/obj/local/$ARCH/libpng.a $BASEDIR/lib/$ARCH/
    cp $BASEDIR/libvncserver/obj/local/$ARCH/libvncserver.a $BASEDIR/lib/$ARCH/
done

mkdir -p $BASEDIR/include/rfb
cp $BASEDIR/libvncserver/jni/rfb/rfb.h $BASEDIR/libvncserver/jni/rfb/rfbconfig.h $BASEDIR/libvncserver/jni/rfb/rfbint.h $BASEDIR/libvncserver/jni/rfb/rfbproto.h $BASEDIR/include/rfb

cp $BASEDIR/jpeg/jni/jpeglib.h $BASEDIR/include/

cp $BASEDIR/libpng/jni/png.h $BASEDIR/include/
