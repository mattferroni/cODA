package andreadamiani.coda.actuators.late;

import java.util.Calendar;

import andreadamiani.coda.Application;
import andreadamiani.coda.deciders.LateDecider;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LateActuator extends BroadcastReceiver {
	public LateActuator() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String actionS = Application.getInternalAction(intent);
		if (actionS.equals(LateDecider.NAME)) {
			if (intent.hasExtra(LateDecider.ALLARM_HOUR_EXTRA)
					&& intent.hasExtra(LateDecider.ALLARM_MINUTES_EXTRA)
					&& intent.hasExtra(LateDecider.MIN_TIME_EXTRA)) {
				int hour = intent.getIntExtra(LateDecider.ALLARM_HOUR_EXTRA, 0);
				int min = intent.getIntExtra(LateDecider.ALLARM_MINUTES_EXTRA,
						0);
				String hourS = hour < 10 ? "0" + Integer.toString(hour)
						: Integer.toString(hour);
				String minS = min < 10 ? "0" + Integer.toString(min) : Integer
						.toString(min);
				String lateAlarmTime = hourS + ":" + minS;

				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(intent.getLongExtra(
						LateDecider.MIN_TIME_EXTRA, 0));
				
				hour = cal.get(Calendar.HOUR_OF_DAY);
				min = cal.get(Calendar.MINUTE);
				
				hourS = hour < 10 ? "0" + Integer.toString(hour)
						: Integer.toString(hour);
				minS = min < 10 ? "0" + Integer.toString(min) : Integer
						.toString(min);
				
				String alarmTime = hourS + ":" + minS;
				
				LateNotification.notify(context, lateAlarmTime, alarmTime);
			}
		}
	}
}