package andreadamiani.coda.observers;

import java.lang.reflect.Method;
import java.util.Locale;

import andreadamiani.coda.Application;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public abstract class Observer extends BroadcastReceiver {

	private static final String DEBUG_TAG = "[cODA] OBSERVER";
	public static final String PERMISSION = "OBSERVER_ACTION";
	
	public enum ObsAction {
		START(2), DIMM(1), STOP(0);
		
		public final int id;
		
		private ObsAction(int id){
			this.id = id;
		}
		
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

	protected void start(Context context, Intent intent){
		Application.getInstance().setState(ObsAction.START);
	}

	protected void dimm(Context context, Intent intent){
		Application.getInstance().setState(ObsAction.DIMM);
	}

	protected void stop(Context context, Intent intent){
		Application.getInstance().setState(ObsAction.STOP);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (!Application.isInternalIntent(intent)) {
			return;
		}

		Log.d(DEBUG_TAG, "Reacting to Intent ...");
		String actionS = Application.getInternalAction(intent);
		try {
			Method actionMethod = this.getClass()
					.getDeclaredMethod(actionS.toLowerCase(Locale.US),
							Context.class, Intent.class);
			actionMethod.setAccessible(true);
			actionMethod.invoke(this, context, intent);
		} catch (NoSuchMethodException e) {
			Log.d(DEBUG_TAG, "The generic observer is not handling the intent");
			return;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	static public void setTimer(Context context, Intent intent,
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

	public static void cancelTimer(Context context, Intent intent) {
		AlarmManager am = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		PendingIntent pIntent = PendingIntent.getBroadcast(
				Application.getInstance(), 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		am.cancel(pIntent);
		return;
	}
}