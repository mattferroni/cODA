package andreadamiani.coda;

import andreadamiani.coda.observers.Observer.ObsAction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

public class Launcher extends BroadcastReceiver {

	private static final String DEBUG_TAG = "[cODA] LAUNCHER";

	public Launcher() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)
				|| intent.getAction().equals(Intent.ACTION_BATTERY_OKAY)) {
			Intent battery = Application.getInstance().getApplicationContext().registerReceiver(null, new IntentFilter(
					Intent.ACTION_BATTERY_CHANGED));
			if (battery.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) <= 20) {
				Log.d(DEBUG_TAG, "Launching in dimmed mode...");
				context.sendBroadcast(new Intent(Application
						.formatIntentAction(ObsAction.DIMM.name())));
			} else {
				Log.d(DEBUG_TAG, "Launching in standard mode...");
				context.sendBroadcast(new Intent(Application
						.formatIntentAction(ObsAction.START.name())));
			}
		} else if (intent.getAction().equals(Intent.ACTION_BATTERY_LOW)) {
			Log.d(DEBUG_TAG, "Dimming services...");
			context.sendBroadcast(new Intent(Application
					.formatIntentAction(ObsAction.DIMM.name())));
		}
	}
}
