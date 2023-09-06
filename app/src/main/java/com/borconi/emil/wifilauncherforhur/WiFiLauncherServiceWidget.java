package com.borconi.emil.wifilauncherforhur;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.borconi.emil.wifilauncherforhur.services.WifiService;

import static com.borconi.emil.wifilauncherforhur.services.WifiService.ACTION_FOREGROUND_STOP;
import static com.borconi.emil.wifilauncherforhur.services.WifiService.isRunning;

/**
 * Implementation of App Widget functionality.
 */
public class WiFiLauncherServiceWidget extends AppWidgetProvider {

    public final static String WIDGET_ACTION="com.borconi.emil.wifilauncherforhur.widget";
    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        // Construct the RemoteViews object
        Log.d("Widget","Update widget called");
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_wi_fi_launcher_service);

        Intent intent = new Intent(context, WiFiLauncherServiceWidget.class);
        intent.setAction(WIDGET_ACTION);
        PendingIntent pd;
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.S)
            pd = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        else
            pd = PendingIntent.getBroadcast(context, 0, intent, 0);

        if (WifiService.isRunning()) {
                views.setImageViewResource(R.id.appwidget_icon, R.mipmap.ic_widget_running);
                views.setTextViewText(R.id.appwidget_text, context.getString(R.string.app_widget_running));
                views.setContentDescription(R.id.appwidget_text, context.getString(R.string.app_widget_running));


        } else {
            views.setImageViewResource(R.id.appwidget_icon, R.mipmap.ic_widget_preview_round);
            views.setTextViewText(R.id.appwidget_text, context.getString(R.string.app_widget_paused));
            views.setContentDescription(R.id.appwidget_text, context.getString(R.string.app_widget_paused));
        }
        views.setOnClickPendingIntent(R.id.appwidget_container, pd);
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d("Widget","Got action: "+intent.getAction());
        if (intent.getAction()==null)
            return;

        if (!intent.getAction().equalsIgnoreCase(WIDGET_ACTION))
            return;
        try {
            if (isRunning())
                context.stopService(new Intent(context, WifiService.class));
            else
                context.startForegroundService(new Intent(context, WifiService.class));
        }
        catch (Exception e){
            Toast.makeText(context,context.getString(R.string.cant_start),Toast.LENGTH_LONG).show();
        }
    };
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        Log.d("Widget","Update widget called");
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

