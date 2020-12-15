package io.catvision.appl;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class InfoActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_info);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		try {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setDisplayShowHomeEnabled(true);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

		// Getting data from the intent
		Intent data = getIntent();
		String clientTag = data.getStringExtra("client_tag");

		// Setting the client tag to the text view
		TextView clientTagView = (TextView)findViewById(R.id.client_tag_text);
		clientTagView.setText(clientTag);
	}

	@Override
	public boolean onSupportNavigateUp() {
		onBackPressed();
		return true;
	}
}
