package andreadamiani.coda.observers;

import java.lang.reflect.Method;
import java.util.Locale;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public abstract class Observer extends BroadcastReceiver {

	public enum ObsAction {
		START, DIMM, STOP
	}
	
	public Observer() {
		super();
	}



	protected static PendingIntent getIntent(Context context, Class<?> service) {
		Intent allarmIntent = new Intent(context, service);
		PendingIntent operation = PendingIntent.getService(context, 0,
				allarmIntent, PendingIntent.FLAG_ONE_SHOT);
		return operation;
	}


	protected abstract void start(Context context, Intent intent);



	protected abstract void dimm(Context context, Intent intent);



	protected abstract void stop(Context context, Intent intent);

	@Override
	public void onReceive(Context context, Intent intent) {
		String actionS = intent.getAction();
		String packageName = context.getPackageName();
	
		if (!actionS.startsWith(packageName)) {
			return;
		}
	
		actionS = actionS.substring(packageName.length() + 1);
		try {
			Method actionMethod = this.getClass()
					.getMethod(actionS.toLowerCase(Locale.US), Context.class,
							Intent.class);
			actionMethod.invoke(this, context, intent);
		} catch (NoSuchMethodException e) {
			// The observer is not able to accomplish this request.
			return;
		} catch (Exception e) {
			// Error Condition
			e.printStackTrace();
			return;
		}
	}

	static public void setTimer(Context context, Class<?> service, int startupDelayRes, int delayRes) {
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		PendingIntent operation = getIntent(context, service);
	
		am.cancel(operation);
		am.setInexactRepeating(
				AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis()
						+ context.getResources().getInteger(
								startupDelayRes), context
						.getResources().getInteger(delayRes), operation);
		return;
	}



	public static void cancelTimer(Context context, Class<?> service) {
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		am.cancel(getIntent(context, service));
		return;
	}
}