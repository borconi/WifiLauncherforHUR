package com.borconi.emil.wifilauncherforhur;

import android.app.UiModeManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.content.res.Configuration;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Set;

import uk.co.emil.borconi.wifip2pforhur.WifiDirectReceiver;


public class BTReceiver extends BroadcastReceiver {

    static public int netid;
    @Override
    public void onReceive(final Context context, Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);


        Set<String> aux = prefs.getStringSet("mac", null);
        if (aux==null)
            return;


        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction()) && aux.contains(device.getAddress())) {
            Log.d("BT-RECEIVER", intent.getAction()+" want: "+aux+", got: "+ device.getAddress());

           if (WifiListener.isConnected)
               return;

            if (((UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE)).getCurrentModeType() == Configuration.UI_MODE_TYPE_CAR)
                return;

            Intent intent1=new Intent(context,WifiListener.class);
            intent1.putExtra("btdev",device);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent1);
            }
            else
                context.startService(intent1);
        }
        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())  && aux.contains(device.getAddress())) {
            Log.d("BT-RECEIVER","BT Disconnected");

            if (!androidx.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean("stoponBT",false))
                return;

            Log.d("BT-RECEIVER","We should exit the listener");
            Intent intent1=new Intent(context,WifiListener.class);
            context.stopService(intent1);

     //       if (!PreferenceManager.getDefaultSharedPreferences(context).getString("connectto","Headunit Reloaded").equalsIgnoreCase("Headunit Reloaded"))
     //           return; //If we are connected to a AAGW, don't turn off the WiFi and don't exit AA!



            UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
            assert uiModeManager != null;
            uiModeManager.disableCarMode(UiModeManager.DISABLE_CAR_MODE_GO_HOME);
            WifiListener.isConnected=false;


            WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            assert wifi != null;

            if (wifi.getConnectionInfo().getSSID().contains("HUR"))
                wifi.disconnect();
        }
        if ("android.net.wifi.STATE_CHANGE".equals(intent.getAction())) {

            NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
            if (NetworkInfo.State.CONNECTED.equals(networkInfo.getState())) {
                final WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                assert wifiManager != null;
                if (wifiManager.getConnectionInfo().getSSID().contains("HUR")) {
                    Log.d("WiFi-Launcher","Connected to HUR Wifi, start projection");
                    (new Thread(new Runnable() {
                        public void run() {
                            while(wifiManager.getConnectionInfo().getIpAddress() == 0) {
                                try {
                                    Thread.sleep(1000L);
                                } catch (InterruptedException var2) {
                                    var2.printStackTrace();
                                }
                            }
                            connectToHur(context);

                        }
                    })).start();
                }
                WifiListener.isConnected=false;
            }
            WifiListener.isConnected=false;

        }

        if ("android.app.action.EXIT_CAR_MODE".equals(intent.getAction()))
        {
            Log.d("Wifi-Listener","Exit car mode.");
            WifiListener.isConnected=false;
        }
        }


public static void connectToHur(Context context)
{

    WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    if (WifiListener.isConnected)
        return;
    int addr=wifiManager.getDhcpInfo().gateway;
    Intent i = new Intent();
    i.setClassName("com.google.android.projection.gearhead", "com.google.android.apps.auto.wireless.setup.service.impl.WirelessStartupActivity");
    i.addFlags(268435456);
    i.putExtra("PARAM_HOST_ADDRESS", (addr & 255) + "." + ((addr >>>= 8) & 255) + "." + ((addr >>>= 8) & 255) + "." + ((addr >>>= 8) & 255)).putExtra("PARAM_SERVICE_PORT", 5288);
    context.getApplicationContext().startActivity(i);
    WifiListener.isConnected=true;
}



}
