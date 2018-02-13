package io.catvision.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

// https://github.com/dm77/barcodescanner
public class QRCodeScannerActivity extends Activity implements ZXingScannerView.ResultHandler {
	private ZXingScannerView mScannerView;

	@Override
	public void onCreate(Bundle state) {
		super.onCreate(state);
		mScannerView = new ZXingScannerView(this);   // Programmatically initialize the scanner view
		setContentView(mScannerView);                // Set the scanner view as the content view
	}

	@Override
	public void onResume() {
		super.onResume();
		mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
		mScannerView.startCamera();          // Start camera on resume
	}

	@Override
	public void onPause() {
		super.onPause();
		mScannerView.stopCamera();           // Stop camera on pause
	}

	@Override
	public void handleResult(Result rawResult) {
		String format = rawResult.getBarcodeFormat().toString();
		if (format.equals("QR_CODE")) {
			try {
				// Reading the API key ID
				Uri uri = Uri.parse(rawResult.getText());
				String apikey = uri.getQueryParameter("apikey").replace(" ", "+");
				// Returning result
				Intent result = new Intent();
				result.putExtra("apikey_id", apikey);
				result.putExtra("format", format);
				setResult(Activity.RESULT_OK, result);
				finish();
			} catch (Exception e) {
				e.printStackTrace();
				mScannerView.resumeCameraPreview(this);
			}
		} else {
			// If you would like to resume scanning, call this method below:
			mScannerView.resumeCameraPreview(this);
		}
	}
}
