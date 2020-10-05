package com.borconi.emil.wifilauncherforhur.activities;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.borconi.emil.wifilauncherforhur.R;
import com.borconi.emil.wifilauncherforhur.services.WifiService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.Task;

public class EnableLocationActivity extends AppCompatActivity {

    public static final String DISMISS_ASKING_FOR_LOCATION_EXTRA = "DISMISS_ASKING_FOR_LOCATION";
    public static final int DISMISS_ASKING_FOR_LOCATION_REQUEST_CODE = 285;
    public static final int ACTION_LOCATION_ENABLE_RESOLUTION_REQUEST_CODE = 330;
    public static final int ACTION_LOCATION_SOURCE_SETTINGS_REQUEST_CODE = 280;
    public static final int FULL_SCREEN_INTENT_REQUEST_CODE = 270;
    public static final int TURN_ON_ACTION_REQUEST_CODE = 271;
    public static final int LOCATION_NOTIFICATION_ID = 4100;

    public static final String FINISH_LOCATION_USER_ACTION_ACTIVITIES_INTENT = "com.borconi.emil.wifilauncherforhur.action.FINISH_LOCATION_USER_ACTION_ACTIVITIES";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null)
            keyguardManager.requestDismissKeyguard(this, null);

        registerReceiver(finishBroadcastReceiver, new IntentFilter(FINISH_LOCATION_USER_ACTION_ACTIVITIES_INTENT));

        setContentView(R.layout.activity_enable_location);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (getIntent().getBooleanExtra("DISPLAY_LOCATION_SCREEN", false)) {
                View turnOnButton = findViewById(R.id.turn_on_button);
                turnOnButton.setVisibility(View.INVISIBLE);
                NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(LOCATION_NOTIFICATION_ID);
                onTurnOnButtonClick(turnOnButton);
            }
        } else {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            }
            Uri notificationRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notificationRingtone);
            r.play();
        }
    }

    public void onTurnOnButtonClick(View v) {
        // Android Auto needs Google Play Services, but just in case we are checking it.
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            // This will show a popup to user to enable Location.
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(10000);
            locationRequest.setFastestInterval(5000);
            locationRequest.setExpirationDuration(-1); // this will expire now this locationRequest.
            locationRequest.setNumUpdates(1);
            locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);
            Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());
            task.addOnFailureListener(this, e -> {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(EnableLocationActivity.this,
                                ACTION_LOCATION_ENABLE_RESOLUTION_REQUEST_CODE);
                    } catch (IntentSender.SendIntentException sendEx) {
                        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), ACTION_LOCATION_SOURCE_SETTINGS_REQUEST_CODE);
                    }
                }
            });
        } else { // If for some weird reason Google Play Services is not present (it shouldn't) we send the user to Location screen to enable it.
            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), ACTION_LOCATION_SOURCE_SETTINGS_REQUEST_CODE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static Notification getNotification(Context context, String channelId) {
        Intent fullScreenIntent = new Intent(context, EnableLocationActivity.class);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, FULL_SCREEN_INTENT_REQUEST_CODE,
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent deleteIntent = new Intent(context, WifiService.class);
        deleteIntent.putExtra(DISMISS_ASKING_FOR_LOCATION_EXTRA, true);

        PendingIntent deletePendingIntent = PendingIntent.getForegroundService(context, DISMISS_ASKING_FOR_LOCATION_REQUEST_CODE, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_aa_wifi_notification)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_location_off_description))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDeleteIntent(deletePendingIntent)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPendingIntent, true);

        Intent turnOnIntent = new Intent(context, EnableLocationActivity.class);
        turnOnIntent.putExtra("DISPLAY_LOCATION_SCREEN", true);

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
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isLocationEnabled()) {
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case ACTION_LOCATION_ENABLE_RESOLUTION_REQUEST_CODE:
            case ACTION_LOCATION_SOURCE_SETTINGS_REQUEST_CODE:
                WifiService.askingForLocation = false;
                finish();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(finishBroadcastReceiver);
        WifiService.askingForLocation = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setShowWhenLocked(false);
            setTurnScreenOn(false);
            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(LOCATION_NOTIFICATION_ID);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            setShowWhenLocked(false);
            setTurnScreenOn(false);
        }
    }
}