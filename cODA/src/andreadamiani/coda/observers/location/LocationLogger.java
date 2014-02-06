package andreadamiani.coda.observers.location;

import andreadamiani.coda.Application;
import andreadamiani.coda.LogProvider;
import andreadamiani.coda.LogWriter;
import andreadamiani.coda.R;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;

public class LocationLogger extends BroadcastReceiver {

	private static final String COORDS_SEPARATOR = ";";
	public static final String NAME = "LOCATION";
	private static final String EXTRA_KEY = LocationManager.KEY_LOCATION_CHANGED;

	public LocationLogger() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Location location = (Location) intent.getExtras().get(EXTRA_KEY);
		String currentTimestamp = LogProvider.parseTimestamp(System
				.currentTimeMillis());
		String expiryTimestamp = LogProvider.parseTimestamp(System
				.currentTimeMillis()
				+ Application.getInstance().getResources()
						.getInteger(R.integer.location_expiry));
		ContentValues newValues = new ContentValues();
		newValues.put(LogProvider.TIMESTAMP, currentTimestamp);
		newValues.put(LogProvider.OBSERVER_NAME, NAME);
		newValues.put(LogProvider.LOG_VALUE,
				LocationLogger.parseToCoords(location));
		newValues.put(LogProvider.EXPIRY, expiryTimestamp);
		LogWriter.write(Application.getInstance().getContentResolver(),
				newValues);
	}

	public static String parseToCoords(Location location) {
		return Location
				.convert(location.getLatitude(), Location.FORMAT_SECONDS)
				+ COORDS_SEPARATOR
				+ Location.convert(location.getLongitude(),
						Location.FORMAT_SECONDS);
	}

	public static Location parseCoords(String coordsString) {
		Location location = new Location("coda.LOCATION_LOG");
		String[] coords = coordsString.split(COORDS_SEPARATOR);
		if (coords.length != 2) {
			throw new IllegalArgumentException();
		}
		location.setLatitude(Double.parseDouble(coords[0]));
		location.setLongitude(Double.parseDouble(coords[1]));
		return location;
	}
}
