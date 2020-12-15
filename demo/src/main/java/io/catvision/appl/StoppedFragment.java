package io.catvision.appl;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link StoppedFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link StoppedFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StoppedFragment extends Fragment {
	// TODO: Rename parameter arguments, choose names that match
	// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
	private static final String ARG_PARAM1 = "param1";
	private static final String ARG_PARAM2 = "param2";

	// TODO: Rename and change types of parameters
	private String mParam1;
	private String mParam2;

	private OnFragmentInteractionListener mListener;

	public StoppedFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param param1 Parameter 1.
	 * @param param2 Parameter 2.
	 * @return A new instance of fragment StoppedFragment.
	 */
	// TODO: Rename and change types and number of parameters
	public static StoppedFragment newInstance(String param1, String param2) {
		StoppedFragment fragment = new StoppedFragment();
		Bundle args = new Bundle();
		args.putString(ARG_PARAM1, param1);
		args.putString(ARG_PARAM2, param2);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			mParam1 = getArguments().getString(ARG_PARAM1);
			mParam2 = getArguments().getString(ARG_PARAM2);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_stopped, container, false);
		// Checking if the API key is present
		refreshApiKeyRelatedView(view);
		return view;
	}

	public void onClick(View v) {
		if (mListener != null) {
			mListener.onFragmentInteractionStartRequest();
		}
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		if (context instanceof OnFragmentInteractionListener) {
			mListener = (OnFragmentInteractionListener) context;
		} else {
			throw new RuntimeException(context.toString()
				+ " must implement OnFragmentInteractionListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnFragmentInteractionListener {
		void onFragmentInteractionStartRequest();
	}

	// Custom methods ------------------------------------------------------------------------------

	/**
	 * Refreshes all elements related to the Api Key ID.
	 * @param view View
	 */
	public void refreshApiKeyRelatedView(View view) {
		if (view == null)
			view = getView();
		try {
			MainActivity mainActivity = (MainActivity) getActivity();
			String api_key = mainActivity.getPreferenceString(MainActivity.SAVED_API_KEY_ID);
			// Views
			Button buttonStart = (Button)view.findViewById(R.id.button2);
			if (api_key != null) {
				// Change the label of the button
				buttonStart.setText(getResources().getString(R.string.start_sharing));
			} else {
				// Change the label of the button
				buttonStart.setText(getResources().getString(R.string.start_connecting));
			}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
}
