package com.borconi.emil.wifilauncherforhur;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;


import java.io.InputStream;
import java.net.Socket;
import java.util.List;

import uk.co.emil.borconi.wifip2pforhur.Wifip2pService;
import uk.co.emil.borconi.wifip2pforhur.HeadunitServerToggle;
import static android.app.Notification.EXTRA_NOTIFICATION_ID;
import static uk.co.emil.borconi.wifip2pforhur.MyTether.startTether;

public class WifiListener extends Wifip2pService {


    private WifiReceiver mylistener=new WifiReceiver();
    private boolean isRunning=true;


    @Override
    public void onCreate(){
        super.onCreate();
        netId=-1;
        Intent snoozeIntent = new Intent(this, WifiReceiver.class);
        snoozeIntent.setAction("com.borconi.emil.wifilauncherforhur.exit");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            snoozeIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        }
        PendingIntent snoozePendingIntent =
                PendingIntent.getBroadcast(this, 0, snoozeIntent, 0);

         mynotification.setContentIntent(snoozePendingIntent)
                .addAction(R.drawable.ic_exit_to_app_24px,"Exit",snoozePendingIntent);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.getBoolean("startserver",false) && Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            HeadunitServerToggle.StartStop(false);
        if (sp.getBoolean("tether",false)&& Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            startTether(this,true);

        startForeground(1, mynotification.build());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !sp.getBoolean("wifidirect",false))
        {
            /* Only run this if we are on pie */
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            assert wifi != null;
            if (!wifi.isWifiEnabled())
                wifi.setWifiEnabled(true);

            List<WifiConfiguration> x = wifi.getConfiguredNetworks();
            /*IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            intentFilter.addAction("android.net.wifi.STATE_CHANGE");
            registerReceiver(mylistener, intentFilter);*/

            for (WifiConfiguration a:x){
                if (a.SSID.startsWith("\"") && a.SSID.endsWith("\""))
                    a.SSID = a.SSID.substring(1, a.SSID.length() - 1);
                    if (a.SSID.equalsIgnoreCase("HUR"))
                    {
                        netId=a.networkId;
                        break;
                    }

            }
            if (netId<=0)
            {
                Log.d("HU-Wifi","HUR wifi not in the list, add it and try to connect");
                WifiConfiguration wifiConfig = new WifiConfiguration();
                wifiConfig.SSID = String.format("\"%s\"", "HUR");
                wifiConfig.preSharedKey = String.format("\"%s\"", "AndroidAutoConnect");
                netId = wifi.addNetwork(wifiConfig);
                //wifi.startScan();
            }
            BTReceiver.netid=netId;
            if (wifi.getConnectionInfo().getNetworkId()!=netId) {
                Log.d("HU-Wifi","Start up, not connected to HUR network, is it in range?");

                //Log.d("HU-Wifi","Start scan is: "+wifi.startScan());
                wifi.disconnect();
                wifi.enableNetwork(netId, true);
                wifi.reconnect();
            }
            else
                connectToHur(wifi.getDhcpInfo().gateway);

        }
        else if (sp.getBoolean("wifidirect",false))
        {
            startP2P();
            setDeviceName(AA_NAME);
            mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    // Code for when the discovery initiation is successful goes here.
                    // No services have actually been discovered yet, so this method
                    // can often be left blank. Code for peer discovery goes in the
                    // onReceive method, detailed below.
                }

                @Override
                public void onFailure(int reasonCode) {
                    // Code for when the discovery initiation fails goes here.
                    // Alert the user that something went wrong.
                }
            });
        }



    }

    @Override
    public void connectToHur(String address){
        super.connectToHur(address);
        stopSelf();
    }

    @Override
    public void startSlave(final WifiP2pInfo info, final WifiP2pGroup wifiP2pGroup){
        if (hur_address!=null)
        {
            return;
        }
        hur_address=info.groupOwnerAddress;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("HU-Wifip2p", "Trying to connect to: " + info.groupOwnerAddress);
                    Socket sock = new Socket(info.groupOwnerAddress, 5299);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                        Log.d("HU-Wifip2p", "Connected");
                        sock.close();
                    }
                    else {
                    InputStream inpstr = sock.getInputStream();
                    while (inpstr.available()<=0)
                    {
                        Thread.sleep(10);
                    }
                    byte[] pass=new byte[inpstr.available()];
                    inpstr.read(pass);
                    sock.close();
                    String password=new String(pass);
                    Log.d("HU-Wifip2p","Password from socket: " + password);
                    WifiConfiguration wifiConfig = new WifiConfiguration();
                    wifiConfig.SSID = String.format("\"%s\"", wifiP2pGroup.getNetworkName());
                    wifiConfig.preSharedKey = String.format("\"%s\"", password);
                    mManager.removeGroup(mChannel,null);
                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    assert wifiManager != null;
                    netId = wifiManager.addNetwork(wifiConfig);
                    mManager.removeGroup(mChannel,null);
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(netId, true);
                    wifiManager.reconnect();
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.app.action.EXIT_CAR_MODE");
        registerReceiver(mylistener, intentFilter);
        return START_STICKY;

    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        isRunning=false;
        try {
            unregisterReceiver(mylistener);
        }
        catch (Exception e){}
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.getBoolean("startserver",false) && Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            HeadunitServerToggle.StartStop(true);
        if (sp.getBoolean("tether",false) && Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            startTether(this,false);
        if (sp.getBoolean("wifidirect",false))
            try {
                mManager.removeGroup(mChannel,null);
                }
            catch (Exception e){}
        android.os.Process.killProcess(android.os.Process.myPid());
    }


    public class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("HU","REceiver: " + intent.getAction());
            if (intent.getAction()==null)
                return;
            if (intent.getAction().equalsIgnoreCase("com.borconi.emil.wifilauncherforhur.exit"))
            {
                stopSelf();
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
                stopSelf();
                return;
            }
        }
    }



}
