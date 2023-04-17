package com.borconi.emil.wifilauncherforhur.services;

import static androidx.core.app.NotificationCompat.EXTRA_NOTIFICATION_ID;
import static androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC;
import static com.borconi.emil.wifilauncherforhur.WiFiLauncherServiceWidget.WIDGET_ACTION;
import static com.borconi.emil.wifilauncherforhur.receivers.WifiReceiver.ACTION_WIFI_LAUNCHER_EXIT;
import static com.borconi.emil.wifilauncherforhur.tethering.MyTether.startTether;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.borconi.emil.wifilauncherforhur.R;
import com.borconi.emil.wifilauncherforhur.WiFiLauncherServiceWidget;
import com.borconi.emil.wifilauncherforhur.receivers.WifiReceiver;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Random;

public class WifiService extends Service {

    private static final int NOTIFICATION_ID = 1035;
    private static final int TURN_OFF_REQUEST_CODE = 1040;
     private static final String NOTIFICATION_CHANNEL_NO_VIBRATION_DEFAULT_ID = "wifilauncher_notification_channel_no_vibration_default";
    private static final String NOTIFICATION_CHANNEL_WITH_VIBRATION_IMPORTANT_ID = "wifilauncher_notification_channel_with_vibration_high";

    private static boolean isRunning = false;

    public static final String ACTION_FOREGROUND_STOP = "actionWifiServiceForegroundStop";


    private NotificationManager notificationManager;
    private static final String TAG = "NetworkServiceDiscovery";
    private static final String SERVICE_TYPE = "_aawireless._tcp.";
    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private static final String PACKAGE_NAME_ANDROID_AUTO_WIRELESS = "com.google.android.projection.gearhead";
    private static final String CLASS_NAME_ANDROID_AUTO_WIRELESS = "com.google.android.apps.auto.wireless.setup.service.impl.WirelessStartupActivity";

    private static final String PARAM_HOST_ADDRESS_EXTRA_NAME = "PARAM_HOST_ADDRESS";
    private static final String PARAM_SERVICE_PORT_EXTRA_NAME = "PARAM_SERVICE_PORT";
    private String hostIpAddress;
    private PendingIntent pendingIntent;
    private NotificationCompat.Builder notification;

    private boolean legacy;
    private ServerSocket legacylistener;

    private ConnectivityManager connectivityManager;

    public static boolean isRunning() {
        return isRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        connectivityManager=getSystemService(ConnectivityManager.class);
        Random r = new Random();
        websockport = r.nextInt(9998 - 8081) + 8081;

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        verifyOrCreateNotificationChannels();
        notification = getNotification(this, getString(R.string.service_wifi_looking_text));
        startForeground(NOTIFICATION_ID, notification.build());
        legacy=PreferenceManager.getDefaultSharedPreferences(this).getBoolean("legacy_mode",false);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("settings_wireless_headunit_wifi_using_router", false))
            startTether(this, true);
        if (!legacy) {
            initializeDiscoveryListener();
            mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        }
        else
        {
            if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("settings_wireless_headunit_wifi_using_router", false))
            {
                NetworkRequest networkRequest = new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build();
                connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
            }

            else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (legacylistener == null) {
                                legacylistener = new ServerSocket(5289);
                                legacylistener.setReuseAddress(true);
                            }
                            Socket s = legacylistener.accept();
                            Log.d(TAG,"Got someone on this port");
                            if (isRunning) {
                                hostIpAddress = (((InetSocketAddress) s.getRemoteSocketAddress()).getAddress()).toString().replace("/", "");
                                Log.d(TAG,"Should try to start AA indent with: "+hostIpAddress);
                                new hurToPhone(null).start();
                            }
                        } catch (Exception e) {
                           e.printStackTrace();
                        }
                    }
                }).start();
            }
        }
        isRunning=true;
    }


    private ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);

            Log.d(TAG,"Found a network: "+network);
            getCurrentWifiGateway();
            new hurToPhone(network).start();
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



    private Intent getAAIntent(){

        Network ns = connectivityManager.getActiveNetwork();


        Intent androidAutoWirelessIntent = new Intent();
        WifiInfo wifiinfo = null;
        try {
            Class<?> cl = Class.forName("android.net.wifi.WifiInfo");
            wifiinfo = (WifiInfo) cl.newInstance();
        }
        catch (Exception e){e.printStackTrace();}


        androidAutoWirelessIntent.setClassName(PACKAGE_NAME_ANDROID_AUTO_WIRELESS, CLASS_NAME_ANDROID_AUTO_WIRELESS);
        androidAutoWirelessIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        androidAutoWirelessIntent
                .putExtra(PARAM_HOST_ADDRESS_EXTRA_NAME, "127.0.0.1")
                .putExtra(PARAM_SERVICE_PORT_EXTRA_NAME, websockport)
                .putExtra("wifi_info",wifiinfo)
                .putExtra("PARAM_SERVICE_WIFI_NETWORK", ns);

        return androidAutoWirelessIntent;
    }
    private void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service found: " + service);
                mNsdManager.resolveService(service, new NsdManager.ResolveListener() {
                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {


                        Log.d(TAG, "Service resolved: " + serviceInfo);
                        hostIpAddress = serviceInfo.getHost().getHostAddress();
                        Log.d(TAG, "Host IP address: " + hostIpAddress);





                        try {
                            phoneToHur.start();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                new hurToPhone(service.getNetwork()).start();
                            }
                            else
                                new hurToPhone( null).start();

                            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
                            startActivity(getAAIntent());

                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        Log.e(TAG, "Resolve failed: " + errorCode);
                    }
                });
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "Service lost: " + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code: " + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code: " + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }


    private OutputStream phonetohuroutput,hurtophoneoutput;
    private DataInputStream phonetohurinput,hurtophoneinput;
    private Socket localsocket,remotesocket;
    private int websockport;
    private final Thread phoneToHur = new Thread(new Runnable() {
        @Override
        public void run() {

            try {
                ServerSocket serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(websockport));
                localsocket=serverSocket.accept();
                phonetohuroutput=localsocket.getOutputStream();
                phonetohurinput=new DataInputStream(localsocket.getInputStream());
                byte[] localbuffer=new byte[16384];

                while (isRunning)
                {
                    while (remotesocket==null)
                    {
                        Thread.sleep(100);
                    }
                    phonetohurinput.readFully(localbuffer,0,4);
                    short enc_len = (short) ((localbuffer[2] & 0xFF) << 8 | (localbuffer[3] & 0xFF));
                    if (localbuffer[1] == 9 || localbuffer[1] == 1)
                        enc_len += 4;

                    phonetohurinput.readFully(localbuffer, 4, enc_len);
                    hurtophoneoutput.write(localbuffer,0,enc_len+4);
                }
            } catch (Exception e) {
                e.printStackTrace();
                stopSelf();
            }


        }
    });


    private class hurToPhone extends Thread implements Runnable {

        Network n;

        public hurToPhone(Network n) {
            this.n = n;
        }

        @Override
        public void run() {
            Log.d(TAG,"Trying to connect: "+hostIpAddress);
            try {
                if (n==null)
                    remotesocket=new Socket(hostIpAddress,5288);
                else
                {
                    remotesocket = n.getSocketFactory().createSocket();
                    remotesocket.connect(new InetSocketAddress(hostIpAddress, 5288));
                }
                if (legacy)
                {
                    startActivity(getAAIntent());
                    phoneToHur.start();
                }
            } catch (IOException e) {
                //Failed to connect to HUR probably the wrong network.... whatever....
            }
            if (remotesocket!=null && remotesocket.isConnected())
                try {

                    hurtophoneoutput=remotesocket.getOutputStream();
                    hurtophoneinput=new DataInputStream(remotesocket.getInputStream());
                    byte[] remotebuffer=new byte[16384];
                    int got;
                    notification.setContentText(getString(R.string.connectedtocar));
                    notificationManager.notify(NOTIFICATION_ID, notification.build());
                    while (isRunning)
                    {
                        while (localsocket==null)
                        {
                            Thread.sleep(100);
                        }
                        hurtophoneinput.readFully(remotebuffer,0,4);

                        short enc_len = (short) ((remotebuffer[2] & 0xFF) << 8 | (remotebuffer[3] & 0xFF));
                        if (remotebuffer[1] == 9 || remotebuffer[1] == 1)
                            enc_len += 4;
                        hurtophoneinput.readFully(remotebuffer, 4, enc_len);
                        phonetohuroutput.write(remotebuffer,0,enc_len+4);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    stopSelf();
                }
        }

    };






    protected void verifyOrCreateNotificationChannels() {
        NotificationChannel notificationChannelNoVibrationDefault = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_NO_VIBRATION_DEFAULT_ID);
        if (notificationChannelNoVibrationDefault == null) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_NO_VIBRATION_DEFAULT_ID, getString(R.string.notification_channel_default_name), NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(getString(R.string.notification_channel_default_description));
            channel.enableVibration(false);
            channel.setSound(null, null);
            channel.setLockscreenVisibility(VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationChannel notificationChannelWithVibrationImportant = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_WITH_VIBRATION_IMPORTANT_ID);
        if (notificationChannelWithVibrationImportant == null) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_WITH_VIBRATION_IMPORTANT_ID, getString(R.string.notification_channel_high_name), NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(getString(R.string.notification_channel_high_description));
            channel.enableVibration(true);
            channel.setLockscreenVisibility(VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(channel);
        }
    }

    protected NotificationCompat.Builder getNotification(Context context, String contentText) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_NO_VIBRATION_DEFAULT_ID)
                .setContentTitle(getString(R.string.service_wifi_title))
                .setContentText(contentText)
                .setAutoCancel(false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_aa_wifi_notification)
                .setTicker(getString(R.string.service_wifi_ticker));

        Intent turnOffIntent = new Intent(context, WifiReceiver.class);
        turnOffIntent.setAction(ACTION_WIFI_LAUNCHER_EXIT);
        turnOffIntent.putExtra(EXTRA_NOTIFICATION_ID,0);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            pendingIntent = PendingIntent.getBroadcast(this, 0, turnOffIntent,  PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        else
            pendingIntent =
                    PendingIntent.getBroadcast(this, 0, turnOffIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        builder.addAction(
                new NotificationCompat.Action.Builder(R.drawable.ic_power_settings_new_24,
                        getString(R.string.turn_off),
                        pendingIntent).build());



        return builder;
    }




    private void updateWidget(int id){
        Intent intent = new Intent(this, WiFiLauncherServiceWidget.class);
        intent.setAction(WIDGET_ACTION);
        PendingIntent pd;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            pd = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        else
            pd = PendingIntent.getBroadcast(this, 0, intent, 0);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.widget_wi_fi_launcher_service);
        ComponentName thisWidget = new ComponentName(this, WiFiLauncherServiceWidget.class);
        remoteViews.setTextViewText(R.id.appwidget_text, getString(id));
        remoteViews.setOnClickPendingIntent(R.id.appwidget_container, pd);
        appWidgetManager.updateAppWidget(thisWidget, remoteViews);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null)
            if (ACTION_FOREGROUND_STOP.equals(intent.getAction())) {
                Log.d("WifiService", "Stop service");
                stopForeground(true);
                stopSelf();
            } else {
                Log.d("WifiService", "Start service");
                super.onStartCommand(intent, flags, startId);
            }
        else
            super.onStartCommand(intent, flags, startId);

        updateWidget(R.string.app_widget_running);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning=false;
        updateWidget(R.string.app_widget_paused);
        if (mDiscoveryListener != null) {
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            }
            catch (Exception e){}
        }


        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("settings_wireless_headunit_wifi_using_router", false))
                startTether(this, false);

        else
        {
            if (legacy)
                try {
                   connectivityManager.unregisterNetworkCallback( networkCallback);
                    }
                catch (Exception e){}
        }
    }



    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void getCurrentWifiGateway() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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
            hostIpAddress= gatewayAddress.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


    }





}
