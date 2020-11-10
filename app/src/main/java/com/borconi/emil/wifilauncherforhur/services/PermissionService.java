package com.borconi.emil.wifilauncherforhur.services;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.borconi.emil.wifilauncherforhur.R;
import com.borconi.emil.wifilauncherforhur.utils.DeviceDetectionUtils;
import com.borconi.emil.wifilauncherforhur.utils.PermissionsUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PermissionService {

    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 12345;
    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 200;
    private static final int ACCESS_BACKGROUND_LOCATION_REQUEST_CODE = 210;

    private static final int OPEN_SETTINGS_MIUI_SPECIAL_ACCESS_REQUEST_CODE = 400;

    private final Activity activity;
    private final SharedPreferences sharedPreferences;
    private boolean alertDialogOpen = false;

    public PermissionService(Activity activity) {
        this.activity = activity;
        sharedPreferences = activity.getSharedPreferences("permissions_service_shared_prefs", Context.MODE_PRIVATE);
    }

    public boolean requestDrawOverlays(boolean showRationaleMessage) {
        boolean requestPermission = !sharedPreferences.getBoolean("drawOverlaysRequested", false) || showRationaleMessage;

        if (!PermissionsUtils.hasDrawOverlays(activity) && requestPermission) {
            if (!alertDialogOpen) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(activity.getResources().getString(R.string.alert_permission_denied_title));
                builder.setMessage(activity.getResources().getString(R.string.alert_need_draw_over_other_apps));
                builder.setPositiveButton("OK", (dialog, id) -> {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + activity.getPackageName()));
                    sharedPreferences.edit().putBoolean("drawOverlaysRequested", true).apply();
                    activity.startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
                    dialog.dismiss();
                });
                builder.setOnDismissListener(dialog -> alertDialogOpen = false);
                alertDialogOpen = true;
                builder.show();
            }
            return false;
        }
        return true;
    }

    public boolean requestLocationServices(boolean showRationaleMessage) {
        List<String> permissions = new ArrayList<String>() {{
            add(Manifest.permission.ACCESS_FINE_LOCATION);
        }};
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        boolean requestPermission = !sharedPreferences.getBoolean("accessFineLocationRequested", false) || showRationaleMessage;
        if (!PermissionsUtils.hasAccessFineLocation(activity) && requestPermission) {
            sharedPreferences.edit().putBoolean("accessFineLocationRequested", true).apply();
            ActivityCompat.requestPermissions(activity, permissions.toArray(new String[0]), ACCESS_FINE_LOCATION_REQUEST_CODE);
            return false;
        }

        // Incremental location permission due to Android 11 best practice enforcement https://developer.android.com/training/location/permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermission = !sharedPreferences.getBoolean("accessBackgroundLocationRequested", false) || showRationaleMessage;
            if (!PermissionsUtils.hasAccessBackgroundLocation(activity) && requestPermission) {
                if (!alertDialogOpen) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle(activity.getResources().getString(R.string.alert_permission_denied_title));
                    builder.setMessage(activity.getResources().getString(R.string.alert_need_background_location));
                    builder.setPositiveButton("OK", (dialog, id) -> {
                        dialog.dismiss();
                        sharedPreferences.edit().putBoolean("accessBackgroundLocationRequested", true).apply();
                        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, ACCESS_BACKGROUND_LOCATION_REQUEST_CODE);
                    });
                    builder.setOnDismissListener(dialog -> alertDialogOpen = false);
                    alertDialogOpen = true;
                    builder.show();
                }
                return false;
            }
        }
        return true;
    }

    public void checkAndSetWifiSuggestions(boolean showRationaleMessage) {
        // addNetwork starting Android 10 is deprecated instead we need to use addNetworkSuggestions in order to connect to desired WiFi
        // https://developer.android.com/reference/android/net/wifi/WifiManager#addNetwork(android.net.wifi.WifiConfiguration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (DeviceDetectionUtils.isMiUi(activity) && !PermissionsUtils.hasWiFiControl(activity)) {
                if (!sharedPreferences.getBoolean("miUiSpecialAppAccessRequested", false)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle(activity.getResources().getString(R.string.alert_important_title));
                    builder.setMessage(activity.getResources().getString(R.string.alert_miui_special_app_request_description));
                    builder.setPositiveButton("OK", (dialog, id) -> {
                        dialog.dismiss();
                        sharedPreferences.edit().putBoolean("miUiSpecialAppAccessRequested", true).apply();
                        activity.startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), OPEN_SETTINGS_MIUI_SPECIAL_ACCESS_REQUEST_CODE);
                    });
                    builder.setOnCancelListener(dialog -> {
                        sharedPreferences.edit().putBoolean("miUiSpecialAppAccessRequested", true).apply();
                        activity.startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), OPEN_SETTINGS_MIUI_SPECIAL_ACCESS_REQUEST_CODE);
                    });
                    builder.setOnDismissListener(dialog -> alertDialogOpen = false);
                    alertDialogOpen = true;
                    builder.show();
                    return;
                }
            }

            boolean wifiControlRequested = sharedPreferences.getBoolean("wifiControlRequested", false);
            boolean requestWifiControl = (!wifiControlRequested && showRationaleMessage) || (wifiControlRequested && showRationaleMessage);
            if (!PermissionsUtils.hasWiFiControl(activity) && requestWifiControl) {
                if (!alertDialogOpen) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle(activity.getResources().getString(R.string.alert_important_title));
                    builder.setMessage(DeviceDetectionUtils.isSamsung() ? R.string.alert_wifi_control_samsung_description : R.string.alert_wifi_control_description);
                    builder.setPositiveButton("OK", (dialog, id) -> {
                        dialog.dismiss();
                        activity.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                    });
                    builder.setOnDismissListener(dialog -> alertDialogOpen = false);
                    alertDialogOpen = true;
                    builder.show();
                }
                return;
            }

            setWiFiSuggestions();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public void setWiFiSuggestions() {
        WifiManager wifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        WifiNetworkSuggestion wifiNetworkSuggestion = new WifiNetworkSuggestion
                .Builder()
                .setSsid(Objects.requireNonNull(sharedPreferences.getString("settings_wireless_headunit_wifi_ssid", activity.getString(R.string.settings_wireless_headunit_wifi_ssid_default_value))))
                .setWpa2Passphrase(Objects.requireNonNull(sharedPreferences.getString("settings_wireless_headunit_wifi_wpa2_passphrase", activity.getString(R.string.settings_wireless_headunit_wifi_wpa2_passphrase_default_value))))
                .build();
        ArrayList<WifiNetworkSuggestion> wifiSuggestionsList = new ArrayList<>();
        wifiSuggestionsList.add(wifiNetworkSuggestion);
        wifiManager.addNetworkSuggestions(wifiSuggestionsList);
        sharedPreferences.edit().putBoolean("wifiControlRequested", true).apply();
    }

    public void requestAllPermissions() {
        requestAllPermissions(false);
    }

    public void requestAllPermissions(boolean showRationaleMessage) {
        if (!requestDrawOverlays(showRationaleMessage)) {
            return;
        }

        if (!requestLocationServices(showRationaleMessage)) {
            return;
        }

        checkAndSetWifiSuggestions(showRationaleMessage);
    }
}
