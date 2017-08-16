package com.teskalabs.cvio;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;

import com.teskalabs.seacat.android.client.CSR;
import com.teskalabs.seacat.android.client.SeaCatClient;
import com.teskalabs.seacat.android.client.socket.SocketConfig;
import com.teskalabs.cvio.inapp.InAppInputManager;
import com.teskalabs.cvio.inapp.KeySym;

import java.io.IOException;
import java.nio.ByteBuffer;

public class CatVision extends ContextWrapper implements VNCDelegate {

	private static final String TAG = CatVision.class.getName();

	public static final String DEFAULT_CLIENT_HANDLE = "-DefaultClientHandle-";
	private static final String PREFS_CLIENT_HANDLE_KEY = "clientHandle";
	private static final String PREFS_NAME = "cvio.prefs";

	protected static CVIOSeaCatPlugin cvioSeaCatPlugin = null;
	private static final int port = 5900;
	private static double downscale = 0;

	private MediaProjectionManager mProjectionManager = null;
	private static MediaProjection sMediaProjection = null;
	private static Thread sCaptureThread = null;

	private ImageReader mImageReader = null;
	private Handler mHandler = null;

	private VirtualDisplay mVirtualDisplay = null;
	private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;

	private int mDensity;
	private Display mDisplay = null;

	private int mRotation;
	private OrientationChangeCallback mOrientationChangeCallback = null;

	private final VNCServer vncServer;
	private final InAppInputManager inputManager;

	private final String APIKeyId;
	private String clientHandle = DEFAULT_CLIENT_HANDLE;

	///

	static protected CatVision instance = null;

	public static CatVision initialize(Application app) {
		return initialize(app, false);
	}

	public synchronized static CatVision initialize(Application app, boolean hasClientHandle) {

		if (instance != null) throw new RuntimeException("Already initialized");

		try {
			instance = new CatVision(app, hasClientHandle);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return instance;
	}

	static public CatVision getInstance()
	{
		return instance;
	}

	///

	private CatVision(Application app, boolean hasClientHandle) throws IOException {
		super(app.getApplicationContext());

		APIKeyId = getApplicationMetaData(app.getApplicationContext(), "cvio.api_key_id");
		if (APIKeyId == null)
		{
			throw new RuntimeException("CatVision access key (cvio.api_key_id) not provided");
		}

		// Enable SeaCat
		if (cvioSeaCatPlugin == null)
		{
			cvioSeaCatPlugin = new CVIOSeaCatPlugin(port);
		}

		if (hasClientHandle) {
			clientHandle = null;
		}

		SeaCatClient.setPackageName("com.teskalabs.cvio");
		SeaCatClient.initialize(app.getApplicationContext(), new Runnable() {
			@Override
			public void run() {
				CatVision.this.submitCSR();
			}
		});

		//resetClientHandle();

		cviojni.set_delegate(this);
		vncServer = new VNCServer(this);
		inputManager = new InAppInputManager(app);

		SeaCatClient.configureSocket(
			port,
			SocketConfig.Domain.AF_UNIX, SocketConfig.Type.SOCK_STREAM, 0,
			vncServer.getSocketFileName(), ""
		);
	}

	synchronized void submitCSR()
	{
		if (clientHandle == null)
		{
			Log.w(TAG, "Client handle is null, cannot submit CSR");
			return;
		}

		String state = SeaCatClient.getState();
		if (state.charAt(1) != 'C')
		{
			Log.w(TAG, "SeaCat is not ready for CSR");
			return;
		}

		CSR csr = new CSR();
		csr.setOrganization(getPackageName());
		csr.setOrganizationUnit(APIKeyId);

		if (clientHandle != DEFAULT_CLIENT_HANDLE)
		{
			csr.setUniqueIdentifier(clientHandle);
		}

		try {
			csr.submit();
		} catch (IOException e) {
			Log.e(TAG, "Submitting CSR", e);
		}
	}

	///

	public void resetClientHandle()
	{
		SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.remove(PREFS_CLIENT_HANDLE_KEY);
		editor.commit();

		clientHandle = null;

		try {
			SeaCatClient.reset();
		} catch (IOException e) {
			Log.e(TAG, "Client reset", e);
		}
	}

	public void setClientHandle(String clientHandle)
	{
		this.clientHandle = clientHandle;

		SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		if (sharedPref.contains(PREFS_CLIENT_HANDLE_KEY))
		{
			if (!sharedPref.getString(PREFS_CLIENT_HANDLE_KEY, "").equals(clientHandle))
			{
				// Reset identity is required, CSR will be submitted asynchronously
				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putString(PREFS_CLIENT_HANDLE_KEY, clientHandle);
				editor.commit();

				try {
					SeaCatClient.reset();
				} catch (IOException e) {
					Log.e(TAG, "Reset identity", e);
				}
			}

			else
			{
				String state = SeaCatClient.getState();
				if (state.charAt(1) == 'C')
				{
					// CSR hasn't successfully reached SeaCat server due to e.g., network connection - try again
					submitCSR();
					return;
				}

				// clientHandle exists and CSR is submitted, client should be successfully onboarded now.
				//TODO: Check SeaCatClient.getState() for all logical combinations (CSR has to be generated, submitted or accepted at this moment)
				Log.w(TAG, "SeaCat client state: " + SeaCatClient.getState());
			}
		}

		else {
			// Fresh onboarding, client handle is new and CSR is to be submitted
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putString(PREFS_CLIENT_HANDLE_KEY, clientHandle);
			editor.commit();

			submitCSR();
		}

	}

	///

	public void requestStart(Activity activity, int requestCode) {
		// call for the projection manager
		mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

		if (sCaptureThread == null) {
			// start capture handling thread
			sCaptureThread = new Thread(new Runnable() {
				public void run() {
					Looper.prepare();
					mHandler = new Handler();
					Looper.loop();
				}
			});
			sCaptureThread.start();
		}

		try {
			SeaCatClient.connect();
		} catch (IOException e) {
			Log.e(TAG, "SeaCatClient expcetion", e);
		}

		activity.startActivityForResult(mProjectionManager.createScreenCaptureIntent(), requestCode);
	}

	public void stop() {
		if (sCaptureThread == null) return;
		if (mHandler == null) return;

		mHandler.post(new Runnable() {
			@Override
			public void run() {
			if (sMediaProjection != null) {
				sMediaProjection.stop();
			}
			}
		});

		vncServer.stop();
		stopRepeatingPing();
	}

	public boolean isStarted() {
		return (sMediaProjection != null);
	}

	public void onActivityResult(Activity activity, int resultCode, Intent data) {
		sMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);

		if (sMediaProjection != null) {
			// display metrics
			DisplayMetrics metrics = getResources().getDisplayMetrics();
			mDensity = metrics.densityDpi;
			mDisplay = activity.getWindowManager().getDefaultDisplay();

			if (downscale == 0) {
				if (mDensity < 150) {
					downscale = 1;
				} else if (mDensity < 300) {
					downscale = 2;
				} else {
					downscale = 4;
				}
			}

			// create virtual display depending on device width / height
			createVirtualDisplay();

			// register orientation change callback
			mOrientationChangeCallback = new OrientationChangeCallback(this);
			if (mOrientationChangeCallback.canDetectOrientation()) {
				mOrientationChangeCallback.enable();
			}

			// register media projection stop callback
			sMediaProjection.registerCallback(new MediaProjectionStopCallback(), mHandler);
		}
	}

	/******************************************
	 * Here we receive Images
	 ****************/

	private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
		@Override
		public void onImageAvailable(ImageReader reader) {
			cviojni.image_ready();
		}
	}

	@Override
	public void takeImage() {
		//TODO: Consider synchronisation
		if (mImageReader == null) return;

		Image image = null;
		try {
			image = mImageReader.acquireLatestImage();
			if (image != null) {
				Image.Plane[] planes = image.getPlanes();
				ByteBuffer b = planes[0].getBuffer();
				// planes[0].getPixelStride() has to be 4 (32 bit)
				cviojni.push_pixels(b, planes[0].getRowStride());
			}
		} catch (Exception e) {
			Log.e(TAG, "ImageReader", e);
		} finally {
			if (image != null) {
				image.close();
			}
		}
	}

	/******************************************
	 * Stopping media projection
	 ****************/

	private class MediaProjectionStopCallback extends MediaProjection.Callback {
		@Override
		public void onStop() {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
				if (mVirtualDisplay != null) mVirtualDisplay.release();
				if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);
				if (mOrientationChangeCallback != null) mOrientationChangeCallback.disable();
				sMediaProjection.unregisterCallback(MediaProjectionStopCallback.this);
				sMediaProjection = null;

				vncServer.stop();
				stopRepeatingPing();
				}
			});
		}
	}


	/******************************************
	 * Orientation change listener
	 ****************/

	private class OrientationChangeCallback extends OrientationEventListener {
		OrientationChangeCallback(Context context) {
			super(context);
		}

		@Override
		public void onOrientationChanged(int orientation) {
			synchronized (this) {
				final int rotation = mDisplay.getRotation();
				if (rotation != mRotation) {
					mRotation = rotation;
					try {
						// clean up
						if (mVirtualDisplay != null) mVirtualDisplay.release();
						if (mImageReader != null)
							mImageReader.setOnImageAvailableListener(null, null);

						// re-create virtual display depending on device width / height
						createVirtualDisplay();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/******************************************
	 * Factoring Virtual Display creation
	 ****************/
	private void createVirtualDisplay() {
		// get width and height
		Point size = new Point();
		mDisplay.getRealSize(size);
		int mWidth = (int)(size.x / downscale);
		int mHeight = (int)(size.y / downscale);

		vncServer.stop();
		vncServer.start(mWidth, mHeight);
		startRepeatingPing();

		// start capture reader
		mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
		mVirtualDisplay = sMediaProjection.createVirtualDisplay("cviodisplay", mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, mHandler);
		mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);
	}


	/******************************************
	 * SeaCat Gateway ping
	 ****************/

	Runnable mPingRun = new Runnable() {
		@Override
		public void run() {
			try {
				SeaCatClient.ping();
			} catch (IOException e) {
				Log.e(TAG, "SeaCat ping", e);
			} finally {
				mHandler.postDelayed(mPingRun, 30 * 1000); // Ensure that we are connected every 30 seconds
			}
		}
	};

	protected void startRepeatingPing() {
		stopRepeatingPing();
		mPingRun.run();
	}

	void stopRepeatingPing() {
		mHandler.removeCallbacks(mPingRun);
	}

	/******************************************
	 * Input support
	 ****************/

	@Override
	public void rfbKbdAddEventProc(boolean down, long keySymCode, String client) {
		KeySym ks = KeySym.lookup.get((int) keySymCode);
		inputManager.onKeyboardEvent(down, ks);
	}

	@Override
	public void rfbKbdReleaseAllKeysProc(String client) {
		Log.d(TAG, "rfbKbdReleaseAllKeysProc: client:"+client);
	}

	///

	@Override
	public void rfbPtrAddEventProc(int buttonMask, int x, int y, String client) {
		inputManager.onMouseEvent(buttonMask, (int)(x * downscale), (int)(y * downscale));
	}

	///

	@Override
	public void rfbSetXCutTextProc(String text, String client) {
		Log.d(TAG, "rfbSetXCutTextProc: text:"+text+" client:"+client);
	}

	@Override
	public int rfbNewClientHook(String client) {
		Log.i(TAG, "New VNC client:"+client);
		return 0;
	}


	/// Internal utility methods


	private static String getApplicationMetaData(Context context, String name) {
		try {
			ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			Bundle bundle = ai.metaData;
			return bundle.getString(name);
		} catch (PackageManager.NameNotFoundException|NullPointerException e) {
			Log.e(TAG, "Unable to load application meta-data: " + e.getMessage());
		}
		return null;
	}
}
