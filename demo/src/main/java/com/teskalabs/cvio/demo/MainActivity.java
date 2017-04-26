package com.teskalabs.cvio.demo;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.teskalabs.seacat.android.client.SeaCatClient;
import com.teskalabs.cvio.CatVision;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

	private BroadcastReceiver receiver;
	private TextView statusTextView;
	private TextView clientTagTextView;

	private CatVision catvision = null;
	private int CATVISION_REQUEST_CODE = 100;


    /****************************************** Activity Lifecycle methods ************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		statusTextView = (TextView) findViewById(R.id.statusTextView);
		clientTagTextView = (TextView) findViewById(R.id.clientTagTextView);

		catvision = CatVision.CreateOrGet(this);

		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.hasCategory(SeaCatClient.CATEGORY_SEACAT))
				{
					String action = intent.getAction();
					if (action.equals(SeaCatClient.ACTION_SEACAT_STATE_CHANGED)) {
						MainActivity.this.statusTextView.setText(intent.getStringExtra(SeaCatClient.EXTRA_STATE));
						return;
					}

					if (action.equals(SeaCatClient.ACTION_SEACAT_CLIENTID_CHANGED)) {
						MainActivity.this.clientTagTextView.setText(intent.getStringExtra(SeaCatClient.EXTRA_CLIENT_TAG));
						return;
					}

				}

				Log.w(TAG, "Unexpected intent: "+intent);
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

		statusTextView.setText(SeaCatClient.getState());
		clientTagTextView.setText(SeaCatClient.getClientTag());
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		unregisterReceiver(receiver);
	}


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CATVISION_REQUEST_CODE) {
			catvision.onActivityResult(this, resultCode, data);
        }
    }

    /****************************************** Menu *******************************/

    private final int menuItemStartCaptureId = 1;
    private final int menuItemStopCaptureId = 2;
    private final int menuItemResetIdentity = 11;
	private final int menuItemDisconnect = 21;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        menu.clear();

        int menuGroup1Id = 1;

        if (catvision.isStarted()) {
			menu.add(menuGroup1Id, menuItemStopCaptureId, 1, "Stop capture");
        } else {
			menu.add(menuGroup1Id, menuItemStartCaptureId, 1, "Start capture");
        }

        menu.add(2, menuItemResetIdentity, 2, "Reset identity");

		final String state = SeaCatClient.getState();
		if (state.charAt(0) == 'E')
		{
			menu.add(3, menuItemDisconnect, 2, "Disconnect");
		}

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case menuItemStartCaptureId:
				catvision.requestStart(this, CATVISION_REQUEST_CODE);
                return true;

            case menuItemStopCaptureId:
				catvision.stop();
                return true;

            case menuItemResetIdentity:
				onResetSelected();
				return true;

			case menuItemDisconnect:
				try {
					SeaCatClient.disconnect();
				} catch (IOException e) {
					Log.e(TAG, "Exception during SeaCatClient.disconnect()", e);
				}
				return true;

			default:
                return super.onOptionsItemSelected(item);
        }
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
							startActivity(new Intent(MainActivity.this, SplashActivity.class));
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

	public void onClickDemoArea(View v) {
		Intent intent = new Intent(getApplicationContext(), TestAreaActivity.class);
		startActivity(intent);
	}

}
