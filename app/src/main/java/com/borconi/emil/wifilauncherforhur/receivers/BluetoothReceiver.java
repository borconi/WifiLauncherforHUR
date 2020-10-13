package com.borconi.emil.wifilauncherforhur.receivers;

import android.app.UiModeManager;
import android.bluetooth.BluetoothAdapter;
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
import com.borconi.emil.wifilauncherforhur.utils.BluetoothUtils;

import java.util.Objects;
import java.util.Set;

import static android.content.Intent.ACTION_BOOT_COMPLETED;

/**
 * BroadcastReceiver to catch when a Bluetooth Device is connected or disconnected
 */
public class BluetoothReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        Set<String> selectedBluetoothMacs = sharedPreferences.getStringSet("selected_bluetooth_devices", null);

        if (selectedBluetoothMacs == null || intent.getAction() == null) {
            return;
        }

        // Check on boot if we are connected to a selected device. Checking on boot because it depends on device CPU, bluetooth intents
        // can take up to 2-4 mins after boot to arrive on this receiver.
        if (intent.getAction().equals(ACTION_BOOT_COMPLETED)) {
            Log.i("BluetoothReceiver", "BOOT!!!");
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                selectedBluetoothMacs.forEach(selectedBluetoothAddress -> {
                    try {
                        BluetoothDevice bluetoothDevice = adapter.getRemoteDevice(selectedBluetoothAddress);
                        if (BluetoothUtils.isConnected(bluetoothDevice)) {
                            startWifiService(context, uiModeManager);
                        }
                    } catch (IllegalArgumentException exception) {
                        Log.e("BluetoothReceiver", "Boot: IllegalArgumentException while trying to get bt address: " + selectedBluetoothAddress, exception);
                    }
                });
            }
        }

        if (device == null) {
            return;
        }

        if (selectedBluetoothMacs.contains(device.getAddress())) {
            switch (intent.getAction()) {
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    Log.d("BluetoothReceiver", intent.getAction() + " want: " + selectedBluetoothMacs + ", got: " + device.getAddress());

                    startWifiService(context, uiModeManager);
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    if (sharedPreferences.getBoolean("stopServiceIfBluetoothDisconnects", false)) {
                        if (WifiService.isRunning()) {
                            Log.d("BluetoothReceiver", "We should exit wifi service");
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
                    break;
            }
        }
    }

    protected void startWifiService(Context context, UiModeManager uiModeManager) {
        if (WifiService.isRunning())
            return;

        if (WifiService.isConnected()) {
            if (uiModeManager != null && uiModeManager.getCurrentModeType() != Configuration.UI_MODE_TYPE_CAR) {
                // We should start service again, isConnected is outdated for an unknown reason.
                Log.d("BluetoothReceiver", "WifiService.isConnected is true but we are not in car mode. This is not good. We are going to restart wifi service");
                WifiService.setIsConnected(false);
            } else { // WifiService is connected and phone in car mode.
                return;
            }
        }

        Intent startWifiServiceIntent = new Intent(context, WifiService.class);
        context.startForegroundService(startWifiServiceIntent);
    }
}
