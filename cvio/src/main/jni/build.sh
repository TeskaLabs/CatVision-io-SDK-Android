#!/bin/bash -e

JAVAC=javac

# # Clean everything first
rm -f com_teskalabs_cvio_android_cviojni.h
rm -fr ../obj ../libs/armeabi-v7a ../libs/armeabi ../libs/x86

# Prepare compiled class
mkdir -p ../../../build/jni/classes
${JAVAC} -d ../../../build/jni/classes -classpath ~/Library/Android/sdk/platforms/android-21/android.jar:../java ../java/com/teskalabs/cvio/VNCServer.java

# Prepare header file
javah -d . -classpath ~/Library/Android/sdk/platforms/android-21/android.jar:../../../build/jni/classes com.teskalabs.cvio.VNCServer

# Compile Android JNI
${ANDROID_NDK}/ndk-build -B
