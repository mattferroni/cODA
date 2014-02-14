package com.example.logproviderpreparator;

import java.util.Calendar;

import andreadamiani.coda.observers.location.LocationLogger;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends Activity implements OnClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		ContentResolver cr = getContentResolver();
		
		
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		for(int i=0; i<31; i++){
			cal.add(Calendar.DAY_OF_YEAR, -1);
			ContentValues val;
			int j=0;
			for(; j<12; j++){
				cal.set(Calendar.HOUR_OF_DAY, j);
				val = generateVal(false);
				val.put("TIMESTAMP", cal.getTimeInMillis());
				cr.insert(Uri.parse("content://andreadamiani.coda.log/obs"), val);
			}
			for(;j>=12 && j<24; j++){
				cal.set(Calendar.HOUR_OF_DAY, j);
				val = generateVal(true);
				val.put("TIMESTAMP", cal.getTimeInMillis());
				cr.insert(Uri.parse("content://andreadamiani.coda.log/obs"), val);
			}
		}
	}
	
	private ContentValues generateVal(boolean atWork){
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, 2099);
		
		ContentValues val = new ContentValues();
		val.put("OBSERVER_NAME", LocationLogger.NAME);
		val.put("EXPIRY", expiry.getTimeInMillis());
		if(!atWork){
			val.put("VALUE", "45:30:29.5842;9:25:55.524");
		} else {
			val.put("VALUE", "45:28:45.3714;9:13:54.0516");
		}
		
		return val;
	}

}
