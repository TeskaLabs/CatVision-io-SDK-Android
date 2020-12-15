package io.catvision.appl;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.teskalabs.cvio.CatVision;
import com.teskalabs.seacat.android.client.SeaCatClient;

import pl.droidsonroids.gif.GifTextView;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements StoppedFragment.OnFragmentInteractionListener, StartedFragment.OnFragmentInteractionListener {

	private BroadcastReceiver receiver;
	private FirebaseAnalytics mFirebaseAnalytics;
	private CatVision catvision;
	// Requests
	private int CATVISION_REQUEST_CODE = 100;
	private int API_KEY_OBTAINER_REQUEST = 101;
	private int INTRO_REQUEST = 102;
	private static final String TAG = MainActivity.class.getName();
	// Preferences
	public static String SAVED_API_KEY_ID = "SAVED_API_KEY_ID";
	public static String NOT_FIRST_TIME = "NOT_FIRST_TIME";


	// Activity Lifecycle methods ------------------------------------------------------------------

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// title
		setTitle(getResources().getString(R.string.title_activity_main));

		catvision = CatVision.getInstance(this);
		catvision.setCustomId(CatVision.DEFAULT_CUSTOM_ID);

		if (findViewById(R.id.fragment_container) != null)
		{
			if (savedInstanceState == null)
			{
				Fragment firstFragment = new StoppedFragment();

				firstFragment.setArguments(getIntent().getExtras());

				getSupportFragmentManager().beginTransaction()
						.add(R.id.fragment_container, firstFragment, StoppedFragment.class.toString())
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
						ft.replace(R.id.fragment_container, newFragment, StartedFragment.class.toString());
						ft.commit();

						return;
					} else if (action.equals(CatVision.ACTION_CVIO_SHARE_STOPPED)) {
						Fragment newFragment = new StoppedFragment();

						FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
						ft.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
						ft.replace(R.id.fragment_container, newFragment, StoppedFragment.class.toString());
						ft.commit();

						return;
					}
				}
			}
		};

		// Deep linking
		Uri data = this.getIntent().getData();
		if (data != null && data.isHierarchical()) {
			this.setIntent(new Intent());
			// Setting the API key
			String apikey = data.getQueryParameter("apikey").replace(" ", "+");
			setApiKeyId(apikey);
			// Showing a dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getResources().getString(R.string.app_name));
			builder.setMessage(getResources().getString(R.string.dl_dialog_message));
			builder.setCancelable(true);
			builder.setPositiveButton(
					getResources().getString(R.string.dialog_button_ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
			AlertDialog alert = builder.create();
			alert.show();
		}

		// Only for the first time
		if (!getPreferenceBoolean(NOT_FIRST_TIME)) {
			loadTutorial();
		}
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
		if (requestCode == INTRO_REQUEST) {
			savePreferenceBoolean(NOT_FIRST_TIME, true);
			if (data != null) {
				int type = data.getIntExtra("type", IntroActivity.TYPE_NONE);
				if (type == IntroActivity.TYPE_QR) {
					setApiKeyFromResource(data);
				} else if (type == IntroActivity.TYPE_DEEP) {
					mFirebaseAnalytics.logEvent(getResources().getString(R.string.event_pair_link), new Bundle());
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(R.string.open_app_with_key_url))));
				}
			}
		} else if (requestCode == CATVISION_REQUEST_CODE) {
			catvision.onActivityResult(this, resultCode, data);

			Bundle bundle = new Bundle();
			bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "start_capture");
			bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "menu_item");
			mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

		} else if (requestCode == API_KEY_OBTAINER_REQUEST) {
			if (resultCode == RESULT_OK) {
				setApiKeyFromResource(data);
			}
		}
	}

	/**
	 * Setting the API key ID from a specified resource.
	 * @param data
	 */
	public void setApiKeyFromResource(Intent data) {
		// Setting a new API key from the scan
		String apikey_id = data.getStringExtra("apikey_id");
		if (apikey_id != null) {
			// Setting the API key
			setApiKeyId(apikey_id);
			// Showing a dialog
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getResources().getString(R.string.app_name));
			builder.setMessage(getResources().getString(R.string.qr_dialog_message));
			builder.setCancelable(true);
			builder.setPositiveButton(
					getResources().getString(R.string.dialog_button_ok),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});
			AlertDialog alert = builder.create();
			alert.show();
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case R.id.menu_show_tag:
				onMenuItemClickShowClientTag(item);
				return true;
			case R.id.menu_test_area:
				onMenuItemClickTestArea(item);
				return true;
			case R.id.menu_reset:
				onMenuItemClickResetIdentity(item);
				return true;
			case R.id.menu_help:
				onMenuItemClickHelp(item);
				return true;
			case R.id.menu_about:
				onMenuItemClickAbout(item);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public boolean onMenuItemClickShowClientTag(MenuItem v) {
		// Getting the client tag
		String clientTag = catvision.getClientTag();
		// Starting the InfoActivity
		Intent intent = new Intent(getApplicationContext(), InfoActivity.class);
		intent.putExtra("client_tag", "Client tag: "+ clientTag);
		startActivity(intent);
		return true;
	}

	public boolean onMenuItemClickResetIdentity(MenuItem v) {
		// Showing a dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getResources().getString(R.string.dlg_unpair_title));
		builder.setMessage(getResources().getString(R.string.dlg_unpair_text));
		builder.setCancelable(true);
		builder.setPositiveButton(
				getResources().getString(R.string.dialog_button_ok),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
						try {
							SeaCatClient.reset();
							// Save also in this context
							savePreferenceString(SAVED_API_KEY_ID, null);
							refreshFragments();
							// Toast
							Toast.makeText(MainActivity.this, "Device unpaired.", Toast.LENGTH_LONG).show();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
		builder.setNegativeButton(
				getResources().getString(R.string.dialog_button_cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();
		return true;
	}

	public boolean onMenuItemClickTestArea(MenuItem v) {
		// Now using io.catvision.app.tictactoe.GameActivity.class instead of TestAreaActivity.class
		Intent intent = new Intent(this, io.catvision.appl.tictactoe.GameActivity.class);
		startActivity(intent);
		return true;
	}

	public boolean onMenuItemClickAbout(MenuItem v) {
		Intent intent = new Intent(getApplicationContext(), AboutActivity.class);
		startActivity(intent);
		return true;
	}

	public boolean onMenuItemClickHelp(MenuItem v) {
		Intent intent = new Intent(getApplicationContext(), HelpActivity.class);
		startActivity(intent);
		return true;
	}

	// ---------------------------------------------------------------------------------------------
	public void onClickStartSharing(View v) {
		String api_key = getPreferenceString(SAVED_API_KEY_ID);
		if (api_key != null) {
			mFirebaseAnalytics.logEvent(getResources().getString(R.string.event_share_me), new Bundle());
			catvision.requestStart(this, CATVISION_REQUEST_CODE);
		} else {
			mFirebaseAnalytics.logEvent(getResources().getString(R.string.event_pair_me), new Bundle());
			startQRScanActivity();
		}
	}

	public void onClickStopSharing(View v) {
		catvision.stop();
	}

	public void onClickSendLink(View v) {
		shareTextUrl();
	}

	public void onClickMainImage(View v) {
		GifTextView gifTextView = (GifTextView)findViewById(R.id.gifView);
		gifTextView.setVisibility(View.VISIBLE);
		gifTextView.setBackgroundResource(R.drawable.catvision_blink);
	}

	// Fragment callbacks --------------------------------------------------------------------------

	@Override
	public void onFragmentInteractionStartRequest() {
	}

	@Override
	public void onFragmentInteractionStopRequest() {
	}

	// Custom methods ------------------------------------------------------------------------------
	/**
	 * Sets the API key to CatVision.
	 * @param apiKeyId String
	 */
	public void setApiKeyId(String apiKeyId) {
		CatVision.resetWithAPIKeyId(MainActivity.this, apiKeyId);
		// Save also in this context
		savePreferenceString(SAVED_API_KEY_ID, apiKeyId);
		refreshFragments();
	}

	/**
	 * Refreshes fragments when some important value changes.
	 */
	public void refreshFragments() {
		// Refresh fragments
		StoppedFragment fragmentStopped = (StoppedFragment)getSupportFragmentManager().findFragmentByTag(StoppedFragment.class.toString());
		if (fragmentStopped != null) {
			fragmentStopped.refreshApiKeyRelatedView(null);
		}
	}

	/**
	 * Starts an activity that retrieves the Api Key ID.
	 */
	public void startQRScanActivity() {
		Intent intent = new Intent(getApplicationContext(), ApiKeyObtainerActivity.class);
		startActivityForResult(intent, API_KEY_OBTAINER_REQUEST);
	}

	/**
	 * Getting a string from shared preferences.
	 * @param name String
	 * @return String
	 */
	public String getPreferenceString(String name) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		return settings.getString(name, null);
	}

	/**
	 * Getting a boolean from shared preferences.
	 * @param name String
	 * @return boolean
	 */
	public boolean getPreferenceBoolean(String name) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		return settings.getBoolean(name, false);
	}

	/**
	 * Setting a string to shared preferences.
	 * @param name String
	 * @param value String
	 */
	public void savePreferenceString(String name, String value) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.putString(name, value);
		editor.apply();
	}

	/**
	 * Setting a boolean to shared preferences.
	 * @param name String
	 * @param value boolean
	 */
	public void savePreferenceBoolean(String name, boolean value) {
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
		editor.putBoolean(name, value);
		editor.apply();
	}

	@Override
	public boolean onSupportNavigateUp() {
		onBackPressed();
		return true;
	}

	// Tutorial ------------------------------------------------------------------------------------
	/**
	 * Shows the tutorial slider to the user.
	 */
	public void loadTutorial() {
		Intent introAct = new Intent(this, IntroActivity.class);
		startActivityForResult(introAct, INTRO_REQUEST);
	}
}
