package com.borconi.emil.wifilauncherforhur.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.borconi.emil.wifilauncherforhur.R;
import com.borconi.emil.wifilauncherforhur.activities.EnableWifiActivity;
import com.borconi.emil.wifilauncherforhur.listeners.WifiServiceStatusChangedListener;
import com.borconi.emil.wifilauncherforhur.receivers.CarModeReceiver;
import com.borconi.emil.wifilauncherforhur.receivers.WifiReceiver;
import com.borconi.emil.wifilauncherforhur.receivers.WifiLocalReceiver;
import com.borconi.emil.wifilauncherforhur.utils.HurConnection;
import com.borconi.emil.wifilauncherforhur.utils.IpUtils;

import java.util.ArrayList;
import java.util.List;

import static android.app.Notification.EXTRA_NOTIFICATION_ID;
import static android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION;
import static androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC;
import static com.borconi.emil.wifilauncherforhur.receivers.CarModeReceiver.ACTION_ENTER_CAR_MODE;
import static com.borconi.emil.wifilauncherforhur.receivers.CarModeReceiver.ACTION_EXIT_CAR_MODE;
import static com.borconi.emil.wifilauncherforhur.receivers.WifiLocalReceiver.ACTION_WIFI_LAUNCHER_EXIT;
import static com.borconi.emil.wifilauncherforhur.receivers.WifiLocalReceiver.ACTION_WIFI_LAUNCHER_FORCE_CONNECT;

public class WifiService extends Service {
    private static final int FIVE_SECONDS = 5000;
    private static final int EIGHT_SECONDS = 8000;
    private static final int TWO_MINUTES = 60 * 1000 * 2;
    private static final String STRING_EMPTY = "";
    private static final String WIFI_STRING_FORMAT = "\"%s\"";
    public static boolean askingForWiFi = false;
    private static final List<WifiServiceStatusChangedListener> listeners = new ArrayList<>();
    private static boolean isRunning = false;
    private static boolean isConnected = false;
    private ConnectivityManager.NetworkCallback networkCallback;
    private CarModeReceiver carModeReceiver;
    private WifiLocalReceiver wifiLocalReceiver;
    private final Handler mHandler = new Handler();
    private String headunitWifiSsid;
    private String headunitWifiWpa2Passphrase;
    private Integer networkId;

    @Override
    public void onCreate() {
        super.onCreate();
        setIsRunning(true);

        setSharedPreferencesValues();
        registerOnSharedPreferenceChangeListener();

        registerReceivers();

        Notification notification = getNotification();
        startForeground(1, notification);

        // Tries to connect to HUR for the first time
        tryToConnect();

        // We will try to connect for 2 minutes, otherwise we will stop this service.
        mHandler.postDelayed(StopServiceRunnable, TWO_MINUTES);
    }

    public void tryToConnect() {
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

        if (!verifyOrEnableWifi()) {
            return;
        }

        verifyOrAddHurHotspot();

        // If connected Wi-Fi is diff than desired, we will disconnect and try to reconnect.
        if (wifiManager.getConnectionInfo().getNetworkId() != networkId) {
            Log.d("HU-Wifi", "Start up, not connected to HUR network, is it in range?");

            wifiManager.disconnect();
            wifiManager.enableNetwork(networkId, true);
            wifiManager.reconnect();
            mHandler.postDelayed(CheckIfIsConnectedRunnable, EIGHT_SECONDS);
        }

        // If Wi-Fi is in connected state, we will send AAWireless intent to HUR.
        if (wifiManager.getConnectionInfo().getSupplicantState() == SupplicantState.COMPLETED) {
            removeAllCallBacks();
            mHandler.postDelayed(CheckIfIsConnectedRunnable, FIVE_SECONDS);

            String gatewayAddress = IpUtils.IntToIp(wifiManager.getDhcpInfo().gateway);
            HurConnection.connect(getApplicationContext(), gatewayAddress);
            Log.d("HurConnection", "Sending Intent to HUR! > gatewayAddress: " + gatewayAddress);
        }
    }

    protected boolean verifyOrEnableWifi() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (!wifiManager.isWifiEnabled()) {
            // Since Android 10 we can't turn on/off wifi programmatically
            // https://developer.android.com/reference/android/net/wifi/WifiManager#setWifiEnabled(boolean)
            // follow this post if Google enables it again: https://issuetracker.google.com/issues/128554616
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (!askingForWiFi) {
                    askingForWiFi = true;
                    // Let's send a message to the user to turn it on.
                    Intent enableWifiActivityIntent = new Intent(getApplicationContext(), EnableWifiActivity.class);
                    enableWifiActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    enableWifiActivityIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                    startActivity(enableWifiActivityIntent);
                    mHandler.postDelayed(CheckIfIsConnectedRunnable, EIGHT_SECONDS);
                }
                return false;
            } else { // Android Pie can turn on Wi-Fi
                wifiManager.setWifiEnabled(true);
            }
        }
        return true;
    }

    protected void verifyOrAddHurHotspot() {
        if (networkId == null) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            @SuppressLint("MissingPermission")
            List<WifiConfiguration> currentWifis = wifiManager.getConfiguredNetworks();

            currentWifis.stream()
                    // Removes " from beginning and ending of string
                    .filter(w -> w.SSID.replaceAll("^\"|\"$", STRING_EMPTY).equalsIgnoreCase(headunitWifiSsid))
                    .findFirst()
                    .ifPresent(w -> networkId = w.networkId);

            if (networkId == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiNetworkSuggestion wifiNetworkSuggestion = new WifiNetworkSuggestion
                            .Builder()
                            .setSsid(headunitWifiSsid)
                            .setWpa2Passphrase(headunitWifiWpa2Passphrase)
                            .build();
                    ArrayList<WifiNetworkSuggestion> wifiSuggestionsList = new ArrayList<>();
                    wifiSuggestionsList.add(wifiNetworkSuggestion);
                    wifiManager.addNetworkSuggestions(wifiSuggestionsList);
                }

                Log.d("HU-Wifi", "HUR wifi not in the list, add it and try to connect");
                WifiConfiguration wifiConfig = new WifiConfiguration();
                wifiConfig.SSID = String.format(WIFI_STRING_FORMAT, headunitWifiSsid);
                wifiConfig.preSharedKey = String.format(WIFI_STRING_FORMAT, headunitWifiWpa2Passphrase);
                networkId = wifiManager.addNetwork(wifiConfig);
            }
        }
    }

    protected void registerReceivers() {
        // This needs to be explicit registered in order to work
        // https://developer.android.com/reference/android/app/UiModeManager.html#ACTION_ENTER_CAR_MODE
        final IntentFilter intentFilterCarModeReceiver = new IntentFilter();
        intentFilterCarModeReceiver.addAction(ACTION_EXIT_CAR_MODE);
        intentFilterCarModeReceiver.addAction(ACTION_ENTER_CAR_MODE);
        intentFilterCarModeReceiver.addAction(NETWORK_STATE_CHANGED_ACTION);
        carModeReceiver = new CarModeReceiver();
        registerReceiver(carModeReceiver, intentFilterCarModeReceiver);

        // Start WifiReceiverLocal
        final IntentFilter intentFilterWifiReceiverLocal = new IntentFilter();
        intentFilterWifiReceiverLocal.addAction(NETWORK_STATE_CHANGED_ACTION);
        intentFilterWifiReceiverLocal.addAction(ACTION_WIFI_LAUNCHER_EXIT);
        intentFilterWifiReceiverLocal.addAction(ACTION_WIFI_LAUNCHER_FORCE_CONNECT);
        wifiLocalReceiver = new WifiLocalReceiver(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(wifiLocalReceiver, intentFilterWifiReceiverLocal);
    }

    protected Notification getNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = new NotificationChannel("wifilauncher_notification", "Wi-Fi Launcher Service", NotificationManager.IMPORTANCE_DEFAULT);
        mChannel.setDescription("Wi-Fi Launcher for HUR");
        mChannel.setShowBadge(false);
        mChannel.setLockscreenVisibility(VISIBILITY_PUBLIC);
        mNotificationManager.createNotificationChannel(mChannel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), mChannel.getId())
                .setContentTitle(getString(R.string.service_wifi_title))
                .setContentText(getString(R.string.service_wifi_text))
                .setSmallIcon(R.drawable.aawifi)
                .setTicker(getString(R.string.service_wifi_ticker));

        Intent turnOffIntent = new Intent(getApplicationContext(), WifiReceiver.class);
        turnOffIntent.setAction(ACTION_WIFI_LAUNCHER_EXIT);
        turnOffIntent.putExtra(EXTRA_NOTIFICATION_ID, 10);

        PendingIntent turnOffPendingIntent =
                PendingIntent.getBroadcast(getApplicationContext(), 10, turnOffIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        builder.addAction(
                new NotificationCompat.Action.Builder(R.drawable.ic_exit_to_app_24px,
                        getString(R.string.turn_off),
                        turnOffPendingIntent).build());

        if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("enableForceConnectButton", false)) {
            Intent forceConnectIntent = new Intent(getApplicationContext(), WifiReceiver.class);
            forceConnectIntent.setAction(ACTION_WIFI_LAUNCHER_FORCE_CONNECT);
            forceConnectIntent.putExtra(EXTRA_NOTIFICATION_ID, 20);

            PendingIntent forceConnectPendingIntent =
                    PendingIntent.getBroadcast(getApplicationContext(), 20, forceConnectIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            builder.addAction(
                    new NotificationCompat.Action.Builder(R.drawable.ic_exit_to_app_24px,
                            getString(R.string.force_connect),
                            forceConnectPendingIntent).build());
        }

        return builder.build();
    }

    protected void setSharedPreferencesValues() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        headunitWifiSsid = sharedPreferences.getString("headunitWifiSsid", getString(R.string.headunitWifiSsid_default_value));
        headunitWifiWpa2Passphrase = sharedPreferences.getString("headunitWifiWpa2Passphrase", getString(R.string.headunitWifiWpa2Passphrase_default_value));
    }

    protected void registerOnSharedPreferenceChangeListener() {
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                .registerOnSharedPreferenceChangeListener((prefs, key) -> {
                    switch (key) {
                        case "headunitWifiSsid":
                            headunitWifiSsid = prefs.getString(key, getString(R.string.headunitWifiSsid_default_value));
                        case "headunitWifiWpa2Passphrase":
                            headunitWifiWpa2Passphrase = prefs.getString(key, getString(R.string.headunitWifiWpa2Passphrase_default_value));
                    }
                });
    }

    private void removeAllCallBacks() {
        mHandler.removeCallbacks(TryToConnectRunnable);
        mHandler.removeCallbacks(CheckIfIsConnectedRunnable);
    }

    private final Runnable TryToConnectRunnable = () -> {
        Log.d("WifiService", "TRY TO CONNECT FROM RUNNABLE");
        checkIfIsInCarMode();

        if (!isConnected) {
            tryToConnect();
        }
    };
    private final Runnable CheckIfIsConnectedRunnable = new Runnable() {
        public void run() {
            Log.d("WifiService", "CHECKING IF IT'S CONNECTED");

            checkIfIsInCarMode();

            if (!isConnected) {
                mHandler.removeCallbacks(TryToConnectRunnable);
                mHandler.postDelayed(TryToConnectRunnable, FIVE_SECONDS);
            } else {
                mHandler.removeCallbacks(CheckIfIsConnectedRunnable);
                mHandler.removeCallbacks(TryToConnectRunnable);
            }
        }
    };

    private final Runnable StopServiceRunnable = new Runnable() {
        public void run() {
            Log.d("WifiService", "STOP SERVICE RUNNABLE CHECK");
            if (!isConnected) {
                Log.d("WifiService", "STOPPING SERVICE BY TIME (2 min)");
                stopSelf();
            } else {
                mHandler.removeCallbacks(StopServiceRunnable);
            }
        }
    };

    /**
     * Register onLostNetworkCallback if stopServiceLosesWifiConnection preference is turned on and if it's connected to HUR.
     */
    private void registerOnLostNetworkCallback() {
        if (isConnected && networkCallback == null) {
            Log.d("Wifi Service", "registering OnLostNetworkCallback");
            final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkRequest.Builder builder = new NetworkRequest.Builder();
                builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onLost(@NonNull Network network) {
                        Log.d("Wifi Service", "Lost connection to HUR wifi, exiting the app");
                        setIsConnected(false);
                        removeNetworkCallback();
                        stopSelf();
                    }
                };
                connectivityManager.registerNetworkCallback(
                        builder.build(), networkCallback
                );
            }
        }
    }

    protected void checkIfIsInCarMode() {
        if (((UiModeManager) getSystemService(Context.UI_MODE_SERVICE)).getCurrentModeType() == Configuration.UI_MODE_TYPE_CAR) {
            Log.d("WifiService", "ENTER CAR MODE (detected on WifiService)");
            setIsConnected(true);
            registerOnLostNetworkCallback();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("WiFi-Launcher", "Start service");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setIsRunning(false);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(wifiLocalReceiver);
        unregisterReceiver(carModeReceiver);

        removeAllCallBacks();
        mHandler.removeCallbacks(StopServiceRunnable);
    }

    public static void addStatusChangedListener(WifiServiceStatusChangedListener listener) {
        listeners.add(listener);
    }

    public static boolean isRunning() {
        return isRunning;
    }

    private static void setIsRunning(boolean isRunning) {
        WifiService.isRunning = isRunning;
        listeners.forEach(l -> l.OnStatusChanged(isRunning, isConnected));
    }

    public static boolean isConnected() {
        return isConnected;
    }

    public static void setIsConnected(boolean isConnected) {
        WifiService.isConnected = isConnected;
        listeners.forEach(l -> l.OnStatusChanged(isRunning, isConnected));
    }

    private void removeNetworkCallback() {
        if (networkCallback != null) {
            Log.d("Wifi Service", "Removing Network callback");
            final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
    }

    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }
}
