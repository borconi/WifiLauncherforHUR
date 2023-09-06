package com.borconi.emil.wifilauncherforhur.connectivity;

import static android.os.Looper.getMainLooper;


import android.app.NotificationManager;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.borconi.emil.wifilauncherforhur.services.WifiService;

public class NDSConnector extends Connector{
    private static final String TAG = "NetworkServiceDiscovery";
    public static final String SERVICE_TYPE = "_aawireless._tcp.";
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager mNsdManager;
    private boolean found=false;

    public NDSConnector(NotificationManager notificationManager, NotificationCompat.Builder notification, Context context) {
        super(notificationManager, notification, context);
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        initializeDiscoveryListener();
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    private void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");

                final Handler handler = new Handler(getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                            try {
                                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
                            }
                            catch (Exception e){}
                    }
                },10000);

            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service found: " + service);
                mNsdManager.resolveService(service, new NsdManager.ResolveListener() {
                    @Override
                    public void onServiceResolved(NsdServiceInfo serviceInfo) {


                        Log.d(TAG, "Service resolved: " + serviceInfo);
                        String hostIpAddress = serviceInfo.getHost().getHostAddress();
                        Log.d(TAG, "Host IP address: " + hostIpAddress);
                        if (!WifiService.connected.get())
                            context.startActivity(getAAIntent(hostIpAddress, false));
                        found=true;
                        try {
                            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
                        }
                        catch (Exception e){}
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
                if (!found)
                mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code: " + errorCode);

            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code: " + errorCode);

            }
        };
    }
    @Override
    public void stop()
    {
        found=true;
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }
}
