package com.borconi.emil.wifilauncherforhur.receivers;

import android.app.UiModeManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.borconi.emil.wifilauncherforhur.R;
import com.borconi.emil.wifilauncherforhur.services.WifiService;

import java.util.Objects;
import java.util.Set;

/**
 * BroadcastReceiver to catch when a Bluetooth Device is connected or disconnected
 */
public class BluetoothReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (device == null || intent.getAction() == null) {
            return;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Set<String> selectedBluetoothMacs = sharedPreferences.getStringSet("selected_bluetooth_devices", null);
        if (selectedBluetoothMacs == null)
            return;

        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);

        if (selectedBluetoothMacs.contains(device.getAddress())) {
            switch (intent.getAction()) {
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    Log.d("Bluetooth Receiver", intent.getAction() + " want: " + selectedBluetoothMacs + ", got: " + device.getAddress());

                    if (WifiService.isRunning() || WifiService.isConnected())
                        return;

                    if (uiModeManager != null && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_CAR)
                        return;

                    Intent startWifiServiceIntent = new Intent(context, WifiService.class);
                    context.startForegroundService(startWifiServiceIntent);
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    if (sharedPreferences.getBoolean("stopServiceIfBluetoothDisconnects", false)) {
                        if (WifiService.isRunning()) {
                            Log.d("BT-RECEIVER", "We should exit wifi service");
                            Intent stopWifiServiceIntent = new Intent(context, WifiService.class);
                            context.stopService(stopWifiServiceIntent);
                        }

                        if (uiModeManager != null) {
                            uiModeManager.disableCarMode(UiModeManager.DISABLE_CAR_MODE_GO_HOME);
                        }

                        // Try to disconnect wifi
                        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        if (wifi != null && wifi.getConnectionInfo().getSSID().contains(Objects.requireNonNull(sharedPreferences.getString("headunitWifiSsid", context.getString(R.string.headunitWifiSsid_default_value)))))
                            wifi.disconnect();
                    }
            }
        }
    }
}
