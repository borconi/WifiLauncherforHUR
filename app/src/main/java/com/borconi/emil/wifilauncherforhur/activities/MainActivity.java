package com.borconi.emil.wifilauncherforhur.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.Bundle;


import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.borconi.emil.wifilauncherforhur.R;
import com.borconi.emil.wifilauncherforhur.listeners.WifiListener;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class MainActivity extends AppCompatActivity {

    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 12345;
    private static final int ACTION_WIFI_SETTINGS_REQUEST_CODE = 240;
    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 200;
    private static final int ACCESS_BACKGROUND_LOCATION_REQUEST_CODE = 210;
    private MainActivity activity;
    private static boolean requestedAccessFineLocation = false;
    private static AlertDialog currentPermissionDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
        activity = this;

        Intent intent = getIntent();
        boolean showWifiAlertDialog = intent.getBooleanExtra("showWifiAlertDialog", false);
        if (showWifiAlertDialog) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getResources().getString(R.string.wifi_off_title));
            builder.setMessage(getResources().getString(R.string.wifi_off_description));
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), 100);
                    dialog.dismiss();
                }
            });
            builder.setOnCancelListener(dialog -> WifiListener.askingForWiFi = false);
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

        if (checkOverlays()) {
            return;
        }

        if (checkPermissions()) {
            return;
        };

        enableWifiSuggestions();
    }

    protected boolean checkOverlays() {
        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getResources().getString(R.string.permission_denied));
            builder.setMessage(getResources().getString(R.string.need_draw_over_other_apps));
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
                    dialog.dismiss();
                }
            });
            builder.setOnCancelListener(dialog -> checkOverlays());
            builder.show();
            return true;
        }
        return false;
    }

    protected boolean checkPermissions() {
        boolean hasAccessFineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        List<String> permissions = new ArrayList<String>() {{
            add(Manifest.permission.ACCESS_FINE_LOCATION);
        }};
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        if (!hasAccessFineLocationPermission && !requestedAccessFineLocation) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), ACCESS_FINE_LOCATION_REQUEST_CODE);
            return true;
        }

        // Incremental location permission due to Android 11 best practice enforcement https://developer.android.com/training/location/permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            boolean hasAccessBackgroundLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;

            if (!hasAccessBackgroundLocationPermission && currentPermissionDialog == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getResources().getString(R.string.permission_denied));
                builder.setMessage(getResources().getString(R.string.need_background_location));
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                        currentPermissionDialog = null;
                        ActivityCompat.requestPermissions(activity, new String[] { Manifest.permission.ACCESS_BACKGROUND_LOCATION }, ACCESS_BACKGROUND_LOCATION_REQUEST_CODE);
                    }
                });
                builder.setOnCancelListener(dialog -> {
                    currentPermissionDialog = null;
                    checkPermissions();
                });
                currentPermissionDialog = builder.show();
                return true;
            }
        }
        return false;
    }

    protected void enableWifiSuggestions() {
        // addNetwork starting Android 10 is deprecated instead we need to use addNetworkSuggestions in order to connect to desired WiFi
        // https://developer.android.com/reference/android/net/wifi/WifiManager#addNetwork(android.net.wifi.WifiConfiguration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiNetworkSuggestion wifiNetworkSuggestion = new WifiNetworkSuggestion
                    .Builder()
                    .setSsid("HUR")
                    .setWpa2Passphrase("AndroidAutoConnect")
                    .setIsAppInteractionRequired(true)
                    .build();
            ArrayList<WifiNetworkSuggestion> wifiSuggestionsList = new ArrayList<>();
            wifiSuggestionsList.add(wifiNetworkSuggestion);
            wifiManager.addNetworkSuggestions(wifiSuggestionsList);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0) {
            switch (requestCode) {
                case ACTION_WIFI_SETTINGS_REQUEST_CODE:
                    WifiListener.askingForWiFi = false;
                    break;
                case ACCESS_FINE_LOCATION_REQUEST_CODE:
                    requestedAccessFineLocation = true;
                case ACCESS_BACKGROUND_LOCATION_REQUEST_CODE:
                    if (IntStream.of(grantResults).anyMatch(p -> p == -1) && currentPermissionDialog == null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(getResources().getString(R.string.permission_denied));
                        builder.setMessage(getResources().getString(R.string.permission_denied_loc));
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                                currentPermissionDialog = null;
                                dialog.dismiss();
                            }
                        });
                        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                currentPermissionDialog = null;
                                checkPermissions();
                                dialog.dismiss();
                            }
                        });
                        builder.setOnCancelListener(dialog -> {
                            currentPermissionDialog = null;
                            checkPermissions();
                        });
                        currentPermissionDialog = builder.show();
                    } else {
                        currentPermissionDialog = null;
                    }
                    break;
            }
        }
    }
}
