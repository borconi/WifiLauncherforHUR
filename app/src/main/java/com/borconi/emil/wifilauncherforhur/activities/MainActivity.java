package com.borconi.emil.wifilauncherforhur.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.borconi.emil.wifilauncherforhur.R;
import com.borconi.emil.wifilauncherforhur.utils.PermissionsUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

public class MainActivity extends AppCompatActivity {

    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 12345;
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
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!checkOverlays()) {
            return;
        }

        if (!checkPermissions()) {
            return;
        }

        enableWifiSuggestions();
    }

    protected boolean checkOverlays() {
        if (!PermissionsUtils.hasDrawOverlays(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getResources().getString(R.string.alert_permission_denied_title));
            builder.setMessage(getResources().getString(R.string.alert_need_draw_over_other_apps));
            builder.setPositiveButton("OK", (dialog, id) -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
                dialog.dismiss();
            });
            builder.setOnCancelListener(dialog -> checkOverlays());
            builder.show();
            return false;
        }
        return true;
    }

    protected boolean checkPermissions() {
        List<String> permissions = new ArrayList<String>() {{
            add(Manifest.permission.ACCESS_FINE_LOCATION);
        }};
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        if (!PermissionsUtils.hasAccessFineLocation(this) && !requestedAccessFineLocation) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), ACCESS_FINE_LOCATION_REQUEST_CODE);
            return false;
        }

        // Incremental location permission due to Android 11 best practice enforcement https://developer.android.com/training/location/permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!PermissionsUtils.hasAccessBackgroundLocation(this) && currentPermissionDialog == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getResources().getString(R.string.alert_permission_denied_title));
                builder.setMessage(getResources().getString(R.string.alert_need_background_location));
                builder.setPositiveButton("OK", (dialog, id) -> {
                    dialog.dismiss();
                    currentPermissionDialog = null;
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, ACCESS_BACKGROUND_LOCATION_REQUEST_CODE);
                });
                builder.setOnCancelListener(dialog -> {
                    currentPermissionDialog = null;
                    checkPermissions();
                });
                currentPermissionDialog = builder.show();
                return false;
            }
        }
        return true;
    }

    protected void enableWifiSuggestions() {
        // addNetwork starting Android 10 is deprecated instead we need to use addNetworkSuggestions in order to connect to desired WiFi
        // https://developer.android.com/reference/android/net/wifi/WifiManager#addNetwork(android.net.wifi.WifiConfiguration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            WifiNetworkSuggestion wifiNetworkSuggestion = new WifiNetworkSuggestion
                    .Builder()
                    .setSsid(Objects.requireNonNull(sharedPreferences.getString("headunitWifiSsid", getString(R.string.headunitWifiSsid_default_value))))
                    .setWpa2Passphrase(Objects.requireNonNull(sharedPreferences.getString("headunitWifiWpa2Passphrase", getString(R.string.headunitWifiWpa2Passphrase_default_value))))
                    .setIsAppInteractionRequired(true)
                    .build();
            ArrayList<WifiNetworkSuggestion> wifiSuggestionsList = new ArrayList<>();
            wifiSuggestionsList.add(wifiNetworkSuggestion);
            wifiManager.addNetworkSuggestions(wifiSuggestionsList);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        if (grantResults.length > 0) {
            switch (requestCode) {
                case ACCESS_FINE_LOCATION_REQUEST_CODE:
                    requestedAccessFineLocation = true;
                    break;
                case ACCESS_BACKGROUND_LOCATION_REQUEST_CODE:
                    if (IntStream.of(grantResults).anyMatch(p -> p == -1) && currentPermissionDialog == null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle(getResources().getString(R.string.alert_permission_denied_title));
                        builder.setMessage(getResources().getString(R.string.alert_permission_location_denied_description));
                        builder.setPositiveButton("Yes", (dialog, id) -> {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                            currentPermissionDialog = null;
                            dialog.dismiss();
                        });
                        builder.setNegativeButton("No", (dialog, id) -> {
                            currentPermissionDialog = null;
                            checkPermissions();
                            dialog.dismiss();
                        });
                        builder.setOnCancelListener(dialog -> {
                            currentPermissionDialog = null;
                            checkPermissions();
                        });
                        currentPermissionDialog = builder.show();
                    } else {
                        currentPermissionDialog = null;
                    }
            }
        }
    }
}
