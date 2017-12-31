package com.teskalabs.cvio.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;

import com.teskalabs.cvio.CatVision;
import com.teskalabs.seacat.android.client.SeaCatClient;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private TextView clientTagTextView = null;
	private TextView statusTextView = null;
	private BroadcastReceiver receiver;

    private FirebaseAnalytics mFirebaseAnalytics;

	private CatVision catvision;
	private int CATVISION_REQUEST_CODE = 100;

	/****************************************** Activity Lifecycle methods ************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

		catvision = CatVision.getInstance(this);
		catvision.setCustomId(CatVision.DEFAULT_CUSTOM_ID);

        clientTagTextView = (TextView) findViewById(R.id.client_tag_text);
		statusTextView = (TextView) findViewById(R.id.status_text);

		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.hasCategory(SeaCatClient.CATEGORY_SEACAT))
				{
					String action = intent.getAction();
					if (action.equals(SeaCatClient.ACTION_SEACAT_CLIENTID_CHANGED)) {
						clientTagTextView.setText(catvision.getClientTag());
						return;
					}

					else if (action.equals(SeaCatClient.ACTION_SEACAT_STATE_CHANGED)) {
						statusTextView.setText(SeaCatClient.getState());
						return;
					}
				}
			}
		};
	}

	@Override
	protected void onStart()
	{
		super.onStart();

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(SeaCatClient.ACTION_SEACAT_STATE_CHANGED);
		intentFilter.addAction(SeaCatClient.ACTION_SEACAT_CSR_NEEDED);
		intentFilter.addAction(SeaCatClient.ACTION_SEACAT_CLIENTID_CHANGED);
		intentFilter.addCategory(SeaCatClient.CATEGORY_SEACAT);
		registerReceiver(receiver, intentFilter);

		clientTagTextView.setText(catvision.getClientTag());
		statusTextView.setText(SeaCatClient.getState());
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

			Bundle bundle = new Bundle();
			bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "start_capture");
			bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "menu_item");
			mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

		}
	}

    /****************************************** Menu *******************************/

    private final int menuItemStartCaptureId = 1;
    private final int menuItemStopCaptureId = 2;
	private final int menuItemResetIdentityId = 3;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        menu.clear();

        int menuGroup1Id = 1;

        if (catvision.isStarted()) {
			menu.add(menuGroup1Id, menuItemStopCaptureId, 1, "Stop capture");
        } else {
			menu.add(menuGroup1Id, menuItemStartCaptureId, 1, "Start capture");
			menu.add(menuGroup1Id, menuItemResetIdentityId, 1, "Reset identity");
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

			case menuItemResetIdentityId:
				try {
					SeaCatClient.reset();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return true;

			default:
                return super.onOptionsItemSelected(item);
        }
    }

	public void onClickDemoArea(View v) {
		Intent intent = new Intent(getApplicationContext(), TestAreaActivity.class);
		startActivity(intent);
	}

}
