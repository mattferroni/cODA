package andreadamiani.coda.observers.accelerometer;


import andreadamiani.coda.R;
import andreadamiani.coda.observers.Observer;
import android.content.Context;
import android.content.Intent;

public class AccelerometerObserver extends Observer {
	
	
	public AccelerometerObserver() {
		setService(AccelerometerLogger.class);
	}

	@Override
	protected void start(Context context, Intent intent) {
		setTimer(context, getService(), R.integer.accelerometer_startup_delay, R.integer.accelerometer_delay);
	}

	@Override
	protected void dimm(Context context, Intent intent) {
		setTimer(context, getService(), R.integer.accelerometer_startup_delay, R.integer.accelerometer_sleep_delay);
	}

	@Override
	protected void stop(Context context, Intent intent) {
		cancelTimer(context, getService());
	}
}
