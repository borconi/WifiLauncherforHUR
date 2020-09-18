package com.borconi.emil.wifilauncherforhur.activities;


import android.util.Log;

public class BluetoothListPreference {
    private String mac;
    private String name;

    public BluetoothListPreference(String id, String name) {
        this.mac = id;
        this.name = name;
    }


    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    //to display object as a string in spinner
    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BluetoothListPreference) {
            BluetoothListPreference c = (BluetoothListPreference) obj;
            if (c.getMac().equals(mac)) {
                Log.d("Compare", "Object is equal");
                return true;
            }
        }
        Log.d("Compare", "Not this BT adapter");
        return false;
    }


}
