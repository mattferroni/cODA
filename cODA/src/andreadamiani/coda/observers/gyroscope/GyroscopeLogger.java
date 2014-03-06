package andreadamiani.coda.observers.gyroscope;

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
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.util.Log;

public class GyroscopeLogger extends Service implements
		SensorEventListener, Callback {

	private static final String VALUE_SEPARATOR = ";";

	private static final String DEBUG_TAG = "[cODA] GYROSCOPE LOGGER";

	public GyroscopeLogger() {
		super();
	}

	public static final String NAME = "GYROSCOPE";

	/** Command to the service to reply with current log. */
	static final int MSG_REPORT = 1;
	static final int MSG_RESULT = 2;

	/**
	 * Handler of incoming messages from clients.
	 */
	static class IncomingHandler extends Handler {

		private final WeakReference<GyroscopeLogger> mService;

		IncomingHandler(GyroscopeLogger service) {
			mService = new WeakReference<GyroscopeLogger>(service);
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
	private Sensor gyroscope;
	private int sensorDelay;
	private int samplingInterval;
	private List<float[]> log;
	private Handler timer;

	private Long lastRead = null;

	private int sensorSamplingRate;

	private boolean registered;

	private WakeLock wakeLock;

	@Override
	public void onCreate() {
		Log.d(DEBUG_TAG, "Creating the service...");
		registered = false;
		PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, NAME);
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		gyroscope = sensorManager
				.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.GINGERBREAD) {
			// Compatibility for API level 8
			String constantName = getResources().getString(
					R.string.gyroscope_sensor_sampling_rate);
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
					R.integer.gyroscope_sensor_sampling_rate);
		}
		sensorSamplingRate = getResources().getInteger(
				R.integer.gyroscope_sensor_sampling_rate);
		samplingInterval = getResources().getInteger(
				R.integer.gyroscope_sensor_sampling_interval);
	}

	@Override
	public void onDestroy() {				
		timer.removeCallbacksAndMessages(null);
		timer = null;
		if (registered) {
			sensorManager.unregisterListener(this, gyroscope);
			registered = false;
		}
		log = null;
		gyroscope = null;
		sensorManager = null;
		if(wakeLock != null){
			wakeLock.release();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(DEBUG_TAG, "Starting logger, it will stop in " + samplingInterval
				/ 1000 + " seconds");
		log = Collections.synchronizedList(new LinkedList<float[]>());
		if (!registered) {
			sensorManager.registerListener(this, gyroscope, sensorDelay);
			registered = true;
		}
		if(wakeLock!=null){
			wakeLock.acquire();
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

		if(log==null){
			return;
		}
		log.add(event.values);
	}

	@Override
	public boolean handleMessage(Message msg) {
		Log.d(DEBUG_TAG, "Stop signal received!");
		if (registered) {
			sensorManager.unregisterListener(this, gyroscope);
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
					GyroscopeLogger.valueToString(val));
			newValues
					.put(LogProvider.EXPIRY,
							System.currentTimeMillis()
									+ getResources().getInteger(
											R.integer.gyroscope_expiry));
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