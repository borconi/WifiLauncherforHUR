package com.borconi.emil.wifilauncherforhur.activities;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.borconi.emil.wifilauncherforhur.R;
import com.borconi.emil.wifilauncherforhur.services.WifiService;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class EnableWifiActivity extends AppCompatActivity {

    public static final int ACTION_WIFI_PANEL_REQUEST_CODE = 240;
    boolean pressedTurnOnButton = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null)
            keyguardManager.requestDismissKeyguard(this, null);

        setContentView(R.layout.activity_enable_wifi);
    }

    public void onTurnOnButtonClick(View v) {
        pressedTurnOnButton = true;
        startActivityForResult(new Intent(Settings.Panel.ACTION_WIFI), ACTION_WIFI_PANEL_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EnableWifiActivity.ACTION_WIFI_PANEL_REQUEST_CODE) {
            WifiService.askingForWiFi = false;
            this.finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!pressedTurnOnButton) {
            WifiService.askingForWiFi = false;
        }
    }
}