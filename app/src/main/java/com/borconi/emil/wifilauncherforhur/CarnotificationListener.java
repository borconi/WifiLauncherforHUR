package com.borconi.emil.wifilauncherforhur;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class CarnotificationListener extends NotificationListenerService {

    private static BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    private BTReceiver bt;
    public static final UUID MY_UUID=UUID.fromString("a7828c76-6ee1-445f-bb90-8156a2d371da");

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter ();
        filter.addAction("android.net.wifi.STATE_CHANGE");
        filter.addAction("android.app.action.EXIT_CAR_MODE");
        bt=new BTReceiver();
        registerReceiver(bt,filter);

        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(btReceiver, filter1);
        if (adapter.isEnabled())
            waitforHUR();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bt);
        unregisterReceiver(btReceiver);
        startService(new Intent(this,CarnotificationListener.class));
    }

    @Override
    public void onListenerDisconnected () {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(new ComponentName(this, CarnotificationListener.class));
        }
    }


    private void waitforHUR(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d("WiFi Launcher","Waiting for incoming BT");
                    BluetoothServerSocket sock = adapter.listenUsingRfcommWithServiceRecord("HUR", MY_UUID);
                    sock.accept();
                    Log.d("WiFi Launcher","BT connected");
                    WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    assert wifiManager != null;
                    wifiManager.setWifiEnabled(true);
                    if (wifiManager.getConnectionInfo().getNetworkId()>0)
                                BTReceiver.connectToHur(getApplicationContext());
                    sock.close();
                    waitforHUR();
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
        }).start();

    }

    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d("BT-INIT","Action: "+action);
            assert action != null;
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch(state) {
                    case BluetoothAdapter.STATE_OFF:
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_ON:
                        waitforHUR();
                        break;

                }

            }

        }
    };
    }
