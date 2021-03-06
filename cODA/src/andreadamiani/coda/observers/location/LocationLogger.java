package andreadamiani.coda.observers.location;

import andreadamiani.coda.Application;
import andreadamiani.coda.LogProvider;
import andreadamiani.coda.R;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

public class LocationLogger extends BroadcastReceiver {

	private static final String DEBUG_TAG = "[cODA] LOCATION LOGGER";

	private static final String VALUE_SEPARATOR = ";";
	public static final String NAME = "LOCATION";
	private static final String EXTRA_KEY = LocationManager.KEY_LOCATION_CHANGED;

	public LocationLogger() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(DEBUG_TAG, "Receiving location update");
		Location location = (Location) intent.getExtras().get(EXTRA_KEY);
		if(location == null){
			Log.d(DEBUG_TAG, "Aborting - no location dispatched.");
			return;
		}
		ContentValues newValues = new ContentValues();
		newValues.put(LogProvider.TIMESTAMP, System.currentTimeMillis());
		newValues.put(LogProvider.OBSERVER_NAME, NAME);
		newValues.put(LogProvider.LOG_VALUE,
				LocationLogger.valueToString(location));
		newValues.put(
				LogProvider.EXPIRY,
				System.currentTimeMillis()
						+ Application.getInstance().getResources()
								.getInteger(R.integer.location_expiry));
		Application.getInstance().getContentResolver()
				.insert(LogProvider.CONTENT_URI, newValues);
	}

	public static String valueToString(Location location) {
		return commaSanitizer(Location
				.convert(location.getLatitude(), Location.FORMAT_SECONDS))
				+ VALUE_SEPARATOR
				+ commaSanitizer(Location.convert(location.getLongitude(),
						Location.FORMAT_SECONDS));
	}
	
	public static String commaSanitizer(String string){
		return string.replace(",",".");
	}

	public static Location parseValue(String coordsString) {
		Location location = new Location("coda.LOCATION_LOG");
		String[] coords = coordsString.split(VALUE_SEPARATOR);
		if (coords.length != 2) {
			throw new IllegalArgumentException();
		}
		location.setLatitude(Double.parseDouble(coords[0]));
		location.setLongitude(Double.parseDouble(coords[1]));
		return location;
	}
	
	public static Location parseValueDirect(String coordsString) {
		Location location = new Location("coda.LOCATION_LOG");
		String[] coords = coordsString.split(VALUE_SEPARATOR);
		if (coords.length != 2) {
			throw new IllegalArgumentException();
		}
		location.setLatitude(Location.convert(coords[0]));
		location.setLongitude(Location.convert(coords[1]));
		return location;
	}
}