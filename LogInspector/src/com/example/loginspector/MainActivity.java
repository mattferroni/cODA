package com.example.loginspector;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.widget.EditText;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

	}

	@Override
	protected void onResume() {
		super.onResume();
		Cursor cur = getContentResolver().query(
				Uri.parse("content://andreadamiani.coda.log/obs" + "/"
						+ "ACCELEROMETER"), null, null, null,
				"TIMESTAMP" + " ASC");
		EditText text = (EditText) findViewById(R.id.text);
		while (cur.moveToNext()) {
			text.append(SimpleDateFormat.getDateTimeInstance().format(
							new Date(cur.getLong(cur
									.getColumnIndex("TIMESTAMP"))))
					+ ";");
			text.append(cur.getString(cur.getColumnIndex("VALUE"))
					+ "\n");

		}
		ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText("label", text.getText());
		cm.setPrimaryClip(clip);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
