package com.borconi.emil.wifilauncherforhur;

import android.app.UiModeManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Set;


public class BTReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);


        Set<String> aux = prefs.getStringSet("mac", null);
        Log.d("BT-RECEIVER", intent.getAction()+" want: "+aux+", got: "+ device.getAddress());

        if ((intent.getAction().equalsIgnoreCase("android.bluetooth.device.action.ACL_CONNECTED") && aux.contains(device.getAddress()))) {


           // WifiListener.isConnected=true;

            if (((UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE)).getCurrentModeType() == Configuration.UI_MODE_TYPE_CAR)
                return;
            Intent intent1=new Intent(context,WifiListener.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent1);
            }
            else
                context.startService(intent1);
        }
        if (intent.getAction().equalsIgnoreCase("android.bluetooth.device.action.ACL_DISCONNECTED")  && aux.contains(device.getAddress())) {
            Log.d("BT-RECEIVER","BT Disconnected");
            if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("stopbt",true))
                return;

            UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
            uiModeManager.disableCarMode(UiModeManager.DISABLE_CAR_MODE_GO_HOME);
          //  WifiListener.isConnected=false;
            Intent intent1=new Intent(context,WifiListener.class);
            context.stopService(intent1);
        }
        }






}
