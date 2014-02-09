/**
 * 
 */
package andreadamiani.coda;

import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import android.util.Log;

/**
 * @author Andrea
 *
 */
public class LogWriter implements Runnable {
	
	private static final String DEBUG_TAG = "[cODA] LOG WRITER";

	private final ArrayList<ContentProviderOperation> batch;
	
	private LogWriter(ArrayList<ContentProviderOperation> batch){
		this.batch = batch;
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			Log.d(DEBUG_TAG, "Writing to log ...");
			Application.getInstance().getContentResolver().applyBatch(LogProvider.AUTHORITY, batch);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			e.printStackTrace();
		}
	}

	
	public static void write(ArrayList<ContentProviderOperation> batch){
		new Thread(new LogWriter(batch)).run();
	}
}
