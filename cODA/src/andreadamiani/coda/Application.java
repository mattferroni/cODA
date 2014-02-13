/**
 * 
 */
package andreadamiani.coda;

import andreadamiani.coda.observers.Observer.ObsAction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

/**
 * @author Andrea
 *
 */
public class Application extends android.app.Application {
	
	private static final String DEBUG_TAG = "cODA APPLICATION";
	
	private static Application instance;
	
	private ObsAction state = ObsAction.STOP;
	
	public static Application getInstance(){
		return instance;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(DEBUG_TAG, "The Application is starting...");
		instance = this;
		setTuningPreferences();
	}
	
	
	private void setTuningPreferences(){
		SharedPreferences prefs = this.getSharedPreferences("andreadamiani.coda", Context.MODE_PRIVATE);
		Editor editor = prefs.edit();
		
		editor.putLong("accelerometer_startup_delay", getResources().getInteger(R.integer.accelerometer_startup_delay));
		editor.putLong("accelerometer_delay", getResources().getInteger(R.integer.accelerometer_delay));
		editor.putLong("accelerometer_sleep_delay", getResources().getInteger(R.integer.accelerometer_sleep_delay));
		editor.putLong("accelerometer_sensor_sampling_rate", getResources().getInteger(R.integer.accelerometer_sensor_sampling_rate));
		editor.putString("accelerometer_sensor_sampling_rate_string", getResources().getString(R.string.accelerometer_sensor_sampling_rate));
		editor.putLong("accelerometer_sensor_sampling_interval", getResources().getInteger(R.integer.accelerometer_sensor_sampling_interval));
		editor.putFloat("accelerometer_sensor_filter_alpha", Float.parseFloat(getResources().getString(R.string.accelerometer_sensor_filter_alpha)));
		editor.putLong("accelerometer_expiry", getResources().getInteger(R.integer.accelerometer_expiry));
		
		editor.putLong("late_decider_location_relevant_period", getResources().getInteger(R.integer.late_decider_location_relevant_period));
		editor.putLong("late_decider_location_check_hour_period", getResources().getInteger(R.integer.late_decider_location_check_hour_period));
		editor.putLong("late_decider_location_valid_hour_period", getResources().getInteger(R.integer.late_decider_location_valid_hour_period));
		editor.putLong("late_decider_location_relevant_distance", getResources().getInteger(R.integer.late_decider_location_relevant_distance));
		
		editor.putLong("location_startup_delay", getResources().getInteger(R.integer.location_startup_delay));
		editor.putLong("location_time_delay", getResources().getInteger(R.integer.location_time_delay));
		editor.putLong("location_sleep_time_delay", getResources().getInteger(R.integer.location_sleep_time_delay));
		editor.putLong("location_space_delay", getResources().getInteger(R.integer.location_space_delay));
		editor.putLong("location_sleep_space_delay", getResources().getInteger(R.integer.location_sleep_space_delay));
		editor.putLong("location_expiry", getResources().getInteger(R.integer.location_expiry));
		
		editor.putLong("cleenup_cycle", getResources().getInteger(R.integer.cleenup_cycle));
		
		editor.putLong("running_relevant_period", getResources().getInteger(R.integer.running_relevant_period));
		editor.putFloat("running_running_direction_acc_treshold", Float.parseFloat(getResources().getString(R.string.running_running_direction_acc_treshold)));
		editor.putFloat("running_running_direction_var_treshold", Float.parseFloat(getResources().getString(R.string.running_running_direction_var_treshold)));
		editor.putFloat("running_vertical_direction_acc_treshold", Float.parseFloat(getResources().getString(R.string.running_vertical_direction_acc_treshold)));
		editor.putFloat("running_vertical_direction_var_treshold", Float.parseFloat(getResources().getString(R.string.running_vertical_direction_var_treshold)));
		editor.putFloat("running_lateral_direction_acc_treshold", Float.parseFloat(getResources().getString(R.string.running_lateral_direction_acc_treshold)));
		editor.putFloat("running_lateral_direction_var_treshold", Float.parseFloat(getResources().getString(R.string.running_lateral_direction_var_treshold)));
		
		editor.commit();
	}
	
	private static void checkInstance(){
		if(instance == null) {
			throw new IllegalStateException("The application is not running!");
		}
	}
	
	public static String formatIntentAction(String action){
		checkInstance();
		return instance.getPackageName() + "." + action;
	}
	
	public static boolean isInternalIntent(Intent intent){
		checkInstance();
		return intent.getAction().startsWith(instance.getPackageName());
	}
	
	public static String getInternalAction(Intent intent){
		try {
			if(isInternalIntent(intent)){
				return intent.getAction().substring(instance.getPackageName().length()+1);
			} else {
				return intent.getAction();
			}
		} catch (IllegalStateException e) {
			return intent.getAction();
		}
	}

	public ObsAction getState() {
		return state;
	}
	
	public void setState(ObsAction state){
		this.state = state;
	}
}