package andreadamiani.alarmclock;

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LateActuator extends BroadcastReceiver {
	public static final String SEPARATOR = ":";

	public LateActuator() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals("andreadamiani.coda.LATE")) {
			if (intent.hasExtra("ALLARM_HOUR_EXTRA")
					&& intent.hasExtra("ALLARM_MINUTES_EXTRA")
					&& intent.hasExtra("MIN_TIME_EXTRA")) {
				int hour = intent.getIntExtra("ALLARM_HOUR_EXTRA", 0);
				int min = intent.getIntExtra("ALLARM_MINUTES_EXTRA",
						0);
				String hourS = hour < 10 ? "0" + Integer.toString(hour)
						: Integer.toString(hour);
				String minS = min < 10 ? "0" + Integer.toString(min) : Integer
						.toString(min);
				String lateAlarmTime = hourS + SEPARATOR + minS;

				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(intent.getLongExtra(
						"MIN_TIME_EXTRA", 0));
				
				hour = cal.get(Calendar.HOUR_OF_DAY);
				min = cal.get(Calendar.MINUTE);
				
				hourS = hour < 10 ? "0" + Integer.toString(hour)
						: Integer.toString(hour);
				minS = min < 10 ? "0" + Integer.toString(min) : Integer
						.toString(min);
				
				String alarmTime = hourS + SEPARATOR + minS;
				
				LateNotification.notify(context, lateAlarmTime, alarmTime);
			}
		}
	}
}