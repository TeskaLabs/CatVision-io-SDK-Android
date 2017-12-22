package com.teskalabs.cvio;

import java.io.IOException;

import android.annotation.TargetApi;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;

import com.teskalabs.cvio.exceptions.CatVisionException;
import com.teskalabs.cvio.exceptions.CatVisionNotSupportedException;
import com.teskalabs.cvio.exceptions.MissingAPIKeyException;
import com.teskalabs.cvio.inapp.InAppInputManager;
import com.teskalabs.cvio.inapp.KeySym;

import com.teskalabs.seacat.android.client.CSR;
import com.teskalabs.seacat.android.client.SeaCatClient;
import com.teskalabs.seacat.android.client.message.JSONMessageTrigger;


public class CatVision extends ContextWrapper implements VNCDelegate {

	private static final String TAG = CatVision.class.getName();

	public static final String DEFAULT_CUSTOM_ID = "-DefaultCustomId-";
	private static final String PREFS_CUSTOM_ID_KEY = "customId";
	private static final String PREFS_NAME = "cvio.prefs";

	private static double downscale = 0;

	private MediaProjectionManager mProjectionManager = null;
	private static MediaProjection sMediaProjection = null;
	private static Thread sCaptureThread = null;

	private ImageReader mImageReader = null;
	private Handler mHandler = null;

	private VirtualDisplay mVirtualDisplay = null;

	private int mDensity;
	private Display mDisplay = null;

	private int mRotation;
	private OrientationChangeCallback mOrientationChangeCallback = null;

	private final VNCServer vncServer;
	private final InAppInputManager inputManager;

	private final String APIKeyId;
	private String customId = DEFAULT_CUSTOM_ID;

	private int mMediaProjectionPixelFormat = PixelFormat.RGBA_8888;

	static final int minAPILevel = Build.VERSION_CODES.LOLLIPOP;
	///

	static protected CatVision instance = null;

	public static CatVision initialize(Application app) {
		return initialize(app, false);
	}

	public synchronized static CatVision initialize(Application app, boolean hasCustomId) {

		if (instance != null) return instance;

		try {
			instance = new CatVision(app, hasCustomId);
		} catch (CatVisionNotSupportedException e) {
			Log.d(TAG, "CatVision.io SDK requires Android API level 21 and higher.");
			return null;
		}catch (Exception e) {
			return null;
		}

		return instance;
	}

	static public CatVision getInstance()
	{
		return instance;
	}

	///

	private CatVision(Application app, boolean hasCustomId) throws IOException, CatVisionException {
		super(app.getApplicationContext());

		// API level compatibility check
		if (android.os.Build.VERSION.SDK_INT < minAPILevel) {
			throw new CatVisionNotSupportedException("Can't initialize CatVision.io - The minimum supported API level is 21.");
		}

		APIKeyId = getApplicationMetaData(app.getApplicationContext(), "cvio.api_key_id");
		if (APIKeyId == null) {
			throw new MissingAPIKeyException("CatVision.io API key (cvio.api_key_id) not provided. See https://docs.catvision.io/get-started/api-key.html");
		}

		if (hasCustomId) {
			customId = null;
		}

		vncServer = new VNCServer(this, this);

		SeaCatClient.setPackageName("com.teskalabs.cvio");
		SeaCatClient.initialize(app.getApplicationContext(), new Runnable() {
			@Override
			public void run() {
				CatVision.this.submitCSR();
			}
		});

		inputManager = new InAppInputManager(app);

		vncServer.configureSeaCat();
	}

	synchronized void submitCSR()
	{
		if (customId == null)
		{
			Log.w(TAG, "Custom Id is null, cannot submit CSR");
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

		if (customId != DEFAULT_CUSTOM_ID)
		{
			csr.setUniqueIdentifier(customId);
		}

		try {
			csr.submit();
		} catch (IOException e) {
			Log.e(TAG, "Submitting CSR", e);
		}
	}

	/// Methods from SeaCatClient

	public void reset() throws IOException {
		SeaCatClient.reset();
	}

	public String getClientTag() {
		return SeaCatClient.getClientTag();
	}

	public String getClientId() {
		return SeaCatClient.getClientId();
	}

	public String getState() {
		return SeaCatClient.getState();
	}

	///

	public void resetCustomId()
	{
		SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.remove(PREFS_CUSTOM_ID_KEY);
		editor.apply();

		customId = null;

		try {
			this.reset();
		} catch (IOException e) {
			Log.e(TAG, "Client reset", e);
		}
	}

	public String getCustomId()
	{
		return this.customId;
	}

	public void setCustomId(String customId)
	{
		this.customId = customId;

		SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		if (sharedPref.contains(PREFS_CUSTOM_ID_KEY))
		{
			if (!sharedPref.getString(PREFS_CUSTOM_ID_KEY, "").equals(customId))
			{
				// Reset identity is required, CSR will be submitted asynchronously
				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putString(PREFS_CUSTOM_ID_KEY, customId);
				editor.apply();

				try {
					this.reset();
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

				// customId exists and CSR is submitted, client should be successfully onboarded now.
				//TODO: Check SeaCatClient.getState() for all logical combinations (CSR has to be generated, submitted or accepted at this moment)
				Log.w(TAG, "SeaCat client state: " + SeaCatClient.getState());
			}
		}

		else {
			// Fresh onboarding, custom id is new and CSR is to be submitted
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putString(PREFS_CUSTOM_ID_KEY, customId);
			editor.apply();

			submitCSR();
		}

	}

	///

	public void requestStart(Activity activity, int requestCode) {
		if (android.os.Build.VERSION.SDK_INT < minAPILevel) {
			Log.w(TAG, "Can't run requestStart() due to a low API level. API level 21 or higher is required.");
			return;
		} else {
			// call for the projection manager
			mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

			if (sCaptureThread == null) {
				// run capture handling thread
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
	}

	public void stop() {
		if (sCaptureThread == null) return;
		if (mHandler == null) return;

		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (android.os.Build.VERSION.SDK_INT < minAPILevel) {
					Log.w(TAG, "Can't run shutdown() due to a low API level. API level 21 or higher is required.");
					return;
				} else {
					if (sMediaProjection != null) {
						sMediaProjection.stop();

						try {
							mHandler.post(new JSONMessageTrigger("cvio-capture-stopped") {
								@Override
								public void onPostExecute() {
									Log.i(TAG, "Trigger 'cvio-capture-stopped' result:" + this.getResponseBody().toString());
								}
							}.put("ClientTag", CatVision.this.getClientTag()));
						} catch (Exception e) {
							Log.e(TAG, "Failed to trigger SeaCat event", e);
						}

					}
				}
			}
		});

		vncServer.shutdown();
		stopRepeatingPing();

	}

	public boolean isStarted() {
		return (sMediaProjection != null);
	}

	public void onActivityResult(Activity activity, int resultCode, Intent data) {

		if (android.os.Build.VERSION.SDK_INT < minAPILevel) {
			Log.w(TAG, "Can't run onActivityResult due to a low API level. API level 21 or higher is required.");
			return;
		}

		else {
			sMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
			if (sMediaProjection != null) {

				try {
					mHandler.post(new JSONMessageTrigger("cvio-capture-started")
						.put("ClientTag", CatVision.this.getClientTag())
						.put("AppId", getPackageName())
					);
				} catch (Exception e) {
					Log.e(TAG, "Failed to trigger SeaCat event", e);
				}

				// display metrics
				DisplayMetrics metrics = getResources().getDisplayMetrics();
				mDensity = metrics.densityDpi;
				mDisplay = activity.getWindowManager().getDefaultDisplay();

				if (downscale == 0) {
					if (mDensity < 280) {
						downscale = 1;
					} else if (mDensity < 400) {
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

				// register media projection shutdown callback
				sMediaProjection.registerCallback(new MediaProjectionStopCallback(), mHandler);
			}
		}
	}

	/******************************************
	 * Here we receive Images
	 ****************/

	@TargetApi(minAPILevel)
	private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
		@Override
		public void onImageAvailable(ImageReader reader) {
			vncServer.imageReady();
		}
	}

	@Override
	public int takeImage() {
		if (android.os.Build.VERSION.SDK_INT < minAPILevel) {
			Log.w(TAG, "Can't run takeImage() due to a low API level. API level 21 or higher is required.");
			return 1; // Ask for shutdown
		} else {
			//TODO: Consider synchronisation
			if (mImageReader == null) return 0;

			Image image = null;
			try {
				image = mImageReader.acquireLatestImage();
				if (image != null) {
					vncServer.push(image, mMediaProjectionPixelFormat);
				}
			} catch (UnsupportedOperationException e)
			{
				if (mImageReader.getImageFormat() == PixelFormat.RGBA_8888)
				{
					Log.w(TAG, "Swiching image format from RGBA_8888 to RGB_565");
					mMediaProjectionPixelFormat = PixelFormat.RGB_565;
					// New virtual display has to be created out of this JNI callback otherwise there will be a deadlock
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							createVirtualDisplay();
						}
					});
					return 1; // Ask for shutdown
				}
				else {
					Log.e(TAG, "ImageReader/UnsupportedOperationException", e);
				}
			} catch (Exception e) {
				Log.e(TAG, "ImageReader.Exception", e);
			} finally {
				if (image != null) {
					image.close();
				}
			}
		}
		return 0;
	}

	/******************************************
	 * Stopping media projection
	 ****************/

	@TargetApi(minAPILevel)
	private class MediaProjectionStopCallback extends MediaProjection.Callback {
		@Override
		public void onStop() {

			mHandler.post(new Runnable() {
				@Override
				public void run() {
				if (android.os.Build.VERSION.SDK_INT < minAPILevel) {
					Log.w(TAG, "Can't run MediaProjectionStopCallback.onStop() due to a low API level. API level 21 or higher is required.");
					return;
				}
				if (mVirtualDisplay != null) mVirtualDisplay.release();
				if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);
				if (mOrientationChangeCallback != null) mOrientationChangeCallback.disable();
				sMediaProjection.unregisterCallback(MediaProjectionStopCallback.this);
				sMediaProjection = null;

				vncServer.shutdown();
				stopRepeatingPing();
				}
			});
		}
	}


	/******************************************
	 * Orientation change listener
	 ****************/
	@TargetApi(minAPILevel)
	private class OrientationChangeCallback extends OrientationEventListener {
		OrientationChangeCallback(Context context) {
			super(context);
		}

		@Override
		public void onOrientationChanged(int orientation) {
			if (android.os.Build.VERSION.SDK_INT < minAPILevel) {
				Log.w(TAG, "Can't run onOrientationChanged due to a low API level. API level 21 or higher is required.");
				return;
			}
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
	@TargetApi(minAPILevel)
	private void createVirtualDisplay() {
		if (android.os.Build.VERSION.SDK_INT < minAPILevel) {
			Log.w(TAG, "Can't run createVirtualDisplay() due to a low API level. API level 21 or higher is required.");
			return;
		}
		// get width and height
		Point size = new Point();
		mDisplay.getRealSize(size);
		int mWidth = (int)(size.x / downscale);
		int mHeight = (int)(size.y / downscale);

		vncServer.shutdown();
		vncServer.run(mWidth, mHeight);
		startRepeatingPing();

		int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;

		// run capture reader
		mImageReader = ImageReader.newInstance(mWidth, mHeight, mMediaProjectionPixelFormat, 2);
		mVirtualDisplay = sMediaProjection.createVirtualDisplay("cvio", mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, mHandler);
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
		try {
			mHandler.post(new JSONMessageTrigger("cvio-new-client").put("VNCClient", client).put("ClientTag", CatVision.this.getClientTag()));
		} catch (Exception e) {
			Log.e(TAG, "Failed to trigger SeaCat event", e);
		}
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
