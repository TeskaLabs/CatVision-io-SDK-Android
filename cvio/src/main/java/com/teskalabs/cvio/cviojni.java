package com.teskalabs.cvio;

import java.nio.ByteBuffer;

public class cviojni {

	static {
		System.loadLibrary("cviojni");
	}

	public static native int run_vnc_server(String socketPath, int width, int height);
	public static native int shutdown_vnc_server();

	public static native void image_ready(); // Send a 'signal' to VNC server that we have a image ready
	public static native int push_pixels(ByteBuffer pixels, int row_stride);

	public static native void set_delegate(VNCDelegate ra);
}
