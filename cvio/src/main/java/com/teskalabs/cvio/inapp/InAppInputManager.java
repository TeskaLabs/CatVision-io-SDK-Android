package com.teskalabs.cvio.inapp;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InAppInputManager implements Application.ActivityLifecycleCallbacks {

	private static final String TAG = InAppInputManager.class.getName();

	private final Object currentActivityLock = new Object(); // Synchronize access to currentActivity and currentRootView
	private WeakReference<Activity> currentActivity = null;

	private boolean button1Pressed = false;
	private int metaState = 0; // State of the meta keys

	public InAppInputManager(Application app) {
		app.registerActivityLifecycleCallbacks(this);
	}

	private View obtainTargetView() {
		final Activity a = obtainActivity();
		//Log.i(TAG, "obtainTargetView a:"+a);
		if (a == null) return null;

		View v = a.findViewById(android.R.id.content).getRootView();
		//Log.i(TAG, "obtainTargetView vr:"+a);
		if (v == null) return null;
		if (v.hasWindowFocus()) return v; // Quick way

		List<View> lv = getWindowManagerViews();
		for(View vi : lv)
		{
			//Log.i(TAG, "obtainTargetView vi:"+vi+" f:"+vi.hasWindowFocus());
			if (vi.hasWindowFocus()) return vi;
		}

		return v;
	}

	private Activity obtainActivity() {
		synchronized (currentActivityLock) {
			if (currentActivity == null) return null;
			return currentActivity.get();
		}
	}

		//

	public void onMouseEvent(int buttonMask, int x, int y) {

		if ((!button1Pressed) && ((buttonMask & 1) != 0))
		{
			injectTouchEvent(1, MotionEvent.ACTION_DOWN, x, y);
			button1Pressed = true;
		}

		else if (button1Pressed)
		{
			if ((buttonMask & 1) == 0)
			{
				injectTouchEvent(1, MotionEvent.ACTION_UP, x, y);
				button1Pressed = false;
			}

			else injectTouchEvent(1, MotionEvent.ACTION_MOVE, x, y);
		}
	}

	private void injectTouchEvent(int buttonId, int event, int x, int y)
	{
		final View view = obtainTargetView();
		if (view == null) return;
		final Activity activity = obtainActivity();
		if (activity == null) return;

		int viewLocation[] = new int[2];
		view.getLocationOnScreen(viewLocation);

		MotionEvent.PointerProperties pp = new MotionEvent.PointerProperties();
		pp.toolType = MotionEvent.TOOL_TYPE_FINGER;
		pp.id = 0;
		MotionEvent.PointerProperties[] pps = new MotionEvent.PointerProperties[]{pp};

		MotionEvent.PointerCoords pc = new MotionEvent.PointerCoords();
		pc.size = 1;
		pc.pressure = 1;
		pc.x = x - viewLocation[0];
		pc.y = y - viewLocation[1];
		MotionEvent.PointerCoords[] pcs = new MotionEvent.PointerCoords[]{pc};

		long t = SystemClock.uptimeMillis();

		final MotionEvent e = MotionEvent.obtain(
				t,          // long downTime
				t + 100,    // long eventTime
				event,      // int action
				pps.length, // int pointerCount
				pps,        // MotionEvent.PointerProperties[] pointerProperties
				pcs,        // MotionEvent.PointerCoords[] pointerCoords
				0,          // int metaState
				0,          // int buttonState
				1,          // float xPrecision
				1,          // float yPrecision
				1,          // int deviceId
				0,          // int edgeFlags
				InputDevice.SOURCE_TOUCHSCREEN, //int source
				0           // int flags
		);

		activity.runOnUiThread(new Runnable() {
			public void run() {

				view.dispatchTouchEvent(e);
			}
		});
	}

	///

	public void onKeyboardEvent(boolean down, KeySym ks)
	{
		if ((ks == KeySym.XK_Escape) && ((this.metaState & (KeyEvent.META_SHIFT_LEFT_ON | KeyEvent.META_SHIFT_RIGHT_ON)) != 0))
		{
			if (!down) return;
			injectBackEvent();
			return;
		}

		if (ks.keyeventCode != -1)
		{
			injectKeyboardEvent(down, ks);

			if (ks.metaState != 0)
			{
				if (down) this.metaState |= ks.metaState;
				else this.metaState &= ~ks.metaState;
			}

			return;
		}

		Log.i(TAG, "onKeyboardEvent: down:"+down+" keySym:"+ks);


	}

	private void injectKeyboardEvent(boolean down, KeySym ks) {
		final View view = obtainTargetView();
		if (view == null) return;
		final Activity activity = obtainActivity();
		if (activity == null) return;

		long t = SystemClock.uptimeMillis();

		int action = KeyEvent.ACTION_UP;
		if (down) action = KeyEvent.ACTION_DOWN;

		final KeyEvent e = new KeyEvent(
				t,               // downTime
				t + 100,         // eventTime
				action,          // action (ACTION_DOWN / ACTION_UP)
				ks.keyeventCode, // code
				0,               // Repeat
				this.metaState,
				0,              // Device Id
				0               //ks.code
		);

//		Log.i(TAG, "Injecting:"+e);

		activity.runOnUiThread(new Runnable() {
			public void run() {
				view.dispatchKeyEvent(e);
			}
		});
	}

	///

	private void injectBackEvent()
	{
		final Activity activity = obtainActivity();
		if (activity == null) return;

		activity.runOnUiThread(new Runnable() {
			public void run() {
				activity.onBackPressed();
			}
		});
	}

	///

	@Override
	public void onActivityCreated(Activity activity, Bundle bundle) {

	}

	@Override
	public void onActivityStarted(Activity activity) {
		Log.i(TAG, "onActivityStarted:" + activity);
		synchronized (currentActivityLock) {
			this.currentActivity = new WeakReference<>(activity);
		}
	}

	@Override
	public void onActivityResumed(Activity activity) {
		Log.i(TAG, "onActivityResumed:" + activity);
		synchronized (currentActivityLock) {
			this.currentActivity = new WeakReference<>(activity);
		}
	}

	@Override
	public void onActivityPaused(Activity activity) {
		Log.i(TAG, "onActivityPaused:" + activity);
		synchronized (currentActivityLock) {
			if ((this.currentActivity != null) && (this.currentActivity.get() == activity)) {
				this.currentActivity = null;
			}
		}
	}

	@Override
	public void onActivityStopped(Activity activity) {
		Log.i(TAG, "onActivityStopped:" + activity);
		synchronized (currentActivityLock) {
			if ((this.currentActivity != null) && (this.currentActivity.get() == activity)) {
				this.currentActivity = null;
			}
		}
	}

	@Override
	public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
	}

	@Override
	public void onActivityDestroyed(Activity activity) {

	}

	///

	private static List<View> getWindowManagerViews() {
		try {

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH &&
					Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {

				// get the list from WindowManagerImpl.mViews
				Class wmiClass = Class.forName("android.view.WindowManagerImpl");
				Object wmiInstance = wmiClass.getMethod("getDefault").invoke(null);

				return viewsFromWM(wmiClass, wmiInstance);

			} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {

				// get the list from WindowManagerGlobal.mViews
				Class wmgClass = Class.forName("android.view.WindowManagerGlobal");
				Object wmgInstance = wmgClass.getMethod("getInstance").invoke(null);

				return viewsFromWM(wmgClass, wmgInstance);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return new ArrayList<View>();
	}

	private static List<View> viewsFromWM(Class wmClass, Object wmInstance) throws Exception {

		Field viewsField = wmClass.getDeclaredField("mViews");
		viewsField.setAccessible(true);
		Object views = viewsField.get(wmInstance);

		if (views instanceof List) {
			return (List<View>) viewsField.get(wmInstance);
		} else if (views instanceof View[]) {
			return Arrays.asList((View[])viewsField.get(wmInstance));
		}

		return new ArrayList<View>();
	}

}
