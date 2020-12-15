package io.catvision.appl;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.google.firebase.analytics.FirebaseAnalytics;

import io.catvision.appl.intro.CustomIntroFragment;

public class IntroActivity extends AppIntro {
	public static int TYPE_NONE = 0;
	public static int TYPE_QR = 1;
	public static int TYPE_DEEP = 2;

	// Permissions
	private static int CAMERA_PERMISSION = 21;
	// Requests
	private static int QR_CODE_REQUEST = 31;
	// Variables
	private FirebaseAnalytics mFirebaseAnalytics;
	private AppIntroFragment firstFragment;
	private AppIntroFragment lastFragment;

	/**
	 * Does an action depending on the specified type from the fragment.
	 * @param type int
	 */
	public void click(int type) {
		switch (type) {
			case 1:
				if (isCameraPermissionGranted())
					startQRScanActivity();
				break;
			case 2:
				Intent intent = getIntent();
				intent.putExtra("type", TYPE_DEEP);
				setResult(RESULT_OK, intent);
				finish();
				break;
			default:
				break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == QR_CODE_REQUEST) {
			if (resultCode == RESULT_OK) {
				data.putExtra("type", TYPE_QR); // specify the event
				setResult(resultCode, data);
				finish();
			}
		}
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Note here that we DO NOT use setContentView();
		// Add your slide fragments here.
		// AppIntro will automatically generate the dots indicator and buttons.
		// addSlide(firstFragment);

		// Special custom fragments
		// Pairing #1
		Fragment customFragment1 = new CustomIntroFragment();
		Bundle b1 = new Bundle();
		b1.putCharSequence("title", getResources().getString(R.string.intro_third_title));
		b1.putCharSequence("description", getResources().getString(R.string.intro_third_text));
		b1.putInt("image", R.drawable.cat3);
		b1.putInt("color", getResources().getColor(R.color.colorSlide3));
		b1.putInt("type", TYPE_QR);
		customFragment1.setArguments(b1);

		// Pairing #2
		Fragment customFragment2 = new CustomIntroFragment();
		Bundle b2 = new Bundle();
		b2.putCharSequence("title", getResources().getString(R.string.intro_fourth_title));
		b2.putCharSequence("description", getResources().getString(R.string.intro_fourth_text));
		b2.putInt("image", R.drawable.cat4);
		b2.putInt("color", getResources().getColor(R.color.colorSlide4));
		b2.putInt("type", TYPE_DEEP);
		customFragment2.setArguments(b2);

		// Instead of fragments, you can also use our default slide
		// Just set a title, description, background and image. AppIntro will do the rest.
		firstFragment = AppIntroFragment.newInstance(getResources().getString(R.string.intro_first_title), getResources().getString(R.string.intro_first_text), R.drawable.cat1, getResources().getColor(R.color.colorSlide1));
		addSlide(firstFragment);
		addSlide(AppIntroFragment.newInstance(getResources().getString(R.string.intro_second_title), getResources().getString(R.string.intro_second_text), R.drawable.cat2, getResources().getColor(R.color.colorSlide2)));

		addSlide(customFragment1);
		addSlide(customFragment2);

		lastFragment = AppIntroFragment.newInstance(getResources().getString(R.string.intro_fifth_title), getResources().getString(R.string.intro_fifth_text), R.drawable.cat5, getResources().getColor(R.color.colorSlide5));
		addSlide(lastFragment);
		// setting the title
		setTitle(getResources().getString(R.string.intro_title_first));

		// OPTIONAL METHODS
		// Override bar/separator color.
		setBarColor(getResources().getColor(R.color.colorPrimary));
		// setSeparatorColor(Color.parseColor("#2196F3"));

		// Hide Skip/Done button.
		showSkipButton(true);
		setProgressButtonEnabled(true);

		// Turn vibration on and set intensity.
		// NOTE: you will probably need to ask VIBRATE permission in Manifest.
		// setVibrate(true);
		// setVibrateIntensity(30);

		mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onSkipPressed(Fragment currentFragment) {
		super.onSkipPressed(currentFragment);
		// Do something when users tap on Skip button.
		finish();
	}

	@Override
	public void onDonePressed(Fragment currentFragment) {
		super.onDonePressed(currentFragment);
		// Do something when users tap on Done button.
		finish();
	}

	@Override
	public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
		super.onSlideChanged(oldFragment, newFragment);
		// Do something when the slide changes.
		try {
			if (newFragment != firstFragment && newFragment != lastFragment) {
				setTitle(getResources().getString(R.string.intro_title_other));
			} else {
				setTitle(getResources().getString(R.string.intro_title_first));
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Starts an activity that retrieves the QR code.
	 */
	public void startQRScanActivity() {
		mFirebaseAnalytics.logEvent(getResources().getString(R.string.event_pair_qr), new Bundle());
		Intent intent = new Intent(getApplicationContext(), QRCodeScannerActivity.class);
		startActivityForResult(intent, QR_CODE_REQUEST);
	}

	// Permissions ---------------------------------------------------------------------------------
	/**
	 * Checks if it is allowed to use the camera.
	 * @return boolean
	 */
	public  boolean isCameraPermissionGranted() {
		if (Build.VERSION.SDK_INT >= 23) {
			if (checkSelfPermission(android.Manifest.permission.CAMERA)
					== PackageManager.PERMISSION_GRANTED) {
				return true;
			} else {
				ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION);
				return false;
			}
		} else {
			return true;
		}
	}

	/**
	 * Continues after the permission is obtained.
	 * @param requestCode int
	 * @param permissions @NonNull String[]
	 * @param grantResults @NonNull int[]
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			if (requestCode == CAMERA_PERMISSION) {
				startQRScanActivity();
			}
		}
	}
}