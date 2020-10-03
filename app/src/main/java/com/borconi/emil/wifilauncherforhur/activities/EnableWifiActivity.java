package com.borconi.emil.wifilauncherforhur.activities;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
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

import com.borconi.emil.wifilauncherforhur.R;
import com.borconi.emil.wifilauncherforhur.services.WifiService;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class EnableWifiActivity extends AppCompatActivity {

    public static final int ACTION_WIFI_PANEL_REQUEST_CODE = 240;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null)
            keyguardManager.requestDismissKeyguard(this, null);

        setContentView(R.layout.activity_enable_wifi);

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        }
        Uri notificationRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notificationRingtone);
        r.play();
    }

    public void onTurnOnButtonClick(View v) {
        startActivityForResult(new Intent(Settings.Panel.ACTION_WIFI), ACTION_WIFI_PANEL_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTION_WIFI_PANEL_REQUEST_CODE) {
            WifiService.askingForWiFi = false;
            this.finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        WifiService.askingForWiFi = false;
    }
}