package andreadamiani.coda;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class Launcher extends BroadcastReceiver {
	public Launcher() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) || intent.getAction().equals(Intent.ACTION_BATTERY_OKAY)){
			Intent battery = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
			if(battery.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) <= 20){
				context.sendBroadcast(new Intent(context.getPackageName()+".DIMM"));
			} else {
				context.sendBroadcast(new Intent(context.getPackageName()+".START"));
			}
		} else if (intent.getAction().equals(Intent.ACTION_BATTERY_LOW)){
			context.sendBroadcast(new Intent(context.getPackageName()+".DIMM"));
		}
	}
}
