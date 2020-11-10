package com.borconi.emil.wifilauncherforhur;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.borconi.emil.wifilauncherforhur.services.WifiService;

import static com.borconi.emil.wifilauncherforhur.services.WifiService.ACTION_FOREGROUND_STOP;

/**
 * Implementation of App Widget functionality.
 */
public class WiFiLauncherServiceWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_wi_fi_launcher_service);

        if (WifiService.isRunning()) {
            if (WifiService.isConnected()) {
                views.setImageViewResource(R.id.appwidget_icon, R.mipmap.ic_widget_connected);
                views.setTextViewText(R.id.appwidget_text, context.getString(R.string.app_widget_connected));
                views.setContentDescription(R.id.appwidget_text, context.getString(R.string.app_widget_connected));
            } else {
                views.setImageViewResource(R.id.appwidget_icon, R.mipmap.ic_widget_running);
                views.setTextViewText(R.id.appwidget_text, context.getString(R.string.app_widget_running));
                views.setContentDescription(R.id.appwidget_text, context.getString(R.string.app_widget_running));

                Intent wifiServiceIntent = new Intent(context, WifiService.class);
                wifiServiceIntent.setAction(ACTION_FOREGROUND_STOP);
                PendingIntent pendingIntent = PendingIntent.getForegroundService(context, 2, wifiServiceIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                views.setOnClickPendingIntent(R.id.appwidget_container, pendingIntent);
            }
        } else {
            views.setImageViewResource(R.id.appwidget_icon, R.mipmap.ic_widget_preview_round);
            views.setTextViewText(R.id.appwidget_text, context.getString(R.string.app_widget_paused));
            views.setContentDescription(R.id.appwidget_text, context.getString(R.string.app_widget_paused));

            Intent wifiServiceIntent = new Intent(context, WifiService.class);
            PendingIntent pendingIntent = PendingIntent.getForegroundService(context, 1, wifiServiceIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            views.setOnClickPendingIntent(R.id.appwidget_container, pendingIntent);
        }

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
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

