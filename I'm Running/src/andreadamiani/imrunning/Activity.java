package andreadamiani.imrunning;

import andreadamiani.imrunning.CheckDialogFragment.OnFragmentInteractionListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class Activity extends FragmentActivity implements
		OnFragmentInteractionListener, OnClickListener {

	private TextView text;
	private Button button;
	private BroadcastReceiver receiver;
	private DialogFragment dialog;
	private boolean restoring = false;
	private Integer prevRingerMode = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		text = (TextView) findViewById(R.id.text);
		button = (Button) findViewById(R.id.button);
		dialog = CheckDialogFragment.newInstance();
		if (savedInstanceState != null && savedInstanceState.containsKey("ENABLED")) {
			this.onFragmentInteraction(savedInstanceState.getBoolean("ENABLED"));
			restoring = true;
		}
		if (savedInstanceState != null && savedInstanceState.containsKey("RINGER_MODE")) {
			this.prevRingerMode = savedInstanceState.getInt("RINGER_MODE");
		}
		receiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context arg0, Intent arg1) {
				if (arg1.getAction().equals("andreadamiani.coda.RUNNING")) {
					dialog.show(Activity.this.getSupportFragmentManager(),
							"RUNNING");
				}
			}
		};
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState != null && savedInstanceState.containsKey("ENABLED")) {
			this.onFragmentInteraction(savedInstanceState.getBoolean("ENABLED"));
			restoring = true;
		}
		if (savedInstanceState != null && savedInstanceState.containsKey("RINGER_MODE")) {
			this.prevRingerMode = savedInstanceState.getInt("RINGER_MODE");
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (getIntent().hasExtra("RUNNING_EXTRA")) {
			dialog.show(getSupportFragmentManager(), "RUNNING");
		} else if (!restoring) {
			onFragmentInteraction(restoring);
		}
		registerReceiver(receiver, new IntentFilter("andreadamiani.coda.LATE"));
		Application.getInstance().setAppStarted(true);
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(receiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.options, menu);
		return true;
	}

	@Override
	public void onFragmentInteraction(boolean enable) {
		if (enable) {
			interceptCalls(true);
			text.setText(R.string.enabled);
			text.setTextColor(Color.GREEN);
			button.setEnabled(true);
		} else {
			interceptCalls(false);
			text.setText(R.string.disabled);
			text.setTextColor(Color.argb(255, 204, 0, 51));
			button.setEnabled(false);
		}
	}

	private void interceptCalls(boolean enable) {
		AudioManager am;
		am = (AudioManager) getBaseContext().getSystemService(
				Context.AUDIO_SERVICE);

		if (prevRingerMode == null) {
			prevRingerMode = am.getRingerMode();
		}

		if (enable) {
			am.setRingerMode(prevRingerMode);
			Application.getInstance().registerReceiver(true);
		} else {
			am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
			Application.getInstance().registerReceiver(false);
		}
	}

	@Override
	public void onClick(View view) {
		onFragmentInteraction(false);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("ENABLED", button.isEnabled());
		if (prevRingerMode != null) {
			outState.putInt("RINGER_MODE", prevRingerMode);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		interceptCalls(false);
		Application.getInstance().setAppStarted(false);
	}
}
