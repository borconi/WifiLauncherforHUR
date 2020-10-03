package com.borconi.emil.wifilauncherforhur.utils;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

public class PermissionsUtils {

    private static final String CHANGE_WIFI_STATE = "android:change_wifi_state";

    public enum PermissionsMissing {
        DRAW_OVERLAYS,
        ACCESS_FINE_LOCATION,
        ACCESS_BACKGROUND_LOCATION,
        WIFI_CONTROL,
        NONE
    }

    public static boolean hasDrawOverlays(Context context) {
        return Settings.canDrawOverlays(context);
    }

    public static boolean hasAccessFineLocation(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static boolean hasAccessBackgroundLocation(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static boolean hasWiFiControl(Context context) {
        // Check if Wi-Fi Control is enabled: https://issuetracker.google.com/issues/169471814
        AppOpsManager appOpsManager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        // This will return true if WifiSuggestions Alert Dialog is opened.
        return appOpsManager.unsafeCheckOp(CHANGE_WIFI_STATE, android.os.Process.myUid(), context.getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    public static PermissionsMissing checkAllPermissions(Context context) {
        if (!hasDrawOverlays(context)) {
            return PermissionsMissing.DRAW_OVERLAYS;
        } else if (!hasAccessFineLocation(context)) {
            return PermissionsMissing.ACCESS_FINE_LOCATION;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!hasAccessBackgroundLocation(context)) {
                return PermissionsMissing.ACCESS_BACKGROUND_LOCATION;
            } else if (!hasWiFiControl(context)) {
                return PermissionsMissing.WIFI_CONTROL;
            }
        }
        return PermissionsMissing.NONE;
    }
}
