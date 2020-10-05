package com.borconi.emil.wifilauncherforhur.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.borconi.emil.wifilauncherforhur.services.WifiService;

import static android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION;

/**
 * BroadcastReceiver to receive system notifications
 * <p>
 * This must be explicit registered in order to receive AA intents
 */
public class CarModeReceiver extends BroadcastReceiver {

    public static final String ACTION_ENTER_CAR_MODE = "android.app.action.ENTER_CAR_MODE";
    public static final String ACTION_EXIT_CAR_MODE = "android.app.action.EXIT_CAR_MODE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null)
            return;

        switch (intent.getAction()) {
            case NETWORK_STATE_CHANGED_ACTION:
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                // We only wants new Wi-Fi connections.
                if (info != null && info.isConnected()) {
                    Log.d("CarModeReceiver", "New Wi-Fi connected");
                    LocalBroadcastManager manager = LocalBroadcastManager.getInstance(context);
                    Intent modifiedIntent = new Intent(intent);
                    modifiedIntent.setComponent(null);
                    // Sending to our local broadcastreceiver (WifiReceiverLocal) because it has access to the WifiService.
                    // We will try to connect AA Wireless
                    manager.sendBroadcast(intent);
                }
                break;
            case ACTION_ENTER_CAR_MODE:
                Log.d("CarModeReceiver", "ENTER CAR MODE");
                WifiService.setIsConnected(true);
                // Do not stop Service here ever. Service should be alive all the time AA is connected.
                // User can turn it off from notification. But ideally we would like to setIsConnected(false) when AA is closed.
                break;
            case ACTION_EXIT_CAR_MODE:
                Log.d("CarModeReceiver", "EXIT CAR MODE STOP SERVICE");
                WifiService.setIsConnected(false);
                context.stopService(new Intent(context, WifiService.class));
                break;
        }
    }
}
