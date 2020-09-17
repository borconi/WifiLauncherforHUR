package com.borconi.emil.wifilauncherforhur;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class WifiReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("HU","REceiver: " + intent.getAction());
        if (intent.getAction()==null)
            return;
        if (intent.getAction().equalsIgnoreCase("com.borconi.emil.wifilauncherforhur.exit"))
        {
            context.stopService(new Intent(context,WifiListener.class));
            return;
        }
           /* if (intent.getAction()==WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                final WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                Log.d("HU-Wifi","Got a scan result");
                if (wifi.getConnectionInfo().getNetworkId()!=netId) {
                    Log.d("HU-Wifi","We are not connected to HUR wifi....");
                    List<ScanResult> results = wifi.getScanResults();
                    for (ScanResult a:results)
                    {
                        if (a.SSID.startsWith("\"") && a.SSID.endsWith("\""))
                            a.SSID = a.SSID.substring(1, a.SSID.length() - 1);
                        if (a.SSID.equalsIgnoreCase("HUR"))
                        {
                            Log.d("HU-Wifi","Found HUR Wifi in the list");
                            wifi.disconnect();
                            wifi.enableNetwork(netId, true);
                            wifi.reconnect();
                            return;
                        }
                    }
                    Log.d("HU-Wifi","HUR wifi not in the list, setting up a 30 sec scan delay");
                    new Handler().postDelayed(new Runnable() {
                                                  @Override
                                                  public void run() {
                                                      Log.d("HU-Wifi","Requesting scan");
                                                      wifi.startScan();
                                                  }
                                              },30000); //Pie is throlling one scan to 30 seconds.
                }
            }*/
        if (intent.getAction().equalsIgnoreCase("android.app.action.EXIT_CAR_MODE"))
        {
            WifiListener.isConnected=false;
            context.stopService(new Intent(context,WifiListener.class));
            return;
        }
    }
}
