/**
 * 
 */
package andreadamiani.coda;

import android.content.ContentResolver;
import android.content.ContentValues;

/**
 * @author Andrea
 *
 */
public class LogWriter implements Runnable {

	private final ContentResolver contentResolver;
	private final ContentValues contentValues;
	
	private LogWriter(ContentResolver contentResolver, ContentValues contentValues){
		this.contentResolver = contentResolver;
		this.contentValues = contentValues;
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		contentResolver.insert(LogProvider.CONTENT_URI, contentValues);
	}

	
	public static void write(ContentResolver contentResolver, ContentValues contentValues){
		new Thread(new LogWriter(contentResolver, contentValues)).run();
	}
}
