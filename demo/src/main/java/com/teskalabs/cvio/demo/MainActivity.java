package com.teskalabs.cvio.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.teskalabs.cvio.CatVision;
import com.teskalabs.seacat.android.client.SeaCatClient;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements StoppedFragment.OnFragmentInteractionListener, StartedFragment.OnFragmentInteractionListener {

	private BroadcastReceiver receiver;
	private FirebaseAnalytics mFirebaseAnalytics;
	private CatVision catvision;

	private int CATVISION_REQUEST_CODE = 100;
	private static final String TAG = MainActivity.class.getName();


	// Activity Lifecycle methods ------------------------------------------------------------------

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		catvision = CatVision.getInstance(this);
		catvision.setCustomId(CatVision.DEFAULT_CUSTOM_ID);

		if (findViewById(R.id.fragment_container) != null)
		{
			if (savedInstanceState == null)
			{
				Fragment firstFragment = new StoppedFragment();

				firstFragment.setArguments(getIntent().getExtras());

				getSupportFragmentManager().beginTransaction()
					.add(R.id.fragment_container, firstFragment)
					.commit();
			}
		}

		// Obtain the FirebaseAnalytics instance.
		mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
			if (intent.hasCategory(SeaCatClient.CATEGORY_SEACAT)) {
				String action = intent.getAction();
				if (action.equals(SeaCatClient.ACTION_SEACAT_CLIENTID_CHANGED)) {
//					clientTagTextView.setText(catvision.getClientTag());
					return;
				} else if (action.equals(SeaCatClient.ACTION_SEACAT_STATE_CHANGED)) {
//					statusTextView.setText(SeaCatClient.getState());
					return;
				}
			}
			else if (intent.hasCategory(CatVision.CATEGORY_CVIO)) {
				String action = intent.getAction();
				if (action.equals(CatVision.ACTION_CVIO_SHARE_STARTED)) {

					Fragment newFragment = new StartedFragment();
					FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
					ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
					ft.replace(R.id.fragment_container, newFragment);
					ft.commit();

					return;
				} else if (action.equals(CatVision.ACTION_CVIO_SHARE_STOPPED)) {
					Fragment newFragment = new StoppedFragment();

					FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
					ft.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
					ft.replace(R.id.fragment_container, newFragment);
					ft.commit();

					return;
				}
			}
			}
		};
	}

	@Override
	protected void onStart() {
		super.onStart();

		IntentFilter intentFilter;

		intentFilter = new IntentFilter();
		intentFilter.addCategory(CatVision.CATEGORY_CVIO);
		intentFilter.addAction(CatVision.ACTION_CVIO_SHARE_STARTED);
		intentFilter.addAction(CatVision.ACTION_CVIO_SHARE_STOPPED);
		registerReceiver(receiver, intentFilter);

		intentFilter = new IntentFilter();
		intentFilter.addCategory(SeaCatClient.CATEGORY_SEACAT);
		intentFilter.addAction(SeaCatClient.ACTION_SEACAT_STATE_CHANGED);
		intentFilter.addAction(SeaCatClient.ACTION_SEACAT_CSR_NEEDED);
		intentFilter.addAction(SeaCatClient.ACTION_SEACAT_CLIENTID_CHANGED);
		registerReceiver(receiver, intentFilter);
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(receiver);
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CATVISION_REQUEST_CODE) {
			catvision.onActivityResult(this, resultCode, data);

			Bundle bundle = new Bundle();
			bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "start_capture");
			bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "menu_item");
			mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

		}
	}


	private void shareTextUrl() {
		Intent share = new Intent(android.content.Intent.ACTION_SEND);
		share.setType("text/plain");
		share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

		// Add data to the intent, the receiving app will decide what to do with it.
		share.putExtra(Intent.EXTRA_SUBJECT, "CatVision.io - Remote Access Link");
		share.putExtra(Intent.EXTRA_TEXT, "Please use this link to access my screen remotely. https://www.catvision.io");

		startActivity(Intent.createChooser(share, "Choose where to send a remote access link."));
	}


	// Menu ----------------------------------------------------------------------------------------

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_main, menu);
		return true;
	}

	public boolean onMenuItemClickShowClientTag(MenuItem v) {
		String clientTag = catvision.getClientTag();
		Toast.makeText(this, "Client tag: "+ clientTag, Toast.LENGTH_LONG).show();
		return true;
	}

	public boolean onMenuItemClickResetIdentity(MenuItem v) {
		try {
			SeaCatClient.reset();
			Toast.makeText(this, "Client identity reset.", Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	public boolean onMenuItemClickTestArea(MenuItem v) {
		Intent intent = new Intent(getApplicationContext(), TestAreaActivity.class);
		startActivity(intent);
		return true;
	}


	// ---------------------------------------------------------------------------------------------

	public void onClickStartSharing(View v) {
		catvision.requestStart(this, CATVISION_REQUEST_CODE);
	}

	public void onClickStopSharing(View v) {
		catvision.stop();
	}

	public void onClickSendLink(View v) {
		shareTextUrl();
	}

	// Fragment callbacks --------------------------------------------------------------------------

	@Override
	public void onFragmentInteractionStartRequest() {
	}

	@Override
	public void onFragmentInteractionStopRequest() {
	}

}
