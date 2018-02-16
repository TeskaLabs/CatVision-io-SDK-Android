package io.catvision.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import io.catvision.app.R;

public class ApiKeyObtainerActivity extends AppCompatActivity {
	// Permissions
	private static int CAMERA_PERMISSION = 201;
	// Requests
	private static int QR_CODE_REQUEST = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_api_key_obtainer);
		Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		try {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setDisplayShowHomeEnabled(true);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == QR_CODE_REQUEST) {
			if (resultCode == RESULT_OK) {
				setResult(resultCode, data);
				finish();
			}
		}
	}

	public void onClickQRCodeScan(View v) {
		if (isCameraPermissionGranted())
			startQRScanActivity();
	}

	public void onClickGetDeepLink(View v) {
		openURLinWebBrowser(getResources().getString(R.string.open_app_with_key_url));
		finish();
	}

	// Custom methods ------------------------------------------------------------------------------
	/**
	 * Starts an activity that retrieves the QR code.
	 */
	public void startQRScanActivity() {
		Intent intent = new Intent(getApplicationContext(), QRCodeScannerActivity.class);
		startActivityForResult(intent, QR_CODE_REQUEST);
	}

	/**
	 * Calls an intent to open a specified URL in the web browser.
	 * @param url String
	 */
	public void openURLinWebBrowser(String url) {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
	}

	public void obtainApiKeyByInput(View w) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Enter new Api Key id");

		// Set up the input
		final EditText input = new EditText(this);

		// Specify the type of input expected
		input.setInputType(InputType.TYPE_CLASS_TEXT);
		builder.setView(input);

		// Set up the buttons
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String ApiKeyId = input.getText().toString();
				// Checking
				if (ApiKeyId.equals("")) {
					dialog.cancel();
					return;
				}
				// Sending back
				Intent intent = new Intent();
				intent.putExtra("apikey_id", ApiKeyId);
				setResult(RESULT_OK, intent);
				finish();
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		builder.show();
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

	// Menu ----------------------------------------------------------------------------------------

	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	public boolean onMenuItemClickShowClientTag(MenuItem v) {
		return true;
	}

	public boolean onMenuItemClickResetIdentity(MenuItem v) {
		return true;
	}

	public boolean onMenuItemClickTestArea(MenuItem v) {
		return true;
	}

	public boolean onMenuItemClickOverrideApiKeyId(MenuItem v) {
		return true;
	}

	@Override
	public boolean onSupportNavigateUp() {
		onBackPressed();
		return true;
	}
}
