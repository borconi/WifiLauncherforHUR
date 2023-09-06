package com.borconi.emil.wifilauncherforhur.tethering;

import static android.content.Context.WIFI_SERVICE;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class MyTether {

    private static Context mContext;
    private static MyOreoWifiManager mMyOreoWifiManager;
    private final static String TAG = "HU-Tether";

    public static void configureHotspot(String name, String password, Context context, boolean forced) {
        WifiConfiguration apConfig = new WifiConfiguration();
        apConfig.SSID = name;
        apConfig.preSharedKey = password;
        apConfig.allowedKeyManagement.clear();
        apConfig.allowedKeyManagement.set(4);


    }

    public static void startTether(final Context context, boolean enabled) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        assert wifiManager != null;
        if (enabled && wifiManager != null)
            wifiManager.setWifiEnabled(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mContext = context;
            hotspotOreo(enabled);
            return;
        }
        try {
            if (Build.VERSION.SDK_INT == 25) //broken tethering
            {
                try {
                    Process p; //Turn on USB data
                    p = Runtime.getRuntime().exec("su");
                    DataOutputStream os = new DataOutputStream(p.getOutputStream());
                    os.writeBytes("service call connectivity " + ((enabled) ? "24" : "25") + " i32 0; exit; \n");
                    os.flush();
                    p.waitFor();
                } catch (Exception e) {
                }
            }

            WifiManager wifimanager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
            WifiConfiguration wificonfiguration = null;
            try {
                wifimanager.setWifiEnabled(!enabled);
                Method method = wifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                method.invoke(wifimanager, wificonfiguration, enabled);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void hotspotOreo(boolean turnOn) {

        if (mMyOreoWifiManager == null) {
            mMyOreoWifiManager = new MyOreoWifiManager(mContext);
        }

        if (turnOn) {


            MyOnStartTetheringCallback callback = new MyOnStartTetheringCallback() {
                @Override
                public void onTetheringStarted() {
                    Log.d(TAG, "Tethering Starterd");




                }

                @Override
                public void onTetheringFailed() {
                    Log.d(TAG, "Tethering failed");
                }
            };

            mMyOreoWifiManager.startTethering(callback, null);
        } else {
            mMyOreoWifiManager.stopTethering(false);

        }

    }
}
