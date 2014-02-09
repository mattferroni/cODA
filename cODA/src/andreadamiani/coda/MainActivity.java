package andreadamiani.coda;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {

	private TextView text;
	
	private class ContentObserver extends android.database.ContentObserver {

		private Handler handler;
		
		public ContentObserver(Handler handler) {
			super(handler);
			this.handler = handler;
		}

		@Override
		public void onChange(boolean selfChange) {
			this.onChange(selfChange, null);
		}

		@Override
		public void onChange(boolean selfChange, Uri uri) {
			ContentResolver cr = MainActivity.this.getContentResolver();
			Cursor c = cr
					.query(LogProvider.CONTENT_URI, null, null, null, null);
			StringBuilder string = new StringBuilder();
			if (c.getCount() <= 0) {
				return;
			}
			while(c.moveToNext()) {
				string.append("TIMESTAMP="
						+ c.getString(c.getColumnIndex(LogProvider.TIMESTAMP))
						+ "\n"
						+ "OBSERVER="
						+ c.getString(c
								.getColumnIndex(LogProvider.OBSERVER_NAME))
						+ "\n"
						+ "VALUE="
						+ c.getString(c.getColumnIndex(LogProvider.LOG_VALUE))
						+ "\n"
						+ "EXPIRY="
						+ c.getString(c.getColumnIndex(LogProvider.EXPIRY))
								+ "\n" + "----------" + "\n");
			}
			Bundle data = new Bundle();
			data.putString(DATA, string.toString());
			Message msg = new Message();
			msg.setData(data);
			handler.dispatchMessage(msg);
		}
	}
	
	private static final int MSG_DATA = 0;
	private static final String DATA = "DATA";
	
	static class DataHandler extends Handler {
		
		private final WeakReference<MainActivity> mActivity;
		
		DataHandler(MainActivity service) {
			mActivity = new WeakReference<MainActivity>(service);
		}
		
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_DATA:
				mActivity.get().text.setText(msg.getData().getString(DATA));
			default:
				super.handleMessage(msg);
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_codawidget);
		text = (TextView) findViewById(R.id.text);
		getContentResolver().registerContentObserver(LogProvider.CONTENT_URI, true, new ContentObserver(new DataHandler(this)));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void start(View arg0) {
		Application.getInstance().sendBroadcast(
				new Intent(Application.getInstance().getPackageName()
						+ ".START"));
	}

	public void dimm(View arg0) {
		Application.getInstance()
				.sendBroadcast(
						new Intent(Application.getInstance().getPackageName()
								+ ".DIMM"));
	}

	public void stop(View arg0) {
		Application.getInstance()
				.sendBroadcast(
						new Intent(Application.getInstance().getPackageName()
								+ ".STOP"));
	}
}
