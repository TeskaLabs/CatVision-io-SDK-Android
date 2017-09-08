package com.teskalabs.cvio;

import java.nio.ByteBuffer;

class cviojni {

	static {
		System.loadLibrary("cviojni");
	}

	static native int jni_run(String socketPath, int width, int height);
	static native int jni_shutdown();

	static native void jni_image_ready(); // Send a 'signal' to VNC server that we have a image ready

	static native int jni_push_pixels_rgba_8888(ByteBuffer pixels, int row_stride);
	static native int jni_push_pixels_rgba_565(ByteBuffer pixels, int row_stride);

	static native void jni_set_delegate(VNCDelegate ra);
}
