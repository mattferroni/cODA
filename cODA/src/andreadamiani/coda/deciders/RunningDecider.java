package andreadamiani.coda.deciders;

import java.util.ArrayList;
import java.util.List;

import andreadamiani.coda.Application;
import andreadamiani.coda.LogProvider;
import andreadamiani.coda.R;
import andreadamiani.coda.observers.accelerometer.AccelerometerLogger;
import andreadamiani.coda.tools.Timer;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

public class RunningDecider extends BroadcastReceiver {
	public static final String ACTIVATE_ACTION = "RUNNING_START";
	private float RUNNING_DIRECTION_ACC_TRESHOLD = 0;
	private float RUNNING_DIRECTION_VAR_TRESHOLD = 0;
	private float VERTICAL_DIRECTION_ACC_TRESHOLD = 0;
	private float VERTICAL_DIRECTION_VAR_TRESHOLD = 0;
	private float LATERAL_DIRECTION_ACC_TRESHOLD = 0;
	private float LATERAL_DIRECTION_VAR_TRESHOLD = 0;

	public RunningDecider() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String actionS = Application.getInternalAction(intent);
		if (actionS.equals("START")) {
			Timer.set(
					Application.getInstance().getApplicationContext(),
					new Intent(Application.formatIntentAction(ACTIVATE_ACTION)),
					R.integer.accelerometer_delay,
					R.integer.accelerometer_delay);
		} else if (actionS.equals("DIMM")) {
			Timer.set(
					Application.getInstance().getApplicationContext(),
					new Intent(Application.formatIntentAction(ACTIVATE_ACTION)),
					R.integer.accelerometer_sleep_delay,
					R.integer.accelerometer_sleep_delay);
		} else if (actionS.equals("STOP")) {
			Timer.cancel(Application.getInstance().getApplicationContext(),
					new Intent(Application.formatIntentAction(ACTIVATE_ACTION)));
		} else if (actionS.equals(ACTIVATE_ACTION)) {
			if (isRunning()) {
				Application.getInstance().sendBroadcast(
						new Intent(Application.formatIntentAction("RUNNING")));
			}
		}
	}

	private boolean isRunning() {
		setParameters(Application.getInstance().getApplicationContext());

		String[] projection = { LogProvider.LOG_VALUE };
		String selection = "(" + LogProvider.TIMESTAMP + " > ?)";

		String[] selectionArgs = { Long.toString(System.currentTimeMillis()
				- Application.getInstance().getResources()
						.getInteger(R.integer.running_relevant_period)) };

		String sortOrder = LogProvider.TIMESTAMP + " ASC";

		Cursor cur = Application
				.getInstance()
				.getContentResolver()
				.query(Uri.parse(LogProvider.CONTENT_URI + "/"
						+ AccelerometerLogger.NAME), projection, selection,
						selectionArgs, sortOrder);

		float[] integral_acceleration = { 0F, 0F, 0F };
		float[] integral_abs_variation = { 0F, 0F, 0F };

		List<float[]> entries = new ArrayList<float[]>();
		while (cur.moveToNext()) {
			float[] entry = AccelerometerLogger.parseValue(cur.getString(cur
					.getColumnIndex(LogProvider.LOG_VALUE)));
			entries.add(entry);
		}

		float[] prevEntry = null;
		for (int i = 0; i < entries.size(); i++) {
			float[] entry = entries.get(i);
			for (int j = 0; j < 3; j++) {
				integral_acceleration[j] += entry[j];
				if (prevEntry != null) {
					integral_abs_variation[j] += Math.abs(entry[j]
							- prevEntry[j]);
				}
			}
		}

		boolean isRunning = false;
		for (int i = 0; i < 3; i++) {
			int[] others = { i - 1 < 0 ? 3 : i - 1, i + 1 > 2 ? 0 : i + 2 };
			if (integral_acceleration[i] > RUNNING_DIRECTION_ACC_TRESHOLD
					&& integral_abs_variation[i] < RUNNING_DIRECTION_VAR_TRESHOLD) {
				boolean verticalDirectionFound = false;
				boolean lateralDirectionFound = false;
				for (int j = 0; j < 2; j++) {
					if (integral_acceleration[others[j]] < VERTICAL_DIRECTION_ACC_TRESHOLD
							&& integral_abs_variation[others[j]] > VERTICAL_DIRECTION_VAR_TRESHOLD
							&& !verticalDirectionFound) {
						verticalDirectionFound = true;
					} else if (integral_acceleration[others[j]] < LATERAL_DIRECTION_ACC_TRESHOLD
							&& integral_abs_variation[others[j]] < LATERAL_DIRECTION_VAR_TRESHOLD
							&& !lateralDirectionFound) {
						lateralDirectionFound = true;
					}
				}
				if (verticalDirectionFound && lateralDirectionFound) {
					isRunning = true;
					break;
				}
			}
		}

		return isRunning;
	}

	private void setParameters(Context context) {
		RUNNING_DIRECTION_ACC_TRESHOLD = Float.parseFloat(context
				.getResources().getString(
						R.string.running_running_direction_acc_treshold));
		RUNNING_DIRECTION_VAR_TRESHOLD = Float.parseFloat(context
				.getResources().getString(
						R.string.running_running_direction_var_treshold));
		VERTICAL_DIRECTION_ACC_TRESHOLD = Float.parseFloat(context
				.getResources().getString(
						R.string.running_vertical_direction_acc_treshold));
		VERTICAL_DIRECTION_VAR_TRESHOLD = Float.parseFloat(context
				.getResources().getString(
						R.string.running_vertical_direction_var_treshold));
		LATERAL_DIRECTION_ACC_TRESHOLD = Float.parseFloat(context
				.getResources().getString(
						R.string.running_lateral_direction_acc_treshold));
		LATERAL_DIRECTION_VAR_TRESHOLD = Float.parseFloat(context
				.getResources().getString(
						R.string.running_lateral_direction_var_treshold));
	}
}