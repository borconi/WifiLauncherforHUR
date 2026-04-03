package com.borconi.emil.wifilauncherforhur.connectivity;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.borconi.emil.wifilauncherforhur.services.WifiService;

public class ConnectivityHelper extends Connector {

    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;
    private final static String TAG = "ConnectivityHelper";

    private static final String PREF_SSID = "last_saved_ssid";
    private static final String PREF_PASS = "last_saved_pass";

    private String currentRequestedSsid = null;
    private boolean isRunning = true; // Controls the retry loop
    private final Handler retryHandler = new Handler(Looper.getMainLooper());

    public ConnectivityHelper(NotificationManager notificationManager, NotificationCompat.Builder notification, Context context) {
        super(notificationManager, notification, context);
        start();
    }

    public void start() {
        isRunning = true;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String savedSsid = prefs.getString(PREF_SSID, null);
        String savedPass = prefs.getString(PREF_PASS, null);

        if (savedSsid != null && !savedSsid.isEmpty()) {
            Log.d(TAG, "Starting ConnectivityHelper with last known network: " + savedSsid);
            connectToNetwork(savedSsid, savedPass != null ? savedPass : "");
        } else {
            Log.w(TAG, "No saved network found for ConnectivityHelper to auto-connect.");
        }
    }

    @SuppressLint("MissingPermission")
    public void connectToNetwork(String ssid, String pass) {
        if (!isRunning) return;

        // Guard against duplicate overlapping requests
        if (ssid.equals(currentRequestedSsid) && networkCallback != null) {
            Log.d(TAG, "Already requesting or connected to: " + ssid + ". Ignoring duplicate request.");
            return;
        }
        currentRequestedSsid = ssid;

        Log.d(TAG, "Requesting Wi-Fi network: " + ssid);

        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString(PREF_SSID, ssid);
        editor.putString(PREF_PASS, pass);
        editor.apply();

        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Clean up previous callback safely
        cleanUpCallback();

        WifiNetworkSpecifier.Builder specifierBuilder = new WifiNetworkSpecifier.Builder().setSsid(ssid);
        if (pass != null && !pass.isEmpty()) {
            specifierBuilder.setWpa2Passphrase(pass);
        }

        NetworkRequest request = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .setNetworkSpecifier(specifierBuilder.build())
                .build();

        networkCallback = new ConnectivityManager.NetworkCallback() {

            private boolean proxyStarted = false;

            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                Log.d(TAG, "Requested Wi-Fi Network Available. Binding app traffic...");
                connectivityManager.bindProcessToNetwork(network);

                // Cancel any pending retries since we successfully connected
                retryHandler.removeCallbacksAndMessages(null);
            }

            @Override
            public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties);

                if (proxyStarted) return;

                if (linkProperties.getDhcpServerAddress() != null) {
                    String carIp = linkProperties.getDhcpServerAddress().getHostAddress();

                    if (carIp != null && carIp.contains(".")) {
                        Log.d(TAG, "Discovered Car IP via DHCP: " + carIp);
                        proxyStarted = true;

                        if (context instanceof WifiService) {
                            ((WifiService) context).startSocketProxy(carIp);
                        }
                    }
                }
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                Log.w(TAG, "Network request timed out or was rejected (onUnavailable).");
                scheduleRetry(ssid, pass);
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                Log.w(TAG, "Requested Wi-Fi Network connection lost. Unbinding process.");
                proxyStarted = false;
                connectivityManager.bindProcessToNetwork(null);
                scheduleRetry(ssid, pass);
            }
        };

        connectivityManager.requestNetwork(request, networkCallback);
    }

    private void scheduleRetry(String ssid, String pass) {
        if (!isRunning) return;

        // 1. Clear the guard so the retry isn't blocked!
        currentRequestedSsid = null;

        // 2. Unregister the dead callback
        cleanUpCallback();

        Log.d(TAG, "Scheduling reconnection attempt in 5 seconds...");
        retryHandler.postDelayed(() -> {
            if (isRunning) {
                Log.d(TAG, "Executing scheduled retry for: " + ssid);
                connectToNetwork(ssid, pass);
            }
        }, 5000);
    }

    private void cleanUpCallback() {
        if (networkCallback != null && connectivityManager != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {}
            networkCallback = null;
        }
    }

    @Override
    public void stop() {
        Log.d(TAG, "Stopping ConnectivityHelper and terminating loops.");
        isRunning = false;
        retryHandler.removeCallbacksAndMessages(null);
        currentRequestedSsid = null;

        try {
            cleanUpCallback();
            if (connectivityManager != null) {
                connectivityManager.bindProcessToNetwork(null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping ConnectivityHelper", e);
        }
    }
}