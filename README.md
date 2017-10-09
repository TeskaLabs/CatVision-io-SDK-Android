# CatVision.io SDK for Android

CatVision.io SDK for Android is an library that enables to securely share screen of the app or device. It also enables to remotely control your app via mouse and keyboard events sent to your application.

![CatVision.io](https://teskalabscom.azureedge.net/media/img/solutions/teskalabs-catvisionio-illustration-android.png)

You can watch a [demonstration video](https://www.youtube.com/watch?v=bKjMwUtapxc) from our early development stage.

## Read the docs

Complete documentation for **CatVision.io** can be found at [https://docs.catvision.io](https://docs.catvision.io)

In the docs you will also find out [how to integrate Catvision](https://docs.catvision.io) in your application.

## Build

Clone this repo and cd into it

```
$ git clone git@github.com:TeskaLabs/CatVision-Android.git
$ cd CatVision-Android
```

Clone submodules

```
$ git submodule init
$ git submodule update
```

Now you can proceed with the following steps:

1. Build the VNC server library
2. Download SeaCat Client dependency
3. Build the cvio module's JNI
4. Build the AAR

### Build the VNC server library

You need [Android NDK](https://developer.android.com/ndk/index.html) toolset to build the binaries for Android devices. They need to be available in your `$PATH` or `$ANDROID_NDK`.

For example on Mac OSX, if you had installed Android NDK using the Android SDK manager before:

```
$ export ANDROID_NDK="${HOME}/Library/Android/sdk/ndk-bundle"
```

Then build the VNC server

```
$ cd external
$ ./build.sh
```

### Download SeaCat Client dependency

The CatVision.io module depends on **SeaCat Client**. SeaCat takes care for identifying your device and making it able to connect securely.

```
$ cp SeaCatClient_Android_v1611-rc-1-release.aar ./seacat 
```

### Build the cvio module's JNI

CatVision java class uses JNI interface to call the VNC server's functions.

`$BASEDIR` is the path to the cloned repository. 

```
$ cd $BASEDIR/cvio/src/main/jni
$ ./build.sh
```

### Build the project

Use Android Studio: `Build->Clean Project`, `Build->Make Project`

The **CatVision AAR** is now in `$BASEDIR/cvio/build/outputs/aar`

TODO: build from command line
