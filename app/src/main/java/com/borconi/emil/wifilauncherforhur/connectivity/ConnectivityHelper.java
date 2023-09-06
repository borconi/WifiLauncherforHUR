package com.borconi.emil.wifilauncherforhur.connectivity;

import static android.content.Context.CONNECTIVITY_SERVICE;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.borconi.emil.wifilauncherforhur.services.WifiService;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

public class ConnectivityHelper extends Connector {


    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;
    private final static String TAG="ConnectivityHelper";

    public ConnectivityHelper(NotificationManager notificationManager, NotificationCompat.Builder notification, Context context) {
        super(notificationManager, notification, context);
        start();
    }


    public void start() {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    String host = getCurrentWifiGateway();
                    startAA(host,false);
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                }

                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities);

                }
            };
            connectivityManager=(ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build();
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    @Override
    public void stop(){
        try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        catch (Exception e){}
    }


    private String getCurrentWifiGateway() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            dhcpInfo.gateway = Integer.reverseBytes(dhcpInfo.gateway);
        }

        byte[] ipAddressBytes = {
                (byte) (dhcpInfo.gateway >>> 24),
                (byte) (dhcpInfo.gateway >>> 16),
                (byte) (dhcpInfo.gateway >>> 8),
                (byte) dhcpInfo.gateway
        };

        try {
            InetAddress gatewayAddress = InetAddress.getByAddress(ipAddressBytes);
           return gatewayAddress.getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }


    }
}
