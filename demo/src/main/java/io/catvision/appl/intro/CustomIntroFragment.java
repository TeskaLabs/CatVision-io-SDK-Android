package io.catvision.appl.intro;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import io.catvision.appl.IntroActivity;
import io.catvision.appl.R;

public final class CustomIntroFragment extends Fragment implements View.OnClickListener {
	private int type;

	public CustomIntroFragment() {

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// A new view
		View view = inflater.inflate(R.layout.custom_fragment_intro, container, false);

		// Getting the elements
		LinearLayout main = (LinearLayout)view.findViewById(R.id.main);
		TextView title = (TextView)view.findViewById(R.id.title);
		TextView description = (TextView)view.findViewById(R.id.description);
		ImageView image = (ImageView)view.findViewById(R.id.image);

		// Getting the arguments
		Bundle bundle = getArguments();
		try {
			type = bundle.getInt("type");
			CharSequence titleValue = bundle.getCharSequence("title");
			if (titleValue != null) {
				title.setText(titleValue);
			}
			CharSequence descriptionValue = bundle.getCharSequence("description");
			if (descriptionValue != null) {
				description.setText(descriptionValue);
			}
			int colorValue = bundle.getInt("color");
			main.setBackgroundColor(colorValue);
			int imageValue = bundle.getInt("image");
			image.setImageResource(imageValue);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}

		// OnClick
		image.setOnClickListener(this);

		return view;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.image:
				IntroActivity introActivity = (IntroActivity)getActivity();
				introActivity.click(type);
				break;
		}
	}
}
