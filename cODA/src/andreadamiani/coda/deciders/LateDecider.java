package andreadamiani.coda.deciders;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Calendar;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONException;
import org.json.JSONObject;

import andreadamiani.coda.Application;
import andreadamiani.coda.LogProvider;
import andreadamiani.coda.R;
import andreadamiani.coda.observers.location.LocationLogger;
import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.util.SparseArray;

public class LateDecider extends IntentService {

	public static final String ALLARM_MINUTES_EXTRA = "ALLARM_MINUTES_EXTRA";
	public static final String ALLARM_HOUR_EXTRA = "ALLARM_HOUR_EXTRA";
	public static final String MIN_TIME_EXTRA = "MIN_TIME";
	public static final String DELAY_EXTRA = "DELAY";
	public static final String NAME = "LATE";

	public LateDecider() {
		super("LateDecider");
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent.hasExtra(AlarmClock.EXTRA_HOUR)
				&& intent.hasExtra(AlarmClock.EXTRA_MINUTES)) {
			if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				return;
			}
			int hour = intent.getIntExtra(AlarmClock.EXTRA_HOUR, 0);
			int min = intent.getIntExtra(AlarmClock.EXTRA_MINUTES, 0);

			long[] interval = getInterval(hour, min);

			Cursor cur = null;
			ContentResolver cr = getContentResolver();
			Uri uri = CalendarContract.Events.CONTENT_URI;

			String[] projection = { CalendarContract.Events.DTSTART,
					CalendarContract.Events.EVENT_LOCATION };

			String selection = "((" + CalendarContract.Events.ALL_DAY
					+ " = ?) AND (" + CalendarContract.Events.AVAILABILITY
					+ " = ?) AND (" + CalendarContract.Events.EVENT_LOCATION
					+ " is not null) AND (" + CalendarContract.Events.DTSTART
					+ " = ?) AND (" + CalendarContract.Events.DTEND + " = ?)";

			String[] selectionArgs = {
					Integer.toString(0),
					Integer.toString(CalendarContract.Events.AVAILABILITY_BUSY),
					Long.toString(interval[0]), Long.toString(interval[1]) };
			String sortOrder = CalendarContract.Events.DTSTART + " ASC";

			cur = cr.query(uri, projection, selection, selectionArgs, sortOrder);
			if (cur.getCount() > 0) {
				cur.moveToFirst();
				String destination = cur
						.getString(cur
								.getColumnIndex(CalendarContract.Events.EVENT_LOCATION));
				long startTime = cur.getLong(cur
						.getColumnIndex(CalendarContract.Events.DTSTART));
				LocationInfo startInfo = computeStartingPoint(startTime);
				if (startInfo == null
						|| startInfo.getCount() < startInfo.getConflicts()) {
					return;
				}
				String startPoint = locationToString(startInfo.getLocation());

				try {
					int duration = calulateTripDuration(startPoint, destination);
					long delay = interval[0] + (duration * 1000) - startTime;
					if (delay > 0) {
						Intent i = new Intent(
								Application.formatIntentAction(NAME));
						i.putExtra(DELAY_EXTRA, delay);
						i.putExtra(MIN_TIME_EXTRA, interval[0] - delay);
						i.putExtra(ALLARM_HOUR_EXTRA, hour);
						i.putExtra(ALLARM_MINUTES_EXTRA, min);
						sendBroadcast(i);
					}
				} catch (LookupFailed e) {
					return;
				}
			}
		}
	}

	private int calulateTripDuration(String start, String end)
			throws LookupFailed {
		URL url;
		try {
			url = new URL(
					"https://maps.googleapis.com/maps/api/directions/xml?"
							+ "json=" + start + "&destination=" + end
							+ "&sensor=true" + "&alternatives=false");
		} catch (MalformedURLException e) {
			throw new RuntimeException();
		}
		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
		if (networkInfo == null || !networkInfo.isConnected()) {
			throw new LookupFailed();
		}
		try {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000 /* milliseconds */);
			conn.setConnectTimeout(15000 /* milliseconds */);
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			conn.connect();
			int response = conn.getResponseCode();
			if (response != HttpURLConnection.HTTP_ACCEPTED) {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(conn.getInputStream(), "UTF-8"));
				StringBuilder string = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					string.append(line + "\n");
				}

				JSONObject object = new JSONObject(string.toString());
				if (!object.getString("status").equalsIgnoreCase("OK")) {
					throw new ConnectTimeoutException();
				}
				return object.getJSONArray("routes").getJSONObject(0)
						.getJSONObject("duration").getInt("value");
			}
		} catch (ProtocolException e) {
			throw new LookupFailed();
		} catch (UnsupportedEncodingException e) {
			throw new LookupFailed();
		} catch (IOException e) {
			throw new LookupFailed();
		} catch (JSONException e) {
			throw new LookupFailed();
		}
		return 0;
	}

	private class LookupFailed extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = -692482335552369522L;

	}

	private static String locationToString(Location location) {
		return location.getLatitude() + "," + location.getLongitude();
	}

	private class LocationInfo {
		private Location location;
		private int count = 1;
		private int conflicts = 0;

		LocationInfo(Location location) {
			this.location = location;
		}

		void incorporate(Location location) {
			if (this.location.distanceTo(location) < LateDecider.this
					.getResources().getInteger(
							R.integer.late_decider_location_relevant_distance)) {
				count++;
			} else {
				conflicts++;
			}
		}

		void merge(LocationInfo locationInfo) {
			if (this.location.distanceTo(locationInfo.getLocation()) < LateDecider.this
					.getResources().getInteger(
							R.integer.late_decider_location_relevant_distance)) {
				this.count += locationInfo.getCount();
				this.conflicts += locationInfo.getConflicts();
			} else {
				this.conflicts += locationInfo.getCount();
				this.count += locationInfo.getConflicts();
			}
		}

		int getCount() {
			return count;
		}

		int getConflicts() {
			return conflicts;
		}

		Location getLocation() {
			return location;
		}

	}

	private LocationInfo computeStartingPoint(long dateInMillis) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(dateInMillis);
		int day = cal.get(Calendar.DAY_OF_WEEK);

		String[] projection = { LogProvider.TIMESTAMP, LogProvider.LOG_VALUE };
		String selection = "(" + LogProvider.TIMESTAMP + " >= ?)";
		String[] selectionArgs = { Long.toString(System.currentTimeMillis()
				+ getResources().getInteger(
						R.integer.late_decider_location_relevant_period)) };
		String sortOrder = LogProvider.TIMESTAMP + " DESC";
		Cursor cur = getContentResolver().query(
				Uri.parse(LogProvider.CONTENT_URI + "/" + LocationLogger.NAME),
				projection, selection, selectionArgs, sortOrder);

		SparseArray<LocationInfo> locations = new SparseArray<LocationInfo>();
		while (cur.moveToNext()) {
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(cur.getLong(cur
					.getColumnIndex(LogProvider.TIMESTAMP)));
			if (c.get(Calendar.DAY_OF_WEEK) == day) {
				int hour = c.get(Calendar.HOUR_OF_DAY);
				Location location = LocationLogger.parseValue(cur.getString(cur
						.getColumnIndex(LogProvider.LOG_VALUE)));
				LocationInfo entry = locations.get(hour);
				if (entry != null) {
					entry.incorporate(location);
				} else {
					locations.put(hour, new LocationInfo(location));
				}
			}
		}

		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int checkPeriod = getResources().getInteger(
				R.integer.late_decider_location_check_hour_period);
		int validPeriod = getResources().getInteger(
				R.integer.late_decider_location_valid_hour_period);

		LocationInfo departure = null;
		for (int i = 0; i < checkPeriod && checkPeriod <= validPeriod; i++) {
			int checkHour = hour;
			for (int j = 0; j < i; j++) {
				checkHour--;
				if (checkHour < 0) {
					checkHour = 23;
				}
			}
			LocationInfo l = locations.get(checkHour);
			if (l == null) {
				checkPeriod++;
				continue;
			}
			if (departure == null) {
				departure = l;
				continue;
			}
			departure.merge(l);
		}

		return departure;
	}

	private long[] getInterval(int hour, int minute) {
		Calendar cal = Calendar.getInstance();
		if (hour < cal.get(Calendar.HOUR_OF_DAY)
				|| ((hour == cal.get(Calendar.HOUR_OF_DAY) && (minute < cal
						.get(Calendar.MINUTE))))) {
			cal.add(Calendar.DAY_OF_YEAR, 1);
		}
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, minute);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		long[] ret = new long[2];
		ret[0] = cal.getTimeInMillis();

		cal.add(Calendar.DAY_OF_YEAR, 1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		ret[1] = cal.getTimeInMillis();

		return ret;
	}
}
