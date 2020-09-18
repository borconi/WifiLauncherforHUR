package com.borconi.emil.wifilauncherforhur.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;

import android.os.Build;
import android.os.Bundle;


import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.borconi.emil.wifilauncherforhur.R;
import com.borconi.emil.wifilauncherforhur.listeners.WifiListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();

        Set<String> enabledapps = NotificationManagerCompat.getEnabledListenerPackages(this);
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

//        boolean havepermission = false;
//        for (String currapp : enabledapps) {
//            if (currapp.equalsIgnoreCase("com.borconi.emil.wifilauncherforhur"))
//                havepermission = true;
//        }

//        if (!havepermission && !sp.getBoolean("ignore_notification",false)) {
//            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//            builder.setTitle(getResources().getString(R.string.perm_req));
//            builder.setMessage(getResources().getString(R.string.perm_desc));
//            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int id) {
//                    startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
//                }
//            });
//            builder.setNegativeButton(getString(R.string.notnow), new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int id) {
//                    checkOverlays();
//                    dialog.dismiss();
//                }
//            });
//            builder.setNeutralButton(getString(R.string.ignore),new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int id) {
//                    sp.edit().putBoolean("ignore_notification",true).commit();
//                    dialog.dismiss();
//                }
//            });
//            builder.setOnCancelListener(dialog -> {
//                dialog.dismiss();
//                checkOverlays();
//            });
//            AlertDialog notification_dialog = builder.show();
//            return;
//        }

        if (checkOverlays()) {
            return;
        };

        checkPermissions();
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

    protected void checkPermissions() {
        boolean hasAccessFineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        List<String> permissions = new ArrayList<String>() {{
            add(Manifest.permission.ACCESS_FINE_LOCATION);
        }};
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }

        if (!hasAccessFineLocationPermission && !requestedAccessFineLocation) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), ACCESS_FINE_LOCATION_REQUEST_CODE);
            return;
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
                currentPermissionDialog = builder.show();
            }
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
                        currentPermissionDialog = builder.show();
                    } else {
                        currentPermissionDialog = null;
                    }
                    break;
            }
        }
    }
}
