package com.borconi.emil.wifilauncherforhur.services;

import static androidx.core.app.NotificationCompat.EXTRA_NOTIFICATION_ID;
import static androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC;
import static com.borconi.emil.wifilauncherforhur.WiFiLauncherServiceWidget.WIDGET_ACTION;
import static com.borconi.emil.wifilauncherforhur.connectivity.Connector.getAAIntent;
import static com.borconi.emil.wifilauncherforhur.receivers.WifiReceiver.ACTION_WIFI_LAUNCHER_EXIT;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.car.app.connection.CarConnection;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;

import com.borconi.emil.wifilauncherforhur.R;
import com.borconi.emil.wifilauncherforhur.WiFiLauncherServiceWidget;
import com.borconi.emil.wifilauncherforhur.connectivity.ConnectivityHelper;
import com.borconi.emil.wifilauncherforhur.connectivity.Connector;
import com.borconi.emil.wifilauncherforhur.connectivity.NDSConnector;
import com.borconi.emil.wifilauncherforhur.connectivity.NearbyPhoneConnector;
import com.borconi.emil.wifilauncherforhur.connectivity.TetherConnector;
import com.borconi.emil.wifilauncherforhur.connectivity.WiFiP2PConnector;
import com.borconi.emil.wifilauncherforhur.receivers.WifiReceiver;
import com.borconi.emil.wifilauncherforhur.streams.InOutStream;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class WifiService extends Service {

    public static final int NOTIFICATION_ID = 1035;
    private static final String NOTIFICATION_CHANNEL_NO_VIBRATION_DEFAULT_ID = "wifilauncher_notification_channel_no_vibration_default";
    private static final String NOTIFICATION_CHANNEL_WITH_VIBRATION_IMPORTANT_ID = "wifilauncher_notification_channel_with_vibration_high";

    public static final String ACTION_FOREGROUND_STOP = "actionWifiServiceForegroundStop";
    public static final String ACTION_CONNECT_QR_WIFI = "actionConnectQrWifi";
    public static final String EXTRA_SSID = "extraSsid";
    public static final String EXTRA_PASS = "extraPass";

    private static boolean isRunning = false;
    private Connector connector;

    // Updated proxy components
    private LocalSocketListener localSocketListener;
    private volatile String currentPeerIp = null;

    private NotificationManager notificationManager;
    private PendingIntent pendingIntent;
    private NotificationCompat.Builder notification;
    private volatile InOutStream nearbyStream = null;

    public static Network ns;
    public static AtomicBoolean connected = new AtomicBoolean(false);
    private androidx.lifecycle.LiveData<Integer> typeLiveData;
    static public boolean mustexit = false;

    public static boolean isRunning() {
        return isRunning;
    }

    public interface ConnectionCallback {
        void onNewConnection(Socket localSocket);
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES})
    @Override
    public void onCreate() {
        super.onCreate();
        mustexit = false;
        System.setProperty("dexmaker.dexcache", getCacheDir().getPath());

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        verifyOrCreateNotificationChannels();
        notification = getNotification(this, getString(R.string.service_wifi_looking_text));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification.build(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC );
        } else {
            startForeground(NOTIFICATION_ID, notification.build());
        }


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int connectionmode = Integer.parseInt(sharedPreferences.getString("connection_mode", "1"));
        switch (connectionmode) {
            case 1:
                connector = new NDSConnector(notificationManager, notification, this);
                break;
            case 2:
                connector = new WiFiP2PConnector(notificationManager, notification, this);
                break;
            case 4:
                connector = new TetherConnector(notificationManager, notification, this);
                break;
            case 5:
                connector = new ConnectivityHelper(notificationManager, notification, this);
                break;
            case 3:
                connector = new NearbyPhoneConnector(notificationManager,notification,this);
                break;
        }

        // Start listening continuously on port 5999 while the service is alive
        localSocketListener = new LocalSocketListener(this::handleNewLocalConnection);
        localSocketListener.startListening();

        isRunning = true;
        CarConnection carConnection = new CarConnection(getApplicationContext());
        typeLiveData = carConnection.getType();
        typeLiveData.observeForever(AAObserver);
    }

    // Triggered by WiFiDirectBroadcastReceiver when peer connects
    public void startSocketProxy(String peerIp) {
        Log.d("WifiService", "Registering target peer IP for socket proxy: " + peerIp);
        getAAIntent("127.0.0.1", this, 5999);
        this.currentPeerIp = peerIp;
        if (connector instanceof WiFiP2PConnector) {
            ((WiFiP2PConnector) connector).stopAdvertising();
        }
    }

    public void startNearbyProxy(InOutStream stream) {
        Log.d("WifiService", "Registering Nearby Connections stream for socket proxy.");
        this.nearbyStream = stream;

        // Start Android Auto connecting to our local proxy socket
        getAAIntent("127.0.0.1", this, 5999);
    }

    private void handleNewLocalConnection(Socket localSocket) {
        String targetIp = currentPeerIp;
        InOutStream targetNearbyStream = nearbyStream;


        if (currentPeerIp == null && nearbyStream == null) {
            Log.w("WifiService", "Local connection received, but no peer IP or Nearby stream is registered yet. Dropping connection.");
            try { localSocket.close(); } catch (Exception ignored) {}
            return;
        }
            // MODES 1, 2, 4, 5: Route over standard TCP IP Socket
            Log.d("WifiService", "Local connection accepted. Routing to " + targetIp + ":5288");
            new Thread(() -> {
                Socket remoteSocket;
                try {
                    if (targetNearbyStream != null)
                        remoteSocket=targetNearbyStream;
                    else
                        remoteSocket= new Socket(targetIp, 5288);
                    // Start bidirectional pumping threads
                    new Thread(new StreamCopier(localSocket.getInputStream(), remoteSocket.getOutputStream())).start();
                    new Thread(new StreamCopier(remoteSocket.getInputStream(), localSocket.getOutputStream())).start();
                } catch (Exception e) {
                    Log.e("WifiService", "Error establishing route to remote socket", e);
                    try { localSocket.close(); } catch (Exception ignored) {}
                }
            }).start();

    }

    private final Observer<Integer> AAObserver = new Observer<Integer>() {
        @Override
        public void onChanged(@Nullable Integer newValue) {
            int connectionState = newValue.intValue();
            Log.d("WiFiService", "Connection state: " + connectionState);
            switch (connectionState) {
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
            pendingIntent = PendingIntent.getBroadcast(this, 0, turnOffIntent, PendingIntent.FLAG_UPDATE_CURRENT);

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

        Log.d("WifiService", "We should update our widget");
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.widget_wi_fi_launcher_service);
        ComponentName thisWidget = new ComponentName(this, WiFiLauncherServiceWidget.class);
        remoteViews.setTextViewText(R.id.appwidget_text, getString(id));
        remoteViews.setOnClickPendingIntent(R.id.appwidget_container, pd);
        if (id == R.string.app_widget_running)
            remoteViews.setImageViewResource(R.id.appwidget_icon, R.mipmap.ic_widget_running);
        else
            remoteViews.setImageViewResource(R.id.appwidget_icon, R.mipmap.ic_widget_preview_round);

        remoteViews.setOnClickPendingIntent(R.id.appwidget_icon, pd);
        appWidgetManager.updateAppWidget(thisWidget, remoteViews);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        // 1. Intercept the new QR connection intent here
        if (ACTION_CONNECT_QR_WIFI.equals(action)) {
            String ssid = intent.getStringExtra(EXTRA_SSID);
            String pass = intent.getStringExtra(EXTRA_PASS);

            if (ssid != null) {
                Log.d("WifiService", "New QR Code scanned. Forcing Mode 5 and connecting to: " + ssid);

                // Save mode 5 and persist credentials for future auto-reconnects
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putString("connection_mode", "5")
                        .putString("last_qr_ssid", ssid)
                        .putString("last_qr_pass", pass)
                        .apply();

                // Stop current connector and transition to ConnectivityHelper
                if (!(connector instanceof ConnectivityHelper)) {
                    if (connector != null) connector.stop();
                    connector = new ConnectivityHelper(notificationManager, notification, this);
                }

                // Fire the connection logic
                ((ConnectivityHelper) connector).connectToNetwork(ssid, pass);
            }
            return START_STICKY;
        }

        if (ACTION_FOREGROUND_STOP.equals(action)) {
            Log.d("WifiService", "Stop service");
            mustexit = true;
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        // 2. Normal Start / Auto-Reconnect Logic
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String mode = prefs.getString("connection_mode", "1");

        if ("5".equals(mode)) {
            // If connection mode is 5 and no QR code, try to connect to the last successfully connected network
            String lastSsid = prefs.getString("last_qr_ssid", null);
            String lastPass = prefs.getString("last_qr_pass", null);

            if (lastSsid != null) {
                Log.d("WifiService", "Mode 5 active. Auto-reconnecting to last QR network: " + lastSsid);
                if (!(connector instanceof ConnectivityHelper)) {
                    if (connector != null) connector.stop();
                    connector = new ConnectivityHelper(notificationManager, notification, this);
                }
                ((ConnectivityHelper) connector).connectToNetwork(lastSsid, lastPass);
            }
        } else {
            // If no QR code and wifi connection is not 5, do nothing.
            // (The connector for modes 1, 2, or 4 was already initialized normally in onCreate)
            Log.d("WifiService", "Standard service start. Active mode: " + mode);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (connector != null) connector.stop();
        } catch (Exception e) {}

        if (localSocketListener != null) {
            localSocketListener.stopListening();
        }

        connected.set(false);
        isRunning = false;
        typeLiveData.removeObserver(AAObserver);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean stillconnected = false;
        if (mustexit)
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                return;

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        Set<String> selectedBluetoothMacs = pref.getStringSet("selected_bluetooth_devices", null);
        if (selectedBluetoothMacs == null)
            return;

        for (BluetoothDevice device : pairedDevices) {
            Log.d("WiFi Service", "Bonded device: " + device.getName());
            if (isConnected(device)) {
                String deviceAddress = device.getAddress();
                if (selectedBluetoothMacs.contains(deviceAddress))
                    stillconnected = true;
            }
        }
        Log.d("This", "We are still connected to the BT: " + stillconnected);
        if (stillconnected && pref.getBoolean("keep_running", false) && !pref.getBoolean("ignore_bt_disconnect", false))
            startForegroundService(new Intent(this, WifiService.class));
    }

    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static boolean isConnected(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("isConnected", (Class[]) null);
            return (boolean) m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // --- Inner Classes For Port Proxying ---

    private static class LocalSocketListener {
        private ServerSocket serverSocket;
        private volatile boolean isRunning;
        private final ConnectionCallback callback;

        LocalSocketListener(ConnectionCallback callback) {
            this.callback = callback;
        }

        void startListening() {
            isRunning = true;
            new Thread(() -> {
                try {
                    serverSocket = new ServerSocket(5999);
                    Log.d("LocalSocketListener", "Server Socket listening locally on port 5999...");

                    while (isRunning) {
                        Socket localSocket = serverSocket.accept();
                        Log.d("LocalSocketListener", "New connection received......");
                        if (callback != null) {
                            callback.onNewConnection(localSocket);
                        }
                    }
                } catch (Exception e) {
                    if (isRunning) {
                        Log.e("LocalSocketListener", "Server socket error", e);
                    }
                }
            }).start();
        }

        void stopListening() {
            isRunning = false;
            try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
        }
    }

    private static class StreamCopier implements Runnable {
        private final InputStream in;
        private final OutputStream out;

        StreamCopier(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[16384];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    Log.d("StreamCopier", "copied " + read + " bytes");
                    out.write(buffer, 0, read);
                    out.flush();
                }
            } catch (Exception e) {
                Log.e("StreamCopier", "Stream pipe disrupted.", e);
            }
        }
    }
}