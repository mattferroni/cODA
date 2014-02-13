package andreadamiani.coda.deciders;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.AlarmClock;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class LateDeciderStarter extends BroadcastReceiver {
	public LateDeciderStarter() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.hasExtra(AlarmClock.EXTRA_HOUR) && intent.hasExtra(AlarmClock.EXTRA_MINUTES)){
			Intent i = new Intent(context, LateDecider.class);
			i.putExtra(AlarmClock.EXTRA_HOUR, intent.getIntExtra(AlarmClock.EXTRA_HOUR, 0));
			i.putExtra(AlarmClock.EXTRA_MINUTES, intent.getIntExtra(AlarmClock.EXTRA_MINUTES, 0));
			context.startService(i);
		}
	}
}
