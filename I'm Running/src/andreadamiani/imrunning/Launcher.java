package andreadamiani.imrunning;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Launcher extends BroadcastReceiver {
	public Launcher() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if(Application.getInstance() == null || !Application.getInstance().isStarted()){
			Intent i = new Intent(context, Activity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.putExtra("RUNNING_EXTRA", true);
			context.startActivity(i);
		}
	}
}
