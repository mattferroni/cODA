package andreadamiani.coda.observers.location;

import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import andreadamiani.coda.observers.Observer;
import andreadamiani.coda.observers.location.implementations.GooglePlayLocationObserver;
import andreadamiani.coda.observers.location.implementations.LocationObserverImplementation;
import andreadamiani.coda.observers.location.implementations.SimpleLocationObserver;
import android.content.Context;
import android.content.Intent;



public class LocationObserver extends Observer {
	
	private Map<Context,LocationObserverImplementation> implementations;
	
	public LocationObserver() {
		setService(LocationLogger.class);
		implementations = new HashMap<Context,LocationObserverImplementation>();
	}
	
	@Override
	protected void start(Context context, Intent intent) {
		getImplementation(context).start(context, intent);
	}

	@Override
	protected void dimm(Context context, Intent intent) {
		getImplementation(context).dimm(context, intent);
	}

	@Override
	protected void stop(Context context, Intent intent) {
		getImplementation(context).stop(context, intent);
	}
	
	private LocationObserverImplementation getImplementation(Context context){
		if(!implementations.containsKey(context)){
			try {
				if(GooglePlayServicesUtil.isGooglePlayServicesAvailable(context)==ConnectionResult.SUCCESS){
					implementations.put(context, new GooglePlayLocationObserver());
				} else {
					implementations.put(context, new SimpleLocationObserver());
				}
			} catch (RuntimeException e) {
				return new SimpleLocationObserver();
			}
		}
		return implementations.get(context);
	}
}
