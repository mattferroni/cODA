package andreadamiani.coda.observers.location;

import andreadamiani.coda.Application;
import andreadamiani.coda.R;
import andreadamiani.coda.observers.Observer;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;

public class LocationObserver extends Observer {

	private static final String DEBUG_TAG = "[cODA] LOCATION OBSERVER";

	private final static Class<?> service = LocationLogger.class;
	private static final int LOCATION_UPDATE_REQUEST_CODE = 0;

	private static final Criteria baseCriteria = new Criteria();
	static {
		// XXX Create a preference on this.
		baseCriteria.setCostAllowed(false);
		baseCriteria.setSpeedRequired(false);
	}

	private static final PendingIntent locationIntent;
	static {
		locationIntent = PendingIntent.getBroadcast(Application.getInstance()
				.getApplicationContext(), LOCATION_UPDATE_REQUEST_CODE,
				new Intent(Application.getInstance().getApplicationContext(),
						service), PendingIntent.FLAG_UPDATE_CURRENT);

	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	// Uses converters for compatibility.
	private Criteria generateCriteria(boolean sleepMode) {
		Criteria criteria = new Criteria(baseCriteria);
		if (!sleepMode) {
			criteria.setAccuracy(accuracyCodeConverter(Criteria.ACCURACY_MEDIUM));
			criteria.setPowerRequirement(Criteria.POWER_MEDIUM);
		} else {
			criteria.setAccuracy(accuracyCodeConverter(Criteria.ACCURACY_LOW));
			criteria.setPowerRequirement(Criteria.POWER_LOW);
		}

		return criteria;
	}

	private int accuracyCodeConverter(int accuracyCode) {
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.GINGERBREAD) {
			return Criteria.ACCURACY_COARSE;
		} else {
			return accuracyCode;
		}
	}

	@Override
	protected void start(Context context, Intent intent) {
		Log.d(DEBUG_TAG, "In standard mode...");
		startLocationObserver(context, intent, false);
	}

	@Override
	protected void dimm(Context context, Intent intent) {
		Log.d(DEBUG_TAG, "In dimmed mode...");
		startLocationObserver(context, intent, true);
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	// Internally handles the compatibility issues
	private void startLocationObserver(Context context, Intent intent,
			boolean sleepMode) {
		stop(context, intent);

		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.GINGERBREAD) {
			// XXX Compatibility from API 8 to API 10
			// No PendingIntent for Location Update
		} else {
			Log.d(DEBUG_TAG, "Scheduling service starts...");
			LocationManager locationManager = (LocationManager) context
					.getSystemService(Context.LOCATION_SERVICE);
			Criteria criteria = generateCriteria(sleepMode);
			locationManager.getProviders(criteria, true);
			if (locationManager.getProviders(criteria, true).isEmpty()) {
				Log.d(DEBUG_TAG, "Aborting - no location device found.");
				return;
			}
			locationManager.requestLocationUpdates(context.getResources()
					.getInteger(R.integer.location_time_delay), context
					.getResources().getInteger(R.integer.location_space_delay),
					criteria, locationIntent);
		}
	}

	@Override
	protected void stop(Context context, Intent intent) {
		Log.d(DEBUG_TAG, "Unscheduling service starts...");
		LocationManager locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		locationManager.removeUpdates(locationIntent);
	}
}
