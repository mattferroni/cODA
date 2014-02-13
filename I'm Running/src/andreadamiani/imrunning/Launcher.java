package andreadamiani.imrunning;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Launcher extends BroadcastReceiver {
	public Launcher() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if(Application.getInstance() == null || !Application.getInstance().isAppStarted()){
			Intent i = new Intent(context, Activity.class);
			i.putExtra("RUNNING_EXTRA", true);
			context.startActivity(i);
		}
	}
}
