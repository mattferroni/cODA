package andreadamiani.alarmclock;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

/**
 * Helper class for showing and canceling late actuator notifications.
 * <p>
 * This class makes heavy use of the {@link NotificationCompat.Builder} helper
 * class to create notifications in a backward-compatible way.
 */
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class LateNotification {
	public static final String NOTIFICATION_ALARM_MINUTES = "NOTIFICATION_ALARM_MINUTES";
	public static final String NOTIFICATION_ALARM_HOUR = "NOTIFICATION_ALARM_HOUR";
	public static final String NOTIFICATION_TEXT = "NOTIFICATION_TEXT";
	/**
	 * The unique identifier for this type of notification.
	 */
	private static final String NOTIFICATION_TAG = "LateActuator";

	/**
	 * Shows the notification, or updates a previously shown notification of
	 * this type, with the given parameters.
	 * <p>
	 * TODO: Customize this method's arguments to present relevant content in
	 * the notification.
	 * <p>
	 * TODO: Customize the contents of this method to tweak the behavior and
	 * presentation of late actuator notifications. Make sure to follow the <a
	 * href="https://developer.android.com/design/patterns/notifications.html">
	 * Notification design guidelines</a> when doing so.
	 * 
	 * @see #cancel(Context)
	 */
	public static void notify(final Context context,
			final String lateAlarmTime, final String alarmTime) {
		final Resources res = context.getResources();

		final Bitmap picture = BitmapFactory.decodeResource(res,
				R.drawable.ic_launcher);

		final String ticker = res
				.getString(R.string.late_actuator_notification_title_template);
		final String title = res
				.getString(R.string.late_actuator_notification_title_template);
		final String text = res.getString(
				R.string.late_actuator_notification_placeholder_text_template,
				lateAlarmTime, alarmTime);
		final String[] alarmTimeSplit = alarmTime.split(LateActuator.SEPARATOR);

		final Intent intent = new Intent(context, MainActivity.class);
		intent.putExtra(NOTIFICATION_TEXT, text);
		intent.putExtra(NOTIFICATION_ALARM_HOUR,
				Integer.parseInt(alarmTimeSplit[0]));
		intent.putExtra(NOTIFICATION_ALARM_MINUTES,
				Integer.parseInt(alarmTimeSplit[1]));
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);

		final NotificationCompat.Builder builder = new NotificationCompat.Builder(
				context)

				// Set appropriate defaults for the notification light, sound,
				// and vibration.
				.setDefaults(Notification.DEFAULT_ALL)

				// Set required fields, including the small icon, the
				// notification title, and text.
				.setSmallIcon(R.drawable.ic_launcher).setContentTitle(title)
				.setContentText(text)

				// All fields below this line are optional.

				// Use a default priority (recognized on devices running Android
				// 4.1 or later)
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)

				// Provide a large icon, shown with the notification in the
				// notification drawer on devices running Android 3.0 or later.
				.setLargeIcon(picture)

				// Set ticker text (preview) information for this notification.
				.setTicker(ticker)

				// Set the pending intent to be initiated when the user touches
				// the notification.
				.setContentIntent(
						PendingIntent.getActivity(context, 0, intent,
								PendingIntent.FLAG_UPDATE_CURRENT))

				// Automatically dismiss the notification when it is touched.
				.setAutoCancel(true);

		notify(context, builder.build());
	}

	@TargetApi(Build.VERSION_CODES.ECLAIR)
	private static void notify(final Context context,
			final Notification notification) {
		final NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
			nm.notify(NOTIFICATION_TAG, 0, notification);
		} else {
			nm.notify(NOTIFICATION_TAG.hashCode(), notification);
		}
	}

	/**
	 * Cancels any notifications of this type previously shown using
	 * {@link #notify(Context, String, int)}.
	 */
	@TargetApi(Build.VERSION_CODES.ECLAIR)
	public static void cancel(final Context context) {
		final NotificationManager nm = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
			nm.cancel(NOTIFICATION_TAG, 0);
		} else {
			nm.cancel(NOTIFICATION_TAG.hashCode());
		}
	}
}