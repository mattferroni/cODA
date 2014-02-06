package andreadamiani.coda;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

@SuppressLint("SimpleDateFormat")
public class LogProvider extends ContentProvider {

	public static final Uri CONTENT_URI = Uri
			.parse("content://andreadamiani.coda.log/obs");

	public static final SimpleDateFormat DATE_FORMAT;

	static {
		DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}
	public static final String OBSERVER_NAME = "OBSERVER_NAME";
	public static final String LOG_VALUE = "VALUE";
	public static final String TIMESTAMP = "TIMESTAMP";
	public static final String EXPIRY = "EXPIRY";

	// Create the constants used to differentiate between the different URI
	// requests.
	private static final int ALLOBS = 1;
	private static final int SINGLE_OBS = 2;
	private static final int LAST_READ = 3;
	private static final int OLDEST_READ = 4;

	private static final UriMatcher uriMatcher;

	// Populate the UriMatcher object, where a URI ending in
	// 'elements' will correspond to a request for all items,
	// and 'elements/[rowID]' represents a single row.
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI("andreadamiani.coda.log", "obs", ALLOBS);
		uriMatcher.addURI("andreadamiani.coda.log", "obs/*/last", LAST_READ);
		uriMatcher.addURI("andreadamiani.coda.log", "obs/*", SINGLE_OBS);
		uriMatcher
				.addURI("andreadamiani.coda.log", "obs/*/oldest", OLDEST_READ);
	}

	private MySQLiteOpenHelper myOpenHelper;

	@Override
	public boolean onCreate() {
		// Construct the underlying database.
		// Defer opening the database until you need to perform
		// a query or transaction.
		myOpenHelper = new MySQLiteOpenHelper(getContext(),
				MySQLiteOpenHelper.DATABASE_NAME, null,
				MySQLiteOpenHelper.DATABASE_VERSION);

		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		// Open the database.
		SQLiteDatabase db;
		try {
			db = myOpenHelper.getWritableDatabase();
		} catch (SQLiteException ex) {
			db = myOpenHelper.getReadableDatabase();
		}

		// Replace these with valid SQL statements if necessary.
		String groupBy = null;
		String having = null;

		// Use an SQLite Query Builder to simplify constructing the
		// database query.
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		String obsName = null;
		final String[] min = { "MIN(" + TIMESTAMP + ")" };
		final String[] max = { "MAX(" + TIMESTAMP + ")" };

		// If this is a row query, limit the result set to the passed in row.
		switch (uriMatcher.match(uri)) {
		case OLDEST_READ:
			obsName = uri.getPathSegments().get(1);
			queryBuilder.appendWhere(TIMESTAMP + "="
					+ createInnerQuery(obsName, min));
			whereObsQuery(uri, queryBuilder, obsName);
			break;
		case LAST_READ:
			obsName = uri.getPathSegments().get(1);
			queryBuilder.appendWhere(TIMESTAMP + "="
					+ createInnerQuery(obsName, max));
			whereObsQuery(uri, queryBuilder, obsName);
			break;
		case SINGLE_OBS:
			whereObsQuery(uri, queryBuilder, obsName);
			break;
		default:
			break;
		}

		// Specify the table on which to perform the query. This can
		// be a specific table or a join as required.
		queryBuilder.setTables(MySQLiteOpenHelper.DATABASE_TABLE);

		// Execute the query.
		Cursor cursor = queryBuilder.query(db, projection, selection,
				selectionArgs, groupBy, having, sortOrder);

		// Return the result Cursor.
		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		// Return a string that identifies the MIME type
		// for a Content Provider URI
		switch (uriMatcher.match(uri)) {
		case ALLOBS:
		case SINGLE_OBS:
		case LAST_READ:
		case OLDEST_READ:
			return "vnd.android.cursor.dir/andreadamiani.coda.log.elemental";
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// Open a read / write database to support the transaction.
		SQLiteDatabase db = myOpenHelper.getWritableDatabase();

		// If this is a row URI, limit the deletion to the specified row.
		switch (uriMatcher.match(uri)) {
		case OLDEST_READ:
			String obsName = uri.getPathSegments().get(1);
			String[] cols = { "MIN(" + TIMESTAMP + ")" };
			selection = TIMESTAMP + "=" + createInnerQuery(obsName, cols)
					+ " AND " + OBSERVER_NAME + "=" + obsName;
			break;
		default:
			throw new IllegalArgumentException(
					"Partial deletion not implemented");
		}

		// Perform the deletion.
		int deleteCount = db.delete(MySQLiteOpenHelper.DATABASE_TABLE,
				selection, selectionArgs);

		// Notify any observers of the change in the data set.
		getContext().getContentResolver().notifyChange(uri, null);

		// Return the number of deleted items.
		return deleteCount;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// Open a read / write database to support the transaction.
		SQLiteDatabase db = myOpenHelper.getWritableDatabase();

		while (true) {
			Cursor cursor = query(
					Uri.parse(CONTENT_URI + values.getAsString(OBSERVER_NAME)
							+ "/oldest"), null, null, null, null);
			if (cursor.getCount() > 0
					&& parseToTimestamp(cursor.getString(cursor
							.getColumnIndex(TIMESTAMP))) < parseToTimestamp(cursor
							.getString(cursor.getColumnIndex(EXPIRY)))) {
				delete(Uri.parse(CONTENT_URI
						+ values.getAsString(OBSERVER_NAME) + "/oldest"), null,
						null);
			} else {
				break;
			}
		}

		// Insert the values into the table
		db.insert(MySQLiteOpenHelper.DATABASE_TABLE, null, values);

		// Notify any observers of the change in the data set.
		getContext().getContentResolver().notifyChange(uri, null);

		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {

		throw new UnsupportedOperationException("Not supported");
	}

	static public String parseTimestamp(long millis) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(millis);
		return DATE_FORMAT.format(c.getTime());
	}

	static public long parseToTimestamp(String date) {
		Calendar c = Calendar.getInstance();
		try {
			c.setTime(DATE_FORMAT.parse(date));
		} catch (ParseException e) {
			throw new IllegalArgumentException("Date format not parsable");
		}
		return c.getTimeInMillis();
	}

	private String createInnerQuery(String obsName, String[] cols) {
		return SQLiteQueryBuilder.buildQueryString(true,
				MySQLiteOpenHelper.DATABASE_TABLE, cols, OBSERVER_NAME + "="
						+ obsName, null, null, null, null);
	}

	private void whereObsQuery(Uri uri, SQLiteQueryBuilder queryBuilder,
			String obsName) {
		if (obsName != null) {
			obsName = uri.getPathSegments().get(1);
		}
		queryBuilder.appendWhere(OBSERVER_NAME + "=" + obsName);
	}

	private static class MySQLiteOpenHelper extends SQLiteOpenHelper {

		// Database name, version, and table names.
		private static final String DATABASE_NAME = "log.db";
		private static final int DATABASE_VERSION = 1;
		private static final String DATABASE_TABLE = "mainTable";

		// SQL Statement to create a new database.
		private static final String DATABASE_CREATE = "create table "
				+ DATABASE_TABLE + " (" + TIMESTAMP + " text not null, "
				+ OBSERVER_NAME + " text not null, " + LOG_VALUE
				+ " text not null, " + EXPIRY + " text not null);";

		public MySQLiteOpenHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		// Called when no database exists in disk and the helper class needs
		// to create a new one.
		@Override
		public void onCreate(SQLiteDatabase _db) {
			_db.execSQL(DATABASE_CREATE);
		}

		// Called when there is a database version mismatch meaning that the
		// version
		// of the database on disk needs to be upgraded to the current version.
		@Override
		public void onUpgrade(SQLiteDatabase _db, int _oldVersion,
				int _newVersion) {
			// Log the version upgrade.
			Log.w("TaskDBAdapter", "Upgrading from version " + _oldVersion
					+ " to " + _newVersion
					+ ", which will destroy all old data");

			// Upgrade the existing database to conform to the new version.
			// Multiple
			// previous versions can be handled by comparing _oldVersion and
			// _newVersion
			// values.

			// The simplest case is to drop the old table and create a new one.
			_db.execSQL("DROP TABLE IF IT EXISTS " + DATABASE_TABLE);
			// Create a new one.
			onCreate(_db);
		}
	}
}
