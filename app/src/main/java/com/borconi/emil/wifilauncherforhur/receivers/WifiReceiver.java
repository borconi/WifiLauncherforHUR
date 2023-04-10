package com.borconi.emil.wifilauncherforhur.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
;

import com.borconi.emil.wifilauncherforhur.services.WifiService;

/**
 * BroadcastReceiver to handle our explicit intents from notification actions
 */
public class WifiReceiver extends BroadcastReceiver {
    public static final String ACTION_WIFI_LAUNCHER_EXIT = "com.borconi.emil.wifilauncherforhur.action.EXIT";
    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction() == null)
            return;

        switch (intent.getAction()) {
            case ACTION_WIFI_LAUNCHER_EXIT:
                Intent startWifiServiceIntent = new Intent(context, WifiService.class);
                context.stopService(startWifiServiceIntent);
                break;
        }
    }
}
