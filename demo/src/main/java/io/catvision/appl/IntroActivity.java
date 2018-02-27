package io.catvision.appl;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;

import io.catvision.appl.R;

public class IntroActivity extends AppIntro {
	private AppIntroFragment firstFragment;
	private AppIntroFragment lastFragment;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Note here that we DO NOT use setContentView();
		// Add your slide fragments here.
		// AppIntro will automatically generate the dots indicator and buttons.
		// addSlide(firstFragment);

		// Instead of fragments, you can also use our default slide
		// Just set a title, description, background and image. AppIntro will do the rest.
		firstFragment = AppIntroFragment.newInstance(getResources().getString(R.string.intro_first_title), getResources().getString(R.string.intro_first_text), R.drawable.cat1, getResources().getColor(R.color.colorSlide1));
		addSlide(firstFragment);
		addSlide(AppIntroFragment.newInstance(getResources().getString(R.string.intro_second_title), getResources().getString(R.string.intro_second_text), R.drawable.cat2, getResources().getColor(R.color.colorSlide2)));
		addSlide(AppIntroFragment.newInstance(getResources().getString(R.string.intro_third_title), getResources().getString(R.string.intro_third_text), R.drawable.cat3, getResources().getColor(R.color.colorSlide3)));
		addSlide(AppIntroFragment.newInstance(getResources().getString(R.string.intro_fourth_title), getResources().getString(R.string.intro_fourth_text), R.drawable.cat4, getResources().getColor(R.color.colorSlide4)));
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
		setVibrate(true);
		setVibrateIntensity(30);
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
}