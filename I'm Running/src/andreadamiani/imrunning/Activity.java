package andreadamiani.imrunning;

import andreadamiani.imrunning.CheckDialogFragment.OnFragmentInteractionListener;
import android.content.Context;
import android.content.Intent;
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
	private DialogFragment dialog;
	private Integer prevRingerMode = null;
	private boolean restarting = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		text = (TextView) findViewById(R.id.text);
		button = (Button) findViewById(R.id.button);
		dialog = CheckDialogFragment.newInstance();
		if (savedInstanceState != null
				&& savedInstanceState.containsKey("RINGER_MODE")) {
			this.prevRingerMode = savedInstanceState.getInt("RINGER_MODE");
		}
		onNewIntent(getIntent());
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		if (savedInstanceState != null
				&& savedInstanceState.containsKey("RINGER_MODE")) {
			this.prevRingerMode = savedInstanceState.getInt("RINGER_MODE");
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if(intent.hasExtra("RUNNING_EXTRA")){
			restarting = true;
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (restarting) {
			dialog.show(getSupportFragmentManager(), "RUNNING");
			restarting = false;
		}
		onFragmentInteraction(Application.getInstance().isEnabled());
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
			Application.getInstance().setEnabled(true);
		} else {
			interceptCalls(false);
			text.setText(R.string.disabled);
			text.setTextColor(Color.argb(255, 204, 0, 51));
			button.setEnabled(false);
			Application.getInstance().setEnabled(false);
		}
	}

	private void interceptCalls(boolean enable) {
		AudioManager am;
		am = (AudioManager) getBaseContext().getSystemService(
				Context.AUDIO_SERVICE);

		if (!enable){
			if (prevRingerMode != null) {
				am.setRingerMode(prevRingerMode);
			}
			Application.getInstance().registerReceiver(false);
		} else if (!Application.getInstance().isEnabled()){
			prevRingerMode = am.getRingerMode();
			am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
			Application.getInstance().registerReceiver(true);
		}
	}

	@Override
	public void onClick(View view) {
		onFragmentInteraction(false);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (prevRingerMode != null) {
			outState.putInt("RINGER_MODE", prevRingerMode);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		interceptCalls(false);
	}
}
