package andreadamiani.coda;

import andreadamiani.coda.observers.Observer.ObsAction;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/**
 * Implementation of App Widget functionality.
 */
public class ControllerWidget extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		AppWidgetManager man = AppWidgetManager.getInstance(context);
		String actionS = Application.getInternalAction(intent);
		if (Application.isInternalIntent(intent)) {
			for (ObsAction action : ObsAction.values()) {
				if (actionS.equals(action.name())) {
					onUpdate(context, man,
							man.getAppWidgetIds(new ComponentName(context.getPackageName(), ControllerWidget.class.getName())));
				}
			}
		} else if (actionS.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
			onUpdate(
					context,
					AppWidgetManager.getInstance(context),
					intent.getExtras().getIntArray(
							AppWidgetManager.EXTRA_APPWIDGET_IDS));
		} else if (actionS.equals(AppWidgetManager.ACTION_APPWIDGET_DISABLED)) {
			onDisabled(context);
		} else if (actionS.equals(AppWidgetManager.ACTION_APPWIDGET_ENABLED)) {
			onEnabled(context);
		}
	}

	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		final int N = appWidgetIds.length;
		for (int i = 0; i < N; i++) {
			updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
		}
	}

	public void onEnabled(Context context) {
		// Enter relevant functionality for when the first widget is created
	}

	public void onDisabled(Context context) {
		// Enter relevant functionality for when the last widget is disabled
	}

	static void updateAppWidget(Context context,
			AppWidgetManager appWidgetManager, int appWidgetId) {

		CharSequence widgetText = context.getString(R.string.appwidget_text);
		int[] icons = { R.drawable.ic_off, R.drawable.ic_dimm, R.drawable.ic_on };
		String[] switchText = {
				context.getString(R.string.widget_button_enable),
				context.getString(R.string.widget_button_disable),
				context.getString(R.string.widget_button_disable) };
		PendingIntent[] switchAction = {
				PendingIntent.getBroadcast(
						context,
						0,
						new Intent(Application
								.formatIntentAction(ObsAction.START.name())),
						PendingIntent.FLAG_UPDATE_CURRENT),
				PendingIntent.getBroadcast(
						context,
						0,
						new Intent(Application
								.formatIntentAction(ObsAction.STOP.name())),
						PendingIntent.FLAG_UPDATE_CURRENT),
				PendingIntent.getBroadcast(
						context,
						0,
						new Intent(Application
								.formatIntentAction(ObsAction.STOP.name())),
						PendingIntent.FLAG_UPDATE_CURRENT) };
		Application app = Application.getInstance();
		int appState = 0;
		if (app != null) {
			appState = Application.getInstance().getState().id;
		}

		// Construct the RemoteViews object
		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.controller_widget);
		views.setTextViewText(R.id.widget_text, widgetText);
		views.setImageViewResource(R.id.widget_LED, icons[appState]);
		views.setCharSequence(R.id.widget_switch, "setText",
				switchText[appState]);
		views.setOnClickPendingIntent(R.id.widget_switch,
				switchAction[appState]);

		// Instruct the widget manager to update the widget
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}
}
