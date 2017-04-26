package com.teskalabs.cvio.demo;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.teskalabs.seacat.android.client.SeaCatClient;

import java.io.IOException;

public class SplashActivity extends AppCompatActivity {

	private static final String TAG = "SplashActivity";

	private BroadcastReceiver receiver;
	private boolean closing;

	private TextView statusTextView;
	private TextView clientTagTextView;

	private Handler handler;
	private Runnable stateChecker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		statusTextView = (TextView) findViewById(R.id.statusTextView);
		clientTagTextView = (TextView) findViewById(R.id.clientTagTextView);

		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.hasCategory(SeaCatClient.CATEGORY_SEACAT))
				{
					String action = intent.getAction();
					if (action.equals(SeaCatClient.ACTION_SEACAT_STATE_CHANGED)) {
						SplashActivity.this.onStateChanged(intent.getStringExtra(SeaCatClient.EXTRA_STATE));
						return;
					}

					if (action.equals(SeaCatClient.ACTION_SEACAT_CLIENTID_CHANGED)) {
						SplashActivity.this.clientTagTextView.setText(intent.getStringExtra(SeaCatClient.EXTRA_CLIENT_TAG));
						return;
					}

				}

				Log.w(TAG, "Unexpected intent: "+intent);
			}
		};

		handler = new Handler();

		stateChecker = new Runnable()
		{
			@Override
			public void run()
			{
				SeaCatClient.broadcastState(); // This triggers initial delivery of the actual state
				handler.postDelayed(this, 500);
			}
		};

	}

	@Override
	protected void onStart()
	{
		super.onStart();

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(SeaCatClient.ACTION_SEACAT_STATE_CHANGED);
		intentFilter.addAction(SeaCatClient.ACTION_SEACAT_CLIENTID_CHANGED);
		intentFilter.addCategory(SeaCatClient.CATEGORY_SEACAT);
		registerReceiver(receiver, intentFilter);

		closing = false;
		stateChecker.run();

		clientTagTextView.setText(SeaCatClient.getClientTag());
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		handler.removeCallbacks(stateChecker);
		unregisterReceiver(receiver);
	}

	private synchronized void onStateChanged(String state)
	{
		statusTextView.setText(state);

		if ((state.charAt(3) == 'Y') && (state.charAt(4) == 'N') && (state.charAt(0) != 'f')) {
			if (closing == false) {
				// Close activity and go to Main Activity
				closing = true;
				startActivity(new Intent(SplashActivity.this, MainActivity.class));
				finish();
			}
		}

	}

	// Menu

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_splash, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();
		if (id == R.id.seacat_reset)
		{
			onResetSelected();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void onResetSelected()
	{
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which){
					case DialogInterface.BUTTON_POSITIVE:
						try {
							SeaCatClient.reset();
							Toast.makeText(getApplicationContext(), "Identity reset initiated.", Toast.LENGTH_LONG).show();
						} catch (IOException e) {
							Log.e(TAG, " SeaCatClient.reset:", e);
						}
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						//No button clicked
						break;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder
				.setMessage("Are you sure?")
				.setPositiveButton("Yes", dialogClickListener)
				.setNegativeButton("No", dialogClickListener)
				.show();
	}

}
