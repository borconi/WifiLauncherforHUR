package com.borconi.emil.wifilauncherforhur.services;

import static androidx.core.app.NotificationCompat.EXTRA_NOTIFICATION_ID;
import static androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC;
import static com.borconi.emil.wifilauncherforhur.WiFiLauncherServiceWidget.WIDGET_ACTION;
import static com.borconi.emil.wifilauncherforhur.receivers.WifiReceiver.ACTION_WIFI_LAUNCHER_EXIT;
import static com.borconi.emil.wifilauncherforhur.tethering.MyTether.startTether;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
import android.net.Network;

import android.net.nsd.NsdManager;

import android.net.wifi.WifiManager;

import android.os.Build;
;
import android.os.IBinder;
import android.os.Parcel;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.connection.CarConnection;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;

import com.borconi.emil.wifilauncherforhur.R;
import com.borconi.emil.wifilauncherforhur.WiFiLauncherServiceWidget;
import com.borconi.emil.wifilauncherforhur.connectivity.ConnectivityHelper;
import com.borconi.emil.wifilauncherforhur.connectivity.Connector;
import com.borconi.emil.wifilauncherforhur.connectivity.NDSConnector;
import com.borconi.emil.wifilauncherforhur.connectivity.SocketListenner;
import com.borconi.emil.wifilauncherforhur.connectivity.TetherConnector;
import com.borconi.emil.wifilauncherforhur.connectivity.WiFiP2PConnector;
import com.borconi.emil.wifilauncherforhur.receivers.WiFiDirectBroadcastReceiver;
import com.borconi.emil.wifilauncherforhur.receivers.WifiReceiver;


import java.lang.reflect.Method;
import java.net.InetAddress;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Enumeration;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class WifiService extends Service {

    public static final int NOTIFICATION_ID = 1035;
    private static final String NOTIFICATION_CHANNEL_NO_VIBRATION_DEFAULT_ID = "wifilauncher_notification_channel_no_vibration_default";
    private static final String NOTIFICATION_CHANNEL_WITH_VIBRATION_IMPORTANT_ID = "wifilauncher_notification_channel_with_vibration_high";

    private static boolean isRunning = false;
    private Connector connector;

    public static final String ACTION_FOREGROUND_STOP = "actionWifiServiceForegroundStop";


    private NotificationManager notificationManager;


    private PendingIntent pendingIntent;
    private NotificationCompat.Builder notification;

    public static Network ns;
    public static AtomicBoolean connected=new AtomicBoolean(false);
    private LiveData<Integer> typeLiveData;
    static public boolean mustexit=false;

    public static boolean isRunning() {
        return isRunning;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mustexit=false;
        System.setProperty(
                "dexmaker.dexcache",
                getCacheDir().getPath());


        Random r = new Random();


        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        verifyOrCreateNotificationChannels();
        notification = getNotification(this, getString(R.string.service_wifi_looking_text));
        startForeground(NOTIFICATION_ID, notification.build());

        SharedPreferences sharedPreferences=PreferenceManager.getDefaultSharedPreferences(this);
        int connectionmode=Integer.parseInt(sharedPreferences.getString("connection_mode","1"));
        switch (connectionmode){
            case 1:
                connector=new NDSConnector(notificationManager,notification,this);
                break;
            case 2:
                connector=new WiFiP2PConnector(notificationManager,notification,this);
                break;
            case 4:
                connector=new TetherConnector(notificationManager,notification,this);
                break;
            case 5:
                connector=new ConnectivityHelper(notificationManager,notification,this);
        }

        isRunning=true;
        CarConnection carConnection = new CarConnection(this);
        typeLiveData = carConnection.getType();
        typeLiveData.observeForever(AAObserver);



    }
    private final Observer<Integer> AAObserver = new Observer<Integer>() {
        @Override
        public void onChanged(@Nullable Integer newValue) {
            // newValue is the updated value of myLiveData
            // Do something with the new value
            int connectionState=newValue.intValue();
            Log.d("WiFiService","Connection state: "+connectionState);
            switch(connectionState) {
                case CarConnection.CONNECTION_TYPE_NOT_CONNECTED:

                    if (connected.get())
                        stopSelf();
                    connected.set(false);
                    break;
                case CarConnection.CONNECTION_TYPE_NATIVE:
                    break;
                case CarConnection.CONNECTION_TYPE_PROJECTION:
                    connected.set(true);
                    notification.setContentText(getString(R.string.connectedtocar));
                    notificationManager.notify(NOTIFICATION_ID, notification.build());
                    break;
                default:

                    break;
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
        turnOffIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            pendingIntent = PendingIntent.getBroadcast(this, 0, turnOffIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        else
            pendingIntent =
                    PendingIntent.getBroadcast(this, 0, turnOffIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        builder.addAction(
                new NotificationCompat.Action.Builder(R.drawable.ic_power_settings_new_24,
                        getString(R.string.turn_off),
                        pendingIntent).build());


        return builder;
    }


    private void updateWidget(int id) {
        Intent intent = new Intent(this, WiFiLauncherServiceWidget.class);
        intent.setAction(WIDGET_ACTION);
        PendingIntent pd;
        pd = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);


        Log.d("WifiService","We should update our widget");
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.widget_wi_fi_launcher_service);
        ComponentName thisWidget = new ComponentName(this, WiFiLauncherServiceWidget.class);
        remoteViews.setTextViewText(R.id.appwidget_text, getString(id));
        remoteViews.setOnClickPendingIntent(R.id.appwidget_container, pd);
        if (id==R.string.app_widget_running)
            remoteViews.setImageViewResource(R.id.appwidget_icon, R.mipmap.ic_widget_running);
        else
            remoteViews.setImageViewResource(R.id.appwidget_icon, R.mipmap.ic_widget_preview_round);

        remoteViews.setOnClickPendingIntent(R.id.appwidget_icon, pd);
        appWidgetManager.updateAppWidget(thisWidget, remoteViews);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null)
            if (ACTION_FOREGROUND_STOP.equals(intent.getAction())) {
                Log.d("WifiService", "Stop service");
                mustexit=true;
                stopForeground(true);
                stopSelf();
            } else {
                if (isRunning)
                    return START_STICKY;
                Log.d("WifiService", "Start service");
                super.onStartCommand(intent, flags, startId);
            }
        else {
            if (isRunning)
                return START_STICKY;
            super.onStartCommand(intent, flags, startId);
        }



        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            connector.stop();

        }
        catch (Exception e){}

        connected.set(false);
        isRunning = false;
        typeLiveData.removeObserver(AAObserver);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        SharedPreferences pref=PreferenceManager.getDefaultSharedPreferences(this);
        boolean stillconnected=false;
        if (mustexit)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                  return;

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        Set<String> selectedBluetoothMacs = pref.getStringSet("selected_bluetooth_devices", null);
        if (selectedBluetoothMacs==null)
            return;

        for (BluetoothDevice device : pairedDevices) {
            Log.d("WiFi Service","Bonded device: "+device.getName());
            if (isConnected(device)) {
                String deviceAddress = device.getAddress();
                    if (selectedBluetoothMacs.contains(deviceAddress))
                        stillconnected = true;
                }
        }
        Log.d("This","We are still connected to the BT: "+stillconnected);
        if (stillconnected && pref.getBoolean("keep_running",false) && !pref.getBoolean("ignore_bt_disconnect",false))
            startForegroundService(new Intent(this,WifiService.class));


    }



    @Nullable
    public IBinder onBind(Intent intent) {

        return null;
    }


    public static boolean isConnected(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("isConnected", (Class[]) null);
            boolean connected = (boolean) m.invoke(device, (Object[]) null);
            return connected;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


}
