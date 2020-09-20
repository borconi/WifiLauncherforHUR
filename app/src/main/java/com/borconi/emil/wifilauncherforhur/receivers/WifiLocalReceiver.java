package com.borconi.emil.wifilauncherforhur.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.borconi.emil.wifilauncherforhur.services.WifiService;

import static android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION;

/**
 * LocalBroadcastService to handle explicit intents from our notification
 *
 * We are receiving what WifiReceiver catches as explicit intents and it rebroadcasting to this one.
 * Also we are receiving from CarModeReceiver: NETWORK_STATE_CHANGED_ACTION.
 */
public class WifiLocalReceiver extends BroadcastReceiver {

    public static final String ACTION_WIFI_LAUNCHER_EXIT = "com.borconi.emil.wifilauncherforhur.action.EXIT";
    public static final String ACTION_WIFI_LAUNCHER_FORCE_CONNECT = "com.borconi.emil.wifilauncherforhur.action.FORCE_CONNECT";

    WifiService wifiService;

    public WifiLocalReceiver(WifiService wifiService) {
        this.wifiService = wifiService;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("HU","Receiver: " + intent.getAction());

        if (intent.getAction() == null)
            return;

        switch (intent.getAction()) {
            case NETWORK_STATE_CHANGED_ACTION:
                Log.d("WifiReceiver", "NETWORK_STATE_CHANGED_ACTION!");
                wifiService.tryToConnect();
                break;
            case ACTION_WIFI_LAUNCHER_FORCE_CONNECT:
                Log.d("WifiReceiver", "FORCE CONNECT!");
                WifiService.setIsConnected(false);
                wifiService.tryToConnect();
                break;
            case ACTION_WIFI_LAUNCHER_EXIT:
                Log.d("WifiReceiver", "TURN OFF ACTION BUTTON");
                WifiService.setIsConnected(false);
                context.stopService(new Intent(context, WifiService.class));
                break;
        }
    }
}
