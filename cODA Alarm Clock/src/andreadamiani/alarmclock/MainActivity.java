package andreadamiani.alarmclock;

import java.util.Calendar;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.TimePicker;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class MainActivity extends Activity implements OnClickListener{
	
	TextView messageText;
	TimePicker timePicker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		messageText = (TextView) findViewById(R.id.notificationMessage);
		timePicker = (TimePicker) findViewById(R.id.timePicker);
		onNewIntent(getIntent());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if(intent.hasExtra(LateNotification.NOTIFICATION_TEXT)){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(intent.getStringExtra(LateNotification.NOTIFICATION_TEXT));
			builder.setTitle(R.string.late_actuator_notification_title_template);
			builder.create().show();
		}
		if(intent.hasExtra(LateNotification.NOTIFICATION_ALARM_HOUR) && intent.hasExtra(LateNotification.NOTIFICATION_ALARM_MINUTES)){
			Calendar cal = Calendar.getInstance();
			timePicker.setCurrentHour(intent.getIntExtra(LateNotification.NOTIFICATION_ALARM_HOUR, cal.get(Calendar.HOUR_OF_DAY)));
			timePicker.setCurrentMinute(intent.getIntExtra(LateNotification.NOTIFICATION_ALARM_MINUTES, cal.get(Calendar.MINUTE)));
		}
	}

	@SuppressLint("InlinedApi")
	@Override
	public void onClick(View arg0) {
		int hour = timePicker.getCurrentHour();
		int min = timePicker.getCurrentMinute();
		
		Intent intent = new Intent();
		intent.putExtra(AlarmClock.EXTRA_MESSAGE, "Custom Alarm");
		if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB){
			intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
		}
		intent.putExtra(AlarmClock.EXTRA_HOUR, hour);
		intent.putExtra(AlarmClock.EXTRA_MINUTES, min);
		Intent intentForBroadcast = (Intent) intent.clone();
		
		intent.setAction(AlarmClock.ACTION_SET_ALARM);
		startActivity(intent);
		
		intentForBroadcast.setAction("andreadamiani.coda.ALARM");
		sendBroadcast(intentForBroadcast);
	}
}
