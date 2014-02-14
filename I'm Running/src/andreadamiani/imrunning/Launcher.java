package andreadamiani.imrunning;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Launcher extends BroadcastReceiver {
	public Launcher() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent i = new Intent(context, Activity.class);
		i.putExtra("RUNNING_EXTRA", true);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
		context.startActivity(i);
	}
}
