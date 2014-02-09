package andreadamiani.coda;

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

	private static final String DEBUG_TAG = "[cODA] LOG CONTENT PROVIDER";

	public static final String OBSERVER_FILTER_KEY = "obs";
	public static final String OLDEST_FILTER_KEY = "oldest";
	public static final String LAST_FILTER_KEY = "last";

	public static final String AUTHORITY = "andreadamiani.coda.log";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + OBSERVER_FILTER_KEY);

	public static final SimpleDateFormat DATE_FORMAT;
	static {
		DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	}

	public static final String OBSERVER_NAME = "OBSERVER_NAME";
	public static final String LOG_VALUE = "VALUE";
	public static final String TIMESTAMP = "TIMESTAMP";
	public static final String EXPIRY = "EXPIRY";

	private static final int ALLOBS = 1;
	private static final int SINGLE_OBS = 2;
	private static final int LAST_READ = 3;
	private static final int OLDEST_READ = 4;

	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher
				.addURI("andreadamiani.coda.log", OBSERVER_FILTER_KEY, ALLOBS);
		uriMatcher.addURI("andreadamiani.coda.log", OBSERVER_FILTER_KEY + "/*/"
				+ LAST_FILTER_KEY, LAST_READ);
		uriMatcher.addURI("andreadamiani.coda.log", OBSERVER_FILTER_KEY + "/*",
				SINGLE_OBS);
		uriMatcher.addURI("andreadamiani.coda.log", OBSERVER_FILTER_KEY + "/*/"
				+ OLDEST_FILTER_KEY, OLDEST_READ);
	}

	private MySQLiteOpenHelper myOpenHelper;

	@Override
	public boolean onCreate() {
		myOpenHelper = new MySQLiteOpenHelper(getContext(),
				MySQLiteOpenHelper.DATABASE_NAME, null,
				MySQLiteOpenHelper.DATABASE_VERSION);
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteDatabase db;
		try {
			db = myOpenHelper.getWritableDatabase();
		} catch (SQLiteException ex) {
			db = myOpenHelper.getReadableDatabase();
		}

		String groupBy = null;
		String having = null;
		String obsName = null;
		final String[] min = { " MIN( " + TIMESTAMP + " )" };
		final String[] max = { " MAX( " + TIMESTAMP + " )" };

		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		switch (uriMatcher.match(uri)) {
		case OLDEST_READ:
			obsName = uri.getPathSegments().get(1);
			queryBuilder.appendWhere(TIMESTAMP + " = ("
					+ createInnerQuery(obsName, min) + ")");
			generateSingleObserverQuery(uri, queryBuilder, obsName, true);
			Log.d(DEBUG_TAG, "Querying for the oldest record for " + obsName);
			break;
		case LAST_READ:
			obsName = uri.getPathSegments().get(1);
			queryBuilder.appendWhere(TIMESTAMP + " = ("
					+ createInnerQuery(obsName, max) + ")");
			generateSingleObserverQuery(uri, queryBuilder, obsName, true);
			Log.d(DEBUG_TAG, "Querying for the last record for " + obsName);
			break;
		case SINGLE_OBS:
			generateSingleObserverQuery(uri, queryBuilder, obsName, false);
			Log.d(DEBUG_TAG, "Querying for all records for " + obsName);
			break;
		default:
			Log.d(DEBUG_TAG, "Querying for the entire log");
			break;
		}

		queryBuilder.setTables(MySQLiteOpenHelper.DATABASE_TABLE);

		return queryBuilder.query(db, projection, selection, selectionArgs,
				groupBy, having, sortOrder);
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = myOpenHelper.getWritableDatabase();

		this.delete(LogProvider.CONTENT_URI, null, null);
		
		Log.d(DEBUG_TAG,
				"Inserting log entry: <" + values.getAsString(TIMESTAMP) + ","
						+ values.getAsString(OBSERVER_NAME) + ","
						+ values.getAsString(LOG_VALUE) + ","
						+ values.getAsString(EXPIRY) + ">");
		db.insert(MySQLiteOpenHelper.DATABASE_TABLE, null, values);
		getContext().getContentResolver().notifyChange(uri, null);
		return Uri.parse(CONTENT_URI + "/" + values.getAsString(OBSERVER_NAME)
				+ "/" + LAST_FILTER_KEY);
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = myOpenHelper.getWritableDatabase();

		switch (uriMatcher.match(uri)) {
		case ALLOBS:
			selection = EXPIRY + " <= " + System.currentTimeMillis() + " ";
			Log.d(DEBUG_TAG, "Deleting the outdated records ...");
			break;
		default:
			throw new IllegalArgumentException(
					"Selective deletion not implemented");
		}

		int deleteCount = db.delete(MySQLiteOpenHelper.DATABASE_TABLE,
				selection, null);
		getContext().getContentResolver().notifyChange(uri, null);
		return deleteCount;
	}

	@Override
	public String getType(Uri uri) {
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

	static public String timestampToString(long millis) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(millis);
		return DATE_FORMAT.format(c.getTime());
	}
	
	private String createInnerQuery(String obsName, String[] cols) {
		return SQLiteQueryBuilder.buildQueryString(true,
				MySQLiteOpenHelper.DATABASE_TABLE, cols, OBSERVER_NAME + " = "
						+ "'" + obsName + "'", null, null, null, null);
	}

	private void generateSingleObserverQuery(Uri uri,
			SQLiteQueryBuilder queryBuilder, String obsName, boolean appending) {
		if (obsName != null) {
			obsName = uri.getPathSegments().get(1);
		}
		queryBuilder.appendWhere((appending ? " AND " : " ") + OBSERVER_NAME
				+ " = " + "'" + obsName + "'");
	}

	private static class MySQLiteOpenHelper extends SQLiteOpenHelper {

		// Database name, version, and table names.
		private static final String DATABASE_NAME = "log.db";
		private static final int DATABASE_VERSION = 1;
		private static final String DATABASE_TABLE = "mainTable";

		// SQL Statement to create a new database.
		private static final String DATABASE_CREATE = "create table "
				+ DATABASE_TABLE + " (" + TIMESTAMP + " integer not null, "
				+ OBSERVER_NAME + " text not null, " + LOG_VALUE
				+ " text not null, " + EXPIRY + " integer not null);";

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
