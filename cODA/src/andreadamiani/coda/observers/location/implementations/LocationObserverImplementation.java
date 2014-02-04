package andreadamiani.coda.observers.location.implementations;

import android.content.Context;
import android.content.Intent;

public interface LocationObserverImplementation {
	void start(Context context, Intent intent);
	void dimm(Context context, Intent intent);
	void stop(Context context, Intent intent);
}
