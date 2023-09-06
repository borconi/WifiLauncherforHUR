package com.borconi.emil.wifilauncherforhur.receivers;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.net.wifi.p2p.WifiP2pManager.EXTRA_NETWORK_INFO;
import static android.net.wifi.p2p.WifiP2pManager.EXTRA_WIFI_P2P_DEVICE;
import static android.net.wifi.p2p.WifiP2pManager.EXTRA_WIFI_STATE;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_ENABLED;
import static android.os.Looper.getMainLooper;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.borconi.emil.wifilauncherforhur.R;
import com.borconi.emil.wifilauncherforhur.connectivity.ConnectivityHelper;
import com.borconi.emil.wifilauncherforhur.connectivity.Connector;
import com.borconi.emil.wifilauncherforhur.connectivity.WiFiP2PConnector;
import com.borconi.emil.wifilauncherforhur.services.WifiService;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

@SuppressLint("MissingPermission")
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver implements WifiP2pManager.ActionListener, WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {


    private final String lookfor;

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private Context mService;
    private boolean looperStarted=false;
    private boolean hurfound=false;
    private Connector connector;
    private WifiP2pManager.DnsSdServiceResponseListener servListener;
    private Network network;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, Context wifiService, WiFiP2PConnector wiFiP2PConnector) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.mService=wifiService;
        this.connector=wiFiP2PConnector;
        lookfor=PreferenceManager.getDefaultSharedPreferences(mService).getString("hur_p2p_name","HUR7").toLowerCase();



        Log.d("WiFi-P2P","Look for Device with following name:"+lookfor);

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();



        Log.d("WifiP2P","Action is: "+action);
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            if (intent.getIntExtra(EXTRA_WIFI_STATE,WIFI_P2P_STATE_ENABLED)==WIFI_P2P_STATE_ENABLED)
            {
                Log.d("WifiP2P","Wifi p2p is enabled: "+intent);

                if (looperStarted)
                    return;
              startDiscovery();
                //Wifi P2P is enabled.
            }
            // Check to see if Wi-Fi is enabled and notify appropriate activity
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            if (manager != null) {
                if (hurfound)
                    return;
                manager.requestPeers(channel, this);
            }
            // Call WifiP2pManager.requestPeers() to get a list of current peers
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            manager.requestConnectionInfo(channel,this);
            // Respond to new connection or disconnections
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing

        }
    }


    private void startDiscovery() {
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("WifiP2P","P2P discovery started");
                forcePeerDiscoverRestartr();
            }

            @Override
            public void onFailure(int i) {
                Log.e("WifiP2P","P2P FAILED to start");
                final Handler handler = new Handler(getMainLooper());
                handler.postDelayed(() -> startDiscovery(), 2000);
            }
        });

    }
    @Override
    public void onSuccess() {

    }

    @Override
    public void onFailure(int i) {

    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {

        for (WifiP2pDevice device: wifiP2pDeviceList.getDeviceList()){

            Log.d("WifiP2P","Found device: "+device.deviceName);
            if (device.deviceName.toLowerCase().contains(lookfor))
            {
                hurfound=true;
                Log.d("WifiP2P","Connecting to: "+device);
                 WifiP2pConfig config = new WifiP2pConfig();
                 config.deviceAddress = device.deviceAddress;
                 config.groupOwnerIntent=0;
                 manager.connect(channel, config,WiFiDirectBroadcastReceiver.this);

            }
        }
    }

    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
// String from WifiP2pInfo struct
        Log.d("WiFiP2P","Connection info: "+wifiP2pInfo);
        if (!wifiP2pInfo.groupFormed)
            return;
        String groupOwnerAddress = wifiP2pInfo.groupOwnerAddress.getHostAddress();

        // After the group negotiation, we can determine the group owner
        // (server).
        if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {

        } else if (wifiP2pInfo.groupFormed) {
            connector.startAA(groupOwnerAddress,true);


        }
    }

    private void loopPeersDiscovery(){



       manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
           @Override
           public void onSuccess() {
               Log.d("WifiP2P","Discovery stopped");
               manager.discoverPeers(channel, null);

           }

           @Override
           public void onFailure(int i) {
               Log.e("WifiP2P","Discovery NOT stopped!");
           }
       });
    }

    private final void forcePeerDiscoverRestartr() {

        looperStarted=true;
        if (hurfound)
        {
            manager.stopPeerDiscovery(channel,null);
            return;
        }
        final Handler handler = new Handler(getMainLooper());
        loopPeersDiscovery();
            handler.postDelayed(() -> forcePeerDiscoverRestartr(), 10000);
    }
}