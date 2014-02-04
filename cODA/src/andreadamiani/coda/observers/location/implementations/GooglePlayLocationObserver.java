package andreadamiani.coda.observers.location.implementations;

import andreadamiani.coda.LogProvider;
import andreadamiani.coda.R;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class GooglePlayLocationObserver extends Service implements
		LocationObserverImplementation,
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

	private class Params {
		public final int PRIORITY;
		public final long INTERVAL;
		public final long FASTEST_INTERVAL;
		public final float SMALLEST_DISPLACEMENT;

		public Params(int priority, long interval, long fastestInterval,
				float smallestDisplacement) {
			PRIORITY = priority;
			INTERVAL = interval;
			FASTEST_INTERVAL = fastestInterval;
			SMALLEST_DISPLACEMENT = smallestDisplacement;
		}
	}

	// Milliseconds per second
	private static final int MILLISECONDS_PER_SECOND = 1000;

	// private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

	private static final String NAME = "LOCATION";

	private Params PARAMS;
	private Params SLEEP_PARAMS;

	private LocationRequest mLocationRequest;

	private LocationClient mLocationClient;

	private boolean connected = false;

	private void lazyParamSet(Context context) {
		PARAMS = new Params(context.getResources().getInteger(
				R.integer.location_google_accuracy_code), context
				.getResources().getInteger(R.integer.location_google_interval)
				* MILLISECONDS_PER_SECOND, context.getResources().getInteger(
				R.integer.location_google_fastest_interval)
				* MILLISECONDS_PER_SECOND, Float.parseFloat(context
				.getResources()
				.getString(R.string.location_google_displacement)));
	}

	private void lazySleepParamSet(Context context) {
		SLEEP_PARAMS = new Params(context.getResources().getInteger(
				R.integer.location_google_sleep_accuracy_code), context
				.getResources().getInteger(
						R.integer.location_google_sleep_interval)
				* MILLISECONDS_PER_SECOND, context.getResources().getInteger(
				R.integer.location_google_sleep_fastest_interval)
				* MILLISECONDS_PER_SECOND, Float.parseFloat(context
				.getResources().getString(
						R.string.location_google_sleep_displacement)));
	}

	@Override
	public void start(Context context, Intent intent) {
		lazyParamSet(context);
		if (connected) {
			stop(context, intent);
		}
		mLocationRequest = LocationRequest.create();
		mLocationRequest.setPriority(PARAMS.PRIORITY);
		mLocationRequest.setInterval(PARAMS.INTERVAL);
		mLocationRequest.setFastestInterval(PARAMS.FASTEST_INTERVAL);
		mLocationRequest.setSmallestDisplacement(PARAMS.SMALLEST_DISPLACEMENT);
		startLocationService();
	}

	@Override
	public void dimm(Context context, Intent intent) {
		lazySleepParamSet(context);
		if (connected) {
			stop(context, intent);
		}
		mLocationRequest = LocationRequest.create();
		mLocationRequest.setPriority(SLEEP_PARAMS.PRIORITY);
		mLocationRequest.setInterval(SLEEP_PARAMS.INTERVAL);
		mLocationRequest.setFastestInterval(SLEEP_PARAMS.FASTEST_INTERVAL);
		mLocationRequest
				.setSmallestDisplacement(SLEEP_PARAMS.SMALLEST_DISPLACEMENT);
		startLocationService();
	}

	private void startLocationService() {
		/*
		 * Create a new location client, using the enclosing class to handle
		 * callbacks.
		 */
		if (connected) {
			return;
		}
		mLocationClient = new LocationClient(this, this, this);
		mLocationClient.connect();
	}

	@Override
	public void stop(Context context, Intent intent) {
		if (!connected) {
			return;
		}
		if (mLocationClient.isConnected()) {
			mLocationClient.removeLocationUpdates(this);
		}
		mLocationClient.disconnect();
		mLocationRequest = null;
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		// TODO Needs an Activity context to try to fix the issue.
		// /*
		// * Google Play services can resolve some errors it detects. If the
		// error
		// * has a resolution, try sending an Intent to start a Google Play
		// * services activity that can resolve error.
		// */
		// if (connectionResult.hasResolution()) {
		// try {
		// // Start an Activity that tries to resolve the error
		// connectionResult.startResolutionForResult(this,
		// CONNECTION_FAILURE_RESOLUTION_REQUEST);
		// /*
		// * Thrown if Google Play services canceled the original
		// * PendingIntent
		// */
		// } catch (IntentSender.SendIntentException e) {
		// // Log the error
		// e.printStackTrace();
		// }
		// }
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		if (connected || mLocationRequest == null) {
			return;
		}
		connected = true;
		mLocationClient.requestLocationUpdates(mLocationRequest, this);
	}

	@Override
	public void onDisconnected() {
		connected = false;
		mLocationClient = null;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onLocationChanged(Location location) {
		String currentTimestamp = LogProvider.parseTimestamp(SystemClock
				.currentThreadTimeMillis());
		ContentResolver cr = getContentResolver();
		ContentValues newValues = new ContentValues();
		newValues.put(LogProvider.TIMESTAMP, currentTimestamp);
		newValues.put(LogProvider.OBSERVER_NAME, NAME);
		newValues.put(LogProvider.LOG_VALUE, location.getLatitude() + ","
				+ location.getLongitude());
		cr.insert(LogProvider.CONTENT_URI, newValues);
	}

}
