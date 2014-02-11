package andreadamiani.coda.tools;

import andreadamiani.coda.Application;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class Timer {
	static public void set(Context context, Intent intent,
			int startupDelayRes, int delayRes) {
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		PendingIntent pIntent = PendingIntent.getBroadcast(
				Application.getInstance(), 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		am.cancel(pIntent);
		am.setInexactRepeating(
				AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis()
						+ context.getResources().getInteger(startupDelayRes),
				context.getResources().getInteger(delayRes), pIntent);
		return;
	}

	public static void cancel(Context context, Intent intent) {
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		PendingIntent pIntent = PendingIntent.getBroadcast(
				Application.getInstance(), 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		am.cancel(pIntent);
		return;
	}
}
