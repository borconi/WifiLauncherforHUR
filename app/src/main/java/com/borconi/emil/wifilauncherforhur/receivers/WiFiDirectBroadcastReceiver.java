package com.borconi.emil.wifilauncherforhur.receivers;

import static android.net.wifi.p2p.WifiP2pManager.EXTRA_WIFI_P2P_INFO;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.borconi.emil.wifilauncherforhur.connectivity.Connector;
import com.borconi.emil.wifilauncherforhur.connectivity.WiFiP2PConnector;
import com.borconi.emil.wifilauncherforhur.services.WifiService;

@SuppressLint("MissingPermission")
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver implements WifiP2pManager.ConnectionInfoListener {

    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final Context mService;
    private final Connector connector;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, Context wifiService, WiFiP2PConnector wiFiP2PConnector) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.mService = wifiService;
        this.connector = wiFiP2PConnector;

        Log.d("WifiP2P", "Receiver initialized in strictly passive mode.");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            WifiP2pInfo p2pInfo = intent.getParcelableExtra(EXTRA_WIFI_P2P_INFO);

            if (manager != null && p2pInfo != null && p2pInfo.groupFormed) {
                manager.requestConnectionInfo(channel, this);
            }
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        Log.d("WifiP2P", "Connection info available: " + wifiP2pInfo);

        if (wifiP2pInfo == null || !wifiP2pInfo.groupFormed) {
            return;
        }

        // Failsafe: Since the head unit is forcing intent=15, we should never be the Group Owner.
        // If Android glitched and made us the GO anyway, tear it down immediately so the head unit retries.
        if (wifiP2pInfo.isGroupOwner) {
            Log.e("WifiP2P", "CRITICAL: We became the Group Owner unexpectedly! Tearing down group to force role reversal.");
            manager.removeGroup(channel, null);
            return;
        }

        if (wifiP2pInfo.groupOwnerAddress == null) {
            Log.e("WifiP2P", "Group is formed, but Group Owner IP is null!");
            return;
        }

        String groupOwnerAddress = wifiP2pInfo.groupOwnerAddress.getHostAddress();
        Log.d("WifiP2P", "We are the client. Peer (Group Owner) IP: " + groupOwnerAddress);

        if (mService instanceof WifiService) {
            ((WifiService) mService).startSocketProxy(groupOwnerAddress);
        }

    }
}