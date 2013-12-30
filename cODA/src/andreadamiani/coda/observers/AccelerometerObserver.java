package andreadamiani.coda.observers;

import java.lang.reflect.Method;
import java.util.Locale;

import andreadamiani.coda.R;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AccelerometerObserver extends BroadcastReceiver {
	public AccelerometerObserver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String actionS = intent.getAction();
		String packageName = context.getPackageName();
		
		if(!actionS.startsWith(packageName)){
			return;
		}
		
		actionS = actionS.substring(packageName.length()+1);
		try {
			Method actionMethod = this.getClass().getMethod(actionS.toLowerCase(Locale.US),Context.class,Intent.class);
			actionMethod.invoke(this,context,intent);
		} catch (NoSuchMethodException e) {
			// The observer is not able to accomplish this request.
			return;
		} catch (Exception e) {
			// Error Condition
			e.printStackTrace();
			return;
		}
	}
	
	@SuppressWarnings("unused")
	private void start(Context context, Intent intent){
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);		
		PendingIntent operation = getIntent(context, intent);
		
		am.cancel(operation);
		am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+context.getResources().getInteger(R.integer.startup_delay), context.getResources().getInteger(R.integer.delay), operation);
		return;
	}
	
	@SuppressWarnings("unused")
	private void dimm(Context context, Intent intent){
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent operation = getIntent(context, intent);
		
		am.cancel(operation);
		am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+context.getResources().getInteger(R.integer.startup_delay), context.getResources().getInteger(R.integer.sleep_delay), operation);
		return;
	}
	
	@SuppressWarnings("unused")
	private void stop(Context context, Intent intent){
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.cancel(getIntent(context, intent));
		return;
	}
	
	private PendingIntent getIntent(Context context, Intent intent){
		Intent allarmIntent = new Intent(context,AccelerometerLogger.class);
		PendingIntent operation = PendingIntent.getService(context,0,allarmIntent,PendingIntent.FLAG_ONE_SHOT);
		return operation;
	}
}
