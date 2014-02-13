package andreadamiani.imrunning;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

/**
 * A simple {@link android.support.v4.app.Fragment} subclass. Activities that
 * contain this fragment must implement the
 * {@link CheckDialogFragment.OnFragmentInteractionListener} interface to handle
 * interaction events. Use the {@link CheckDialogFragment#newInstance} factory
 * method to create an instance of this fragment.
 * 
 */
public class CheckDialogFragment extends DialogFragment implements OnClickListener, Callback {
	private OnFragmentInteractionListener mListener;
	private Handler timer;

	/**
	 * Use this factory method to create a new instance of this fragment.
	 * @return A new instance of fragment CheckDialogFragment.
	 */
	public static CheckDialogFragment newInstance() {
		CheckDialogFragment fragment = new CheckDialogFragment();
		return fragment;
	}

	public CheckDialogFragment() {
		// Required empty public constructor
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		timer = new Handler(this);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		timer.sendEmptyMessageDelayed(0, getResources().getInteger(R.integer.allert_duration));
	}
	
	@Override
	public void onPause() {
		super.onPause();
		timer.removeMessages(0);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		timer = null;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_check_dialog, container,
				false);
		view.findViewById(R.id.yes).setOnClickListener(this);
		view.findViewById(R.id.no).setOnClickListener(this);
		return view;
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()){
		case R.id.yes:
			mListener.onFragmentInteraction(true);
			this.dismiss();
			break;
		case R.id.no:
			this.onCancel(this.getDialog());
			break;
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnFragmentInteractionListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
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
	 * fragment to allow an interaction in this fragment to be communicated to
	 * the activity and potentially other fragments contained in that activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnFragmentInteractionListener {
		public void onFragmentInteraction(boolean enable);
	}

	@Override
	public boolean handleMessage(Message arg0) {
		mListener.onFragmentInteraction(true);
		this.dismiss();
		return true;
	}
	
	@Override
	public void onCancel(DialogInterface dialog) {
		super.onCancel(dialog);
		mListener.onFragmentInteraction(false);
		this.dismiss();
	}

}
