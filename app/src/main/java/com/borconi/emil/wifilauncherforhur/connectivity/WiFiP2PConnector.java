package com.borconi.emil.wifilauncherforhur.connectivity;




import android.app.NotificationManager;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;

import androidx.core.app.NotificationCompat;

import com.borconi.emil.wifilauncherforhur.receivers.WiFiDirectBroadcastReceiver;





public class WiFiP2PConnector extends Connector {

    private WiFiDirectBroadcastReceiver wifip2preceiver;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private final static String TAG = "WiFi-P2P";


    public WiFiP2PConnector(NotificationManager notificationManager, NotificationCompat.Builder notification, Context context) {
        super(notificationManager,notification,context);
        start();
    }

    public void start() {

        manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);

        if (manager!=null) {
            channel = manager.initialize(context, context.getMainLooper(), null);
            if (channel != null)
                startWifip2p(manager, channel);
        }
    }

    private void startWifip2p(WifiP2pManager manager, WifiP2pManager.Channel channel){
        wifip2preceiver = new WiFiDirectBroadcastReceiver(manager,channel, context,this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        context.registerReceiver(wifip2preceiver, intentFilter);
    }
    public void stop(){
        try {
            if (manager!=null)
            {
                manager.cancelConnect(channel,null);
                manager.removeGroup(channel,null);
                context.unregisterReceiver(wifip2preceiver);
            }
        }
        catch (Exception e){}
    }
}
