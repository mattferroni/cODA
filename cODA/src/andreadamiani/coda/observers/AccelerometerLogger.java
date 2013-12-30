package andreadamiani.coda.observers;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import andreadamiani.coda.R;
import android.app.Service;
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

public class AccelerometerLogger extends Service implements SensorEventListener,Callback {

	/** Command to the service to reply with current log. */
    static final int MSG_REPORT = 1;
    static final int MSG_RESULT = 2;

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REPORT:
                    Message ans = Message.obtain(null, MSG_RESULT, log);
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
	
	private SensorManager sensorManager;
	private Sensor accelerometer;
	private int sensorDelay;
	private int samplingInterval;
	private List<float[]> log; 
	private Handler timer;
	private float[] linear_acceleration = {0,0,0};
	private float[] gravity = {0,0,0};
	private final Messenger bindingMessenger = new Messenger(new IncomingHandler());
	
	public AccelerometerLogger() {
	}

	
	@Override
	public void onCreate(){
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if(android.os.Build.VERSION.SDK_INT<android.os.Build.VERSION_CODES.GINGERBREAD){
			//Compatibility for API level 8
			String constantName = getResources().getString(R.string.sensor_sampling_rate);
			try {
				sensorDelay = SensorManager.class.getDeclaredField(constantName).getInt(null);
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
			sensorDelay = getResources().getInteger(R.integer.sensor_sampling_rate);
		}
		samplingInterval = getResources().getInteger(R.integer.sensor_sampling_interval);
	}
	
	@Override
	public void onDestroy(){
		log=null;
		accelerometer = null;
		sensorManager = null;
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
		log = Collections.synchronizedList(new LinkedList<float[]>());
		sensorManager.registerListener(this, accelerometer, sensorDelay);
		timer = new Handler(this);
		timer.sendEmptyMessageDelayed(0, samplingInterval);
		return 0;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return bindingMessenger.getBinder();
	}


	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Verify necessity
	}


	@Override
	public void onSensorChanged(SensorEvent event){
		  // TODO Verify filtering effectiveness
		
		  // Alpha is calculated as t / (t + dT),
		  // where t is the low-pass filter's time-constant and
		  // dT is the event delivery rate.

		  final float alpha = Float.parseFloat(getResources().getString(R.string.sensor_filter_alpha));

		  // Isolate the force of gravity with the low-pass filter.
		  gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
		  gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
		  gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

		  // Remove the gravity contribution with the high-pass filter.
		  linear_acceleration[0] = event.values[0] - gravity[0];
		  linear_acceleration[1] = event.values[1] - gravity[1];
		  linear_acceleration[2] = event.values[2] - gravity[2];
		  
		  log.add(linear_acceleration);
	}
	
	@Override
	public boolean handleMessage(Message msg) {
		sensorManager.unregisterListener(this, accelerometer);
		//TODO Permanently save log.
		return false;
	}
}
