package com.borconi.emil.wifilauncherforhur.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.borconi.emil.wifilauncherforhur.services.WifiService;

public class QrDeepLinkActivity extends Activity {

    private static final String TAG = "QrDeepLink";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null && "wifilauncher".equals(data.getScheme())) {
                try {
                    // Android automatically parses the URI: wifilauncher://hotspot?ssid=MY_SSID&password=MY_PASS
                    String ssid = data.getQueryParameter("ssid");
                    String pass = data.getQueryParameter("password");

                    if (pass == null) pass = ""; // Handle open networks gracefully

                    Log.d(TAG, "Scanned QR Code. SSID: " + ssid + " | Pass: " + pass);

                    if (ssid != null && !ssid.isEmpty()) {
                        // Forward credentials to WifiService
                        Intent serviceIntent = new Intent(this, WifiService.class);
                        serviceIntent.setAction(WifiService.ACTION_CONNECT_QR_WIFI);
                        serviceIntent.putExtra(WifiService.EXTRA_SSID, ssid);
                        serviceIntent.putExtra(WifiService.EXTRA_PASS, pass);

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent);
                        } else {
                            startService(serviceIntent);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse custom scheme QR data", e);
                }
            }
        }

        // Terminate the trampoline activity immediately so the user doesn't see a blank screen
        finish();
    }
}