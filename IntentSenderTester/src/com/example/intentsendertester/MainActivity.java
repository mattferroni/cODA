package com.example.intentsendertester;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends Activity implements OnClickListener{

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
		sendBroadcast(new Intent("andreadamiani.coda.RUNNING"));
	}
	
	public void onClick2(View v){
		Intent intent = new Intent("andreadamiani.coda.LATE");
		intent.putExtra("ALLARM_MINUTES_EXTRA", 0);
		intent.putExtra("ALLARM_HOUR_EXTRA", 1);
		intent.putExtra("MIN_TIME_EXTRA", System.currentTimeMillis());
		intent.putExtra("DELAY_EXTRA", 2);
		sendBroadcast(intent);
	}
}
