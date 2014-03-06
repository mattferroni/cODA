package andreadamiani.coda.observers.gyroscope;

import andreadamiani.coda.Application;
import andreadamiani.coda.R;
import andreadamiani.coda.observers.Observer;
import andreadamiani.coda.tools.Timer;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class GyroscopeObserver extends Observer {

	private static final String DEBUG_TAG = "[cODA] GYROSCOPE OBSERVER";

	private final static Class<?> service = GyroscopeLogger.class;
	public static final String START_GYROSCOPE_LOGGER = "START_GYROSCOPE_LOGGER";
	public static final String STOP_GYROSCOPE_LOGGER = "STOP_GYROSCOPE_LOGGER";

	public GyroscopeObserver() {
		super();
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		Log.d(DEBUG_TAG, "Reacting to Intent ...");
		if (Application.isInternalIntent(intent)) {
			String actionS = Application.getInternalAction(intent);
			if (actionS.equals(START_GYROSCOPE_LOGGER)) {
				Application.getInstance().startService(
						new Intent(Application.getInstance(), service));
			} else if (actionS.equals(STOP_GYROSCOPE_LOGGER)) {
				Application.getInstance().stopService(
						new Intent(Application.getInstance(), service));
			}
		}
	}

	@Override
	protected void start(Context context, Intent intent) {
		Log.d(DEBUG_TAG, "Scheduling service starts (standard mode)...");
		Timer.set(
				context,
				new Intent(Application
						.formatIntentAction(START_GYROSCOPE_LOGGER)),
				R.integer.gyroscope_startup_delay,
				R.integer.gyroscope_delay);
		super.start(context, intent);
	}

	@Override
	protected void dimm(Context context, Intent intent) {
		Log.d(DEBUG_TAG, "Scheduling service starts (dimmed mode)...");
		Timer.set(
				context,
				new Intent(Application
						.formatIntentAction(START_GYROSCOPE_LOGGER)),
				R.integer.gyroscope_startup_delay,
				R.integer.gyroscope_sleep_delay);
		super.dimm(context, intent);
	}

	@Override
	protected void stop(Context context, Intent intent) {
		Log.d(DEBUG_TAG, "Unscheduling service starts...");
		Application.getInstance().sendBroadcast(
				new Intent(Application
						.formatIntentAction(STOP_GYROSCOPE_LOGGER)));
		Timer.cancel(
				context,
				new Intent(Application
						.formatIntentAction(START_GYROSCOPE_LOGGER)));
		super.stop(context, intent);
	}
}
