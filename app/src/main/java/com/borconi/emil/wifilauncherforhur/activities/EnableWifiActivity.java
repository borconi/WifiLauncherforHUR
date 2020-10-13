package com.borconi.emil.wifilauncherforhur.activities;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.borconi.emil.wifilauncherforhur.R;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class EnableWifiActivity extends AppCompatActivity {

    public static final int ACTION_WIFI_PANEL_REQUEST_CODE = 240;
    public static final int FULL_SCREEN_INTENT_REQUEST_CODE = 250;
    public static final int TURN_ON_ACTION_REQUEST_CODE = 251;
    public static final int WIFI_NOTIFICATION_ID = 4000;

    public static final String FINISH_WIFI_USER_ACTION_ACTIVITIES_INTENT = "com.borconi.emil.wifilauncherforhur.action.FINISH_WIFI_USER_ACTION_ACTIVITIES";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null)
            keyguardManager.requestDismissKeyguard(this, null);

        registerReceiver(finishBroadcastReceiver, new IntentFilter(FINISH_WIFI_USER_ACTION_ACTIVITIES_INTENT));

        setContentView(R.layout.activity_enable_wifi);

        if (getIntent().getBooleanExtra("DISPLAY_ACTION_PANEL", false)) {
            findViewById(R.id.turn_on_button).setVisibility(View.INVISIBLE);
            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(WIFI_NOTIFICATION_ID);
            startActivityForResult(new Intent(Settings.Panel.ACTION_WIFI), ACTION_WIFI_PANEL_REQUEST_CODE);
        }
    }

    public void onTurnOnButtonClick(View v) {
        startActivityForResult(new Intent(Settings.Panel.ACTION_WIFI), ACTION_WIFI_PANEL_REQUEST_CODE);
    }

    public static Notification getNotification(Context context, String channelId) {
        Intent fullScreenIntent = new Intent(context, EnableWifiActivity.class);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, FULL_SCREEN_INTENT_REQUEST_CODE,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_aa_wifi_notification)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_wifi_off_description))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPendingIntent, true);

        Intent turnOnIntent = new Intent(context, EnableWifiActivity.class);
        turnOnIntent.putExtra("DISPLAY_ACTION_PANEL", true);

        PendingIntent turnOnPendingIntent =
                PendingIntent.getActivity(context, TURN_ON_ACTION_REQUEST_CODE, turnOnIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.addAction(
                new NotificationCompat.Action.Builder(R.drawable.ic_power_settings_new_24,
                        context.getString(R.string.turn_on),
                        turnOnPendingIntent).build());

        return builder.build();
    }

    private final BroadcastReceiver finishBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTION_WIFI_PANEL_REQUEST_CODE) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setShowWhenLocked(false);
        setTurnScreenOn(false);
        unregisterReceiver(finishBroadcastReceiver);
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(WIFI_NOTIFICATION_ID);
    }
}