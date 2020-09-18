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
        if (intent.getAction()==null)
            return;
        if (intent.getAction().equalsIgnoreCase("com.borconi.emil.wifilauncherforhur.exit"))
        {
            context.stopService(new Intent(context, WifiListener.class));
            return;
        }

        if (intent.getAction().equalsIgnoreCase("android.app.action.EXIT_CAR_MODE"))
        {
            WifiListener.isConnected=false;
            context.stopService(new Intent(context,WifiListener.class));
            return;
        }
    }
}
