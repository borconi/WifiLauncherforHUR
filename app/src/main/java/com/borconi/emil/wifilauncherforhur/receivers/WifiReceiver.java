package com.borconi.emil.wifilauncherforhur.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.borconi.emil.wifilauncherforhur.listeners.WifiListener;


public class WifiReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("HU","REceiver: " + intent.getAction());

        if (intent.getAction() == null)
            return;

        switch (intent.getAction()) {
            case "com.borconi.emil.wifilauncherforhur.exit":
            case "android.app.action.EXIT_CAR_MODE":
                WifiListener.isConnected = false;
                context.stopService(new Intent(context, WifiListener.class));
                break;
        }

    }
}
