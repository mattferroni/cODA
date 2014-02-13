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

	private boolean started;
	private boolean enabled;

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
					boolean incoming = bundle.getString(
							TelephonyManager.EXTRA_STATE).equals(
							TelephonyManager.EXTRA_STATE_RINGING);
					if ((phoneNumber != null) && incoming) {
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
			registerReceiver(receiver, new IntentFilter(
					TelephonyManager.ACTION_PHONE_STATE_CHANGED));
			isRegistered = true;
		} else if (!enabled && isRegistered) {
			unregisterReceiver(receiver);
			isRegistered = false;
		}
	}

	public boolean isStarted() {
		return started;
	}

	public void setStarted(boolean started) {
		this.started = started;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}