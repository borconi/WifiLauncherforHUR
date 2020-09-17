package com.borconi.emil.wifilauncherforhur;

import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.MacAddress;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import java.util.UUID;

import uk.co.emil.borconi.wifip2pforhur.HeadunitServerToggle;
import uk.co.emil.borconi.wifip2pforhur.Wifip2pService;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;
import static uk.co.emil.borconi.wifip2pforhur.MyTether.startTether;

public class WifiListener extends Wifip2pService {



    private WifiReceiver mylistener=new WifiReceiver();

    private ConnectivityManager.NetworkCallback callback;
    private NetworkRequest networkRequest;
    static public boolean isConnected=false;
    private ConnectivityManager.NetworkCallback networkCallback;


    @Override
    public void onCreate(){
        super.onCreate();
        netId=-1;

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.app.action.EXIT_CAR_MODE");
        registerReceiver(mylistener, intentFilter);

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


       /* if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && !sp.getBoolean("wifidirect",false)) {
            WifiNetworkSpecifier.Builder builder = new WifiNetworkSpecifier.Builder();
            builder.setSsid("HUR");
           // builder.setBssid(MacAddress.fromString("02:08:22:fa:a5:23"));
            //builder.setBssid(MacAddress.fromString("00:08:22:b6:0c:3d"));
            builder.setWpa2Passphrase("AndroidAutoConnect");

            WifiNetworkSpecifier wifiNetworkSpecifier = builder.build();
            NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
            networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED);

            networkRequestBuilder.setNetworkSpecifier(wifiNetworkSpecifier);

             networkRequest = networkRequestBuilder.build();
             Log.d("WiFi_Launcher","Network request: "+networkRequest);
            final ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                cm.requestNetwork(networkRequest, callback=new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        //Use this network object to Send request.
                        //eg - Using OkHttp library to create a service request
                        InetAddress server = cm.getLinkProperties(network).getDnsServers().get(0);
                        Log.d("WiFi-Launcher","Got the following address: "+server.getHostAddress());
                        connectToHur(server.getHostAddress());


                        super.onAvailable(network);
                    }
                });

            }
        }*/



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
    public void connectToHur(final String address){
        Log.d("WiFi Listenner","Calling super connecto to HUR");
        super.connectToHur(address);

        isConnected=true;

        final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        networkCallback=new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLost(Network network) {
                Log.d("Wifi Listener","Lost connection to HUR wifi, exiting the app");
                connectivityManager.unregisterNetworkCallback(this);
                stopSelf();
            }
        };
        connectivityManager.registerNetworkCallback(
                builder.build(),networkCallback
        );

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
    Log.d("WiFi-Launcher","Start service");
        return START_STICKY;

    }
    @Override
    public void onDestroy(){
        super.onDestroy();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.getBoolean("startserver",false) && Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            HeadunitServerToggle.StartStop(true);
        if (sp.getBoolean("tether",false) && Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            startTether(this,false);

        if (networkCallback!=null)
        {
            ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        //android.os.Process.killProcess(android.os.Process.myPid());
    }






}
