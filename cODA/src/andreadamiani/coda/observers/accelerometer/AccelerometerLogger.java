package andreadamiani.coda.observers.accelerometer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.ParseException;

import andreadamiani.coda.LogProvider;
import andreadamiani.coda.R;
import andreadamiani.coda.tools.LogWriter;
import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class AccelerometerLogger extends Service implements
		SensorEventListener, Callback {

	private static final String VALUE_SEPARATOR = ";";

	private static final String DEBUG_TAG = "[cODA] ACCELEROMETER LOGGER";

	public AccelerometerLogger() {
		super();
	}

	public static final String NAME = "ACCELEROMETER";

	/** Command to the service to reply with current log. */
	static final int MSG_REPORT = 1;
	static final int MSG_RESULT = 2;

	/**
	 * Handler of incoming messages from clients.
	 */
	static class IncomingHandler extends Handler {

		private final WeakReference<AccelerometerLogger> mService;

		IncomingHandler(AccelerometerLogger service) {
			mService = new WeakReference<AccelerometerLogger>(service);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_REPORT:
				Message ans = Message.obtain(null, MSG_RESULT,
						mService.get().log);
				try {
					msg.replyTo.send(ans);
				} catch (RemoteException e) {
				}
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	private final IncomingHandler mHandler = new IncomingHandler(this);
	private final Messenger bindingMessenger = new Messenger(mHandler);

	private SensorManager sensorManager;
	private Sensor accelerometer;
	private int sensorDelay;
	private int samplingInterval;
	private List<float[]> log;
	private Handler timer;

	private float[] gravity = { 0, 0, 0 };
	private Long lastRead = null;

	private int sensorSamplingRate;

	private boolean registered;

	@Override
	public void onCreate() {
		Log.d(DEBUG_TAG, "Creating the service...");
		registered = false;
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.GINGERBREAD) {
			// Compatibility for API level 8
			String constantName = getResources().getString(
					R.string.accelerometer_sensor_sampling_rate);
			try {
				sensorDelay = SensorManager.class
						.getDeclaredField(constantName).getInt(null);
			} catch (IllegalArgumentException e) {
				sensorDelay = SensorManager.SENSOR_DELAY_FASTEST;
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				sensorDelay = SensorManager.SENSOR_DELAY_FASTEST;
				e.printStackTrace();
			} catch (NoSuchFieldException e) {
				sensorDelay = SensorManager.SENSOR_DELAY_FASTEST;
				e.printStackTrace();
			}
		} else {
			sensorDelay = getResources().getInteger(
					R.integer.accelerometer_sensor_sampling_rate);
		}
		sensorSamplingRate = getResources().getInteger(
				R.integer.accelerometer_sensor_sampling_rate);
		samplingInterval = getResources().getInteger(
				R.integer.accelerometer_sensor_sampling_interval);
	}

	@Override
	public void onDestroy() {
		timer.removeCallbacksAndMessages(null);
		timer = null;
		if (registered) {
			sensorManager.unregisterListener(this, accelerometer);
			registered = false;
		}
		log = null;
		accelerometer = null;
		sensorManager = null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(DEBUG_TAG, "Starting logger, it will stop in " + samplingInterval
				/ 1000 + " seconds");
		log = Collections.synchronizedList(new LinkedList<float[]>());
		if (!registered) {
			sensorManager.registerListener(this, accelerometer, sensorDelay);
			registered = true;
		}
		timer = new Handler(this);
		timer.sendEmptyMessageDelayed(0, samplingInterval);
		return Service.START_NOT_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return bindingMessenger.getBinder();
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		long currentTime = System.currentTimeMillis();

		if (lastRead != null && lastRead + sensorSamplingRate > currentTime) {
			return;
		}

		this.lastRead = currentTime;

		// Alpha is calculated as t / (t + dT),
		// where t is the low-pass filter's time-constant and
		// dT is the event delivery rate.

		final float alpha = Float.parseFloat(getResources().getString(
				R.string.accelerometer_sensor_filter_alpha));

		if(gravity==null){
			return;
		}
		// Isolate the force of gravity with the low-pass filter.
		gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
		gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
		gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

		// Remove the gravity contribution with the high-pass filter.
		float[] linear_acceleration = new float[3];
		linear_acceleration[0] = event.values[0] - gravity[0];
		linear_acceleration[1] = event.values[1] - gravity[1];
		linear_acceleration[2] = event.values[2] - gravity[2];

		Log.d(DEBUG_TAG,
				"Registering "
						+ AccelerometerLogger
								.valueToString(linear_acceleration));
		if(log==null){
			return;
		}
		log.add(linear_acceleration);
	}

	@Override
	public boolean handleMessage(Message msg) {
		Log.d(DEBUG_TAG, "Stop signal received!");
		if (registered) {
			sensorManager.unregisterListener(this, accelerometer);
			registered = false;
		}
		lastRead = null;
		ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
		for (int i = 0; i < log.size(); i++) {
			long timestamp = System.currentTimeMillis() - sensorSamplingRate
					* (log.size() - (i + 1));
			float[] val = log.get(i);
			ContentValues newValues = new ContentValues();
			newValues.put(LogProvider.TIMESTAMP, timestamp);
			newValues.put(LogProvider.OBSERVER_NAME, NAME);
			newValues.put(LogProvider.LOG_VALUE,
					AccelerometerLogger.valueToString(val));
			newValues
					.put(LogProvider.EXPIRY,
							System.currentTimeMillis()
									+ getResources().getInteger(
											R.integer.accelerometer_expiry));
			Log.d(DEBUG_TAG, "Preparing insert for: " + val[0] + ";" + val[1]
					+ ";" + val[2] + "(time: " + timestamp + ")");
			batch.add(ContentProviderOperation
					.newInsert(LogProvider.CONTENT_URI).withValues(newValues)
					.withYieldAllowed(true).build());
		}
		LogWriter.write(batch);
		Log.d(DEBUG_TAG, "Stopping service...");
		this.stopSelf();
		return true;
	}

	public static String valueToString(float[] value) {
		StringBuilder string = new StringBuilder(Float.toString(value[0]));
		for (int i = 1; i < value.length; i++) {
			string.append(VALUE_SEPARATOR + Float.toString(value[i]));
		}
		return string.toString();
	}

	public static float[] parseValue(String value) {
		String[] values = value.split(VALUE_SEPARATOR);
		if (values.length != 3) {
			throw new ParseException();
		}
		float[] ret = new float[3];
		for (int i = 0; i < values.length; i++) {
			ret[i] = Float.parseFloat(values[i]);
		}
		return ret;
	}
}