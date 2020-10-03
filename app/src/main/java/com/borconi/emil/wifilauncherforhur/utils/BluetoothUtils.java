package com.borconi.emil.wifilauncherforhur.utils;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.lang.reflect.Method;

public class BluetoothUtils {

    private static final String IS_CONNECTED = "isConnected";

    @SuppressWarnings("JavaReflectionMemberAccess")
    public static boolean isConnected(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod(IS_CONNECTED, (Class<?>[]) null);
            return (boolean) m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            // This exception should never appear.
            Log.i("BluetoothUtils", "Error while trying to see if Bluetooth Device is connected", e);
        }
        return false;
    }
}
