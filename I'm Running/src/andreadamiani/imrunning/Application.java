/**
 * 
 */
package andreadamiani.imrunning;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;

/**
 * @author Andrea
 * 
 */
public class Application extends android.app.Application {

	private static Application instance;

	private boolean isRegistered = false;
	private BroadcastReceiver receiver;

	public static Application getInstance() {
		return instance;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
		receiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				try {
					Bundle bundle = intent.getExtras();
					String phoneNumber = bundle
							.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
					if ((phoneNumber != null)) {
						SmsManager smsManager = SmsManager.getDefault();

						String myMessage = getString(R.string.auto_answer);
						smsManager.sendTextMessage(phoneNumber, null,
								myMessage, null, null);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		};
	}

	public void registerReceiver(boolean enabled) {
		if (enabled && !isRegistered) {
			registerReceiver(receiver, new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
		} else if (!enabled && isRegistered) {
			unregisterReceiver(receiver);
		}
	}
}