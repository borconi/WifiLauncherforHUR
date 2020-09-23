package com.borconi.emil.wifilauncherforhur.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

public class PermissionsUtils {
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
}
