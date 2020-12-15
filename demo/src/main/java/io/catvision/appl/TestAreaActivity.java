package io.catvision.appl;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class TestAreaActivity extends AppCompatActivity {

	private static final String TAG = TestAreaActivity.class.getName();

	private ImageView clickMark;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test_area);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		try {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setDisplayShowHomeEnabled(true);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

		clickMark = (ImageView) findViewById(R.id.clickMark);
	}

	@Override
	protected void onStart()
	{
		super.onStart();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
	}


	private int getRelativeLeft(View myView) {
		if (myView.getParent() == myView.getRootView())
			return myView.getLeft();
		else
			return myView.getLeft() + getRelativeLeft((View) myView.getParent());
	}

	private int getRelativeTop(View myView) {
		if (myView.getParent() == myView.getRootView())
			return myView.getTop();
		else
			return myView.getTop() + getRelativeTop((View) myView.getParent());
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int x = (int) event.getX() - getRelativeLeft((View)clickMark.getParent()) - (clickMark.getWidth() / 2);
		int y = (int) event.getY() - getRelativeTop((View)clickMark.getParent()) - (clickMark.getHeight() / 2);

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				clickMark.setX(x);
				clickMark.setY(y);
				break;

			case MotionEvent.ACTION_MOVE:
				clickMark.setX(x);
				clickMark.setY(y);
				break;

			case MotionEvent.ACTION_UP:
				break;
		}

		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.i(TAG, "onKeyDown: keyCode:"+keyCode+" event:"+event);
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onSupportNavigateUp() {
		onBackPressed();
		return true;
	}
}
