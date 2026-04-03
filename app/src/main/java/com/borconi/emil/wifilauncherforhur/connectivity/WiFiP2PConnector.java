package com.borconi.emil.wifilauncherforhur.connectivity;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;

import androidx.annotation.RequiresPermission;
import androidx.core.app.NotificationCompat;

import com.borconi.emil.wifilauncherforhur.receivers.WiFiDirectBroadcastReceiver;

public class WiFiP2PConnector extends Connector {

    private WiFiDirectBroadcastReceiver wifip2preceiver;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private final static String TAG = "WiFi-P2P";

    private Handler handler;
    private boolean shouldAdvertise = false;

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES})
    public WiFiP2PConnector(NotificationManager notificationManager, NotificationCompat.Builder notification, Context context) {
        super(notificationManager, notification, context);
        handler = new Handler(Looper.getMainLooper());
        start();
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES})
    public void start() {
        manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);

        if (manager != null) {
            channel = manager.initialize(context, context.getMainLooper(), null);
            if (channel != null) {
                startWifip2p(manager, channel);
            }
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES})
    private void startWifip2p(WifiP2pManager manager, WifiP2pManager.Channel channel) {

        manager.clearLocalServices(channel, null);
        WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("HUR", "_presence._tcp", null);
        manager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() { Log.d(TAG, "Successfully broadcasting HUR service."); }
            @Override
            public void onFailure(int reason) { Log.e(TAG, "Failed to broadcast HUR service: " + reason); }
        });

        wifip2preceiver = new WiFiDirectBroadcastReceiver(manager, channel, context, this);
        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        context.registerReceiver(wifip2preceiver, intentFilter);

        shouldAdvertise = true;
        startAdvertisingLoop();
    }

    private void startAdvertisingLoop() {
        if (!shouldAdvertise || manager == null || channel == null) return;

        manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES})
            @Override
            public void onSuccess() {
                if (shouldAdvertise) {
                    manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "P2P discovery (advertising) refreshed successfully");
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.e(TAG, "P2P discovery failed to start. Reason code: " + reason);
                        }
                    });
                }
            }

            @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES})
            @Override
            public void onFailure(int reason) {
                // Try to discover anyway just in case it wasn't running
                if (shouldAdvertise) {
                    manager.discoverPeers(channel, null);
                }
            }
        });

        // Loop every 10 seconds
        handler.postDelayed(this::startAdvertisingLoop, 10000);
    }

    public void stopAdvertising() {
        Log.d(TAG, "Stopping P2P advertising loop...");
        shouldAdvertise = false;

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        try {
            if (manager != null && channel != null) {
                manager.stopPeerDiscovery(channel, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping peer discovery", e);
        }
    }

    public void stop() {
        stopAdvertising();

        try {
            if (manager != null) {
                manager.cancelConnect(channel, null);
                manager.removeGroup(channel, null);
                context.unregisterReceiver(wifip2preceiver);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error shutting down WiFiP2PConnector", e);
        }
    }
}