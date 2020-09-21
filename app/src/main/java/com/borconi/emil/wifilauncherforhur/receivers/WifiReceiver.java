package com.borconi.emil.wifilauncherforhur.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static com.borconi.emil.wifilauncherforhur.receivers.WifiLocalReceiver.ACTION_WIFI_LAUNCHER_EXIT;
import static com.borconi.emil.wifilauncherforhur.receivers.WifiLocalReceiver.ACTION_WIFI_LAUNCHER_FORCE_CONNECT;

/**
 * BroadcastReceiver to handle our explicit intents from notification actions
 */
public class WifiReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        // We need this ReBroadcaster to receive explicit intents and re send it using LocalBroadcastManager
        // so our Dynamic BroadcastReceiver can receive it
        if (intent.getAction() == null)
            return;

        switch (intent.getAction()) {
            case ACTION_WIFI_LAUNCHER_FORCE_CONNECT:
            case ACTION_WIFI_LAUNCHER_EXIT:
                Log.d("WifiReceiverRebroadcast", "Re sending intent to LocalBroadcastReceivers");
                LocalBroadcastManager manager = LocalBroadcastManager.getInstance(context);
                Intent modifiedIntent = new Intent(intent);
                modifiedIntent.setComponent(null);
                manager.sendBroadcast(modifiedIntent);
                break;
        }
    }
}
