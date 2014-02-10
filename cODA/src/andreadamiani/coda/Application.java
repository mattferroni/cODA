/**
 * 
 */
package andreadamiani.coda;

import andreadamiani.coda.observers.Observer.ObsAction;
import android.content.Intent;
import android.util.Log;

/**
 * @author Andrea
 *
 */
public class Application extends android.app.Application {
	
	private static final String DEBUG_TAG = "cODA APPLICATION";
	
	private static Application instance;
	
	private ObsAction state = ObsAction.STOP;
	
	public static Application getInstance(){
		return instance;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(DEBUG_TAG, "The Application is starting...");
		instance = this;
	}
	
	private static void checkInstance(){
		if(instance == null) {
			throw new IllegalStateException("The application is not running!");
		}
	}
	
	public static String formatIntentAction(String action){
		checkInstance();
		return instance.getPackageName() + "." + action;
	}
	
	public static boolean isInternalIntent(Intent intent){
		checkInstance();
		return intent.getAction().startsWith(instance.getPackageName());
	}
	
	public static String getInternalAction(Intent intent){
		try {
			if(isInternalIntent(intent)){
				return intent.getAction().substring(instance.getPackageName().length()+1);
			} else {
				return intent.getAction();
			}
		} catch (IllegalStateException e) {
			return intent.getAction();
		}
	}

	public ObsAction getState() {
		return state;
	}
	
	public void setState(ObsAction state){
		this.state = state;
	}
}