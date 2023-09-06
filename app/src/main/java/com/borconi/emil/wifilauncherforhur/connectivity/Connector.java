package com.borconi.emil.wifilauncherforhur.connectivity;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static com.borconi.emil.wifilauncherforhur.services.WifiService.NOTIFICATION_ID;
import static org.mockito.Mockito.withSettings;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.os.Parcel;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.borconi.emil.wifilauncherforhur.R;
import com.borconi.emil.wifilauncherforhur.services.WifiService;

import org.mockito.Mockito;

public class Connector {

    NotificationManager notificationManager;
    NotificationCompat.Builder notification;
    Context context;

    private static final String PACKAGE_NAME_ANDROID_AUTO_WIRELESS = "com.google.android.projection.gearhead";
    private static final String CLASS_NAME_ANDROID_AUTO_WIRELESS = "com.google.android.apps.auto.wireless.setup.service.impl.WirelessStartupActivity";

    private static final String PARAM_HOST_ADDRESS_EXTRA_NAME = "PARAM_HOST_ADDRESS";
    private static final String PARAM_SERVICE_PORT_EXTRA_NAME = "PARAM_SERVICE_PORT";

    public Connector(NotificationManager notificationManager, NotificationCompat.Builder notification, Context context) {
        this.notificationManager = notificationManager;
        this.notification = notification;
        this.context = context;

    }

    public Intent getAAIntent(String ip, boolean checkWifi) {

            Network fakeNetwork = null;
            try {
                fakeNetwork = Mockito.mock(Network.class, withSettings().useConstructor(9999));
            } catch (Exception e) {
            }

            if (fakeNetwork != null) {
                Parcel p = Parcel.obtain();
                fakeNetwork.writeToParcel(p, 0);
                p.setDataPosition(0);
                WifiService.ns = Network.CREATOR.createFromParcel(p);
                Log.d("FakeNetwork", "Network is: " + WifiService.ns);
            } else {
                ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                WifiService.ns = connectivity.getActiveNetwork();
            }

            if (!checkWifi){
                ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                WifiService.ns = connectivity.getActiveNetwork();
            }

            if (WifiService.ns == null) {
                notification.setContentText(context.getString(R.string.no_network));
                notificationManager.notify(NOTIFICATION_ID, notification.build());
                return null;
            }

        Intent androidAutoWirelessIntent = new Intent();
        WifiInfo wifiinfo = null;
        try {
            Class<?> cl = Class.forName("android.net.wifi.WifiInfo");
            wifiinfo = (WifiInfo) cl.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        androidAutoWirelessIntent.setClassName(PACKAGE_NAME_ANDROID_AUTO_WIRELESS, CLASS_NAME_ANDROID_AUTO_WIRELESS);
        androidAutoWirelessIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        androidAutoWirelessIntent
                .putExtra(PARAM_HOST_ADDRESS_EXTRA_NAME, ip)
                .putExtra(PARAM_SERVICE_PORT_EXTRA_NAME, 5288)
                .putExtra("wifi_info", wifiinfo)
                .putExtra("PARAM_SERVICE_WIFI_NETWORK", WifiService.ns);

        return androidAutoWirelessIntent;
    }

    public Boolean isWiFiConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        Network nw = connectivityManager.getActiveNetwork();
        if (nw == null) return false;
        NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
        return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));

    }

    public void startAA(String hostIpAddress, boolean checkWifi){
        if (checkWifi && isWiFiConnected())
        {
            notification.setContentText(context.getString(R.string.still_connected_to_wifi));
            notificationManager.notify(NOTIFICATION_ID, notification.build());
            ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if (!WifiService.connected.get())
                        context.startActivity(getAAIntent(hostIpAddress,checkWifi));
                    ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
                    connectivityManager.unregisterNetworkCallback(this);
                }

            };
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build();
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        }
        else
        {
            if (!WifiService.connected.get())
                context.startActivity(getAAIntent(hostIpAddress,checkWifi));
        }
    }

    public void stop() {
    }
}
