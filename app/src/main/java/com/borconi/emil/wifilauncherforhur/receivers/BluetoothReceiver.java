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
import static com.borconi.emil.wifilauncherforhur.tethering.MyTether.startTether;

/**
 * BroadcastReceiver to catch when a Bluetooth Device is connected or disconnected
 */
public class BluetoothReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        Set<String> selectedBluetoothMacs = sharedPreferences.getStringSet("selected_bluetooth_devices", null);

        if (selectedBluetoothMacs == null || intent.getAction() == null) {
            return;
        }


        if (device == null) {
            return;
        }

        if (selectedBluetoothMacs.contains(device.getAddress())) {
            switch (intent.getAction()) {

                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    Log.d("BluetoothReceiver", intent.getAction() + " want: " + selectedBluetoothMacs + ", got: " + device.getAddress());

                    startWifiService(context);
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:

                        if (WifiService.isRunning()) {
                            Log.d("BluetoothReceiver", "We should exit wifi service");
                            Intent stopWifiServiceIntent = new Intent(context, WifiService.class);
                            context.stopService(stopWifiServiceIntent);
                        }

                    break;
            }
        }
    }

    protected void startWifiService(Context context) {
        if (WifiService.isRunning())
            return;
        Intent startWifiServiceIntent = new Intent(context, WifiService.class);
        context.startForegroundService(startWifiServiceIntent);

    }
}
