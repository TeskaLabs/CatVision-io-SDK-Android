package com.teskalabs.cvio;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.PixelFormat;
import android.media.Image;
import android.os.Build;
import android.util.Log;

import com.teskalabs.seacat.android.client.SeaCatClient;
import com.teskalabs.seacat.android.client.socket.SocketConfig;

import java.io.IOException;
import java.nio.ByteBuffer;

class VNCServer extends ContextWrapper {

    private static final String TAG = VNCServer.class.getName();
	protected static CVIOSeaCatPlugin cvioSeaCatPlugin = null;

	private Thread mVNCThread = null;
	private final String socketFileName;
	private static final int port = 5900;

	static {
		// JNI part of this class
		System.loadLibrary("cviojni");
	}

    public VNCServer(Context base, VNCDelegate delegate) {
		super(base);

		// Enable SeaCat
		if (cvioSeaCatPlugin == null) {
			cvioSeaCatPlugin = new CVIOSeaCatPlugin(port);
		}

		jni_set_delegate(delegate);

		// There has to be one directory (/s/) - it is used to ensure correct access level
		socketFileName = getDir("cvio", Context.MODE_PRIVATE).getAbsolutePath() + "/s/vnc";
    }

	public void configureSeaCat() throws IOException {
		SeaCatClient.configureSocket(
			port,
			SocketConfig.Domain.AF_UNIX, SocketConfig.Type.SOCK_STREAM, 0,
			socketFileName, ""
		);
	}


	public boolean run(final int screenWidth, final int screenHeight)
    {
        if ((screenWidth < 0) || (screenHeight < 0))
        {
            Log.e(TAG, "Screen width/height is not specified");
            return false;
        }

		synchronized (this) {
			// Check if VNC Thread is alive
			while (this.mVNCThread != null) {
				int rc;
				rc = jni_shutdown();
				if (rc != 0) Log.w(TAG, "jni_shutdown returned: " + rc);

				try {
					this.mVNCThread.join(5000);
					if (this.mVNCThread.isAlive()) {
						Log.w(TAG, this.mVNCThread + " is still joining ...");
						continue;
					}
					this.mVNCThread = null;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			// Prepare VNC thread and launch
			if (this.mVNCThread == null) {
				this.mVNCThread = new Thread(new Runnable() {
					public void run() {
						int rc;

						Log.d(TAG, "VNC Server started");

						rc = jni_run(socketFileName, screenWidth, screenHeight);
						if (rc != 0) Log.w(TAG, "VNC Server thread exited with rc: " + rc);

						Log.i(TAG, "VNC Server terminated");
					}
				});
				this.mVNCThread.setName("cvioVNCThread");
				this.mVNCThread.setDaemon(true);
				this.mVNCThread.start();
			}
		}

        return true;
    }


    public void shutdown() {

		if (this.mVNCThread == null) return;

		synchronized (this) {

			while (this.mVNCThread != null) {
				int rc;
				rc = jni_shutdown();
				if (rc != 0) Log.w(TAG, "jni_shutdown returned: " + rc);

				try {
					this.mVNCThread.join(5000);
					if (this.mVNCThread.isAlive()) {
						Log.w(TAG, this.mVNCThread + " is still joining ...");
						continue;
					}
					this.mVNCThread = null;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
    }

	// Signalize that we have an image ready
	public void imageReady() {
		jni_image_ready();
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public void push(Image image, int pixelFormat) {
		Image.Plane[] planes = image.getPlanes();
		ByteBuffer b = planes[0].getBuffer();
		if (pixelFormat == PixelFormat.RGBA_8888) {
			// planes[0].getPixelStride() has to be 4 (32 bit)
			jni_push_pixels_rgba_8888(b, planes[0].getRowStride());
		}
		else if (pixelFormat == PixelFormat.RGB_565)
		{
			// planes[0].getPixelStride() has to be 16 (16 bit)
			jni_push_pixels_rgba_565(b, planes[0].getRowStride());
		}
		else
		{
			Log.e(TAG, "Image reader acquired unsupported image format " + pixelFormat);
		}
	}

	// JNI interface to VNC server

	private static native int jni_run(String socketPath, int width, int height);
	private static native int jni_shutdown();

	private static native void jni_image_ready(); // Send a 'signal' to VNC server that we have a image ready

	private static native int jni_push_pixels_rgba_8888(ByteBuffer pixels, int row_stride);
	private static native int jni_push_pixels_rgba_565(ByteBuffer pixels, int row_stride);

	private static native void jni_set_delegate(VNCDelegate ra);

}
