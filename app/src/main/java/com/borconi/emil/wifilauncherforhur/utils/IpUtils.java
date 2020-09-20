package com.borconi.emil.wifilauncherforhur.utils;

public class IpUtils {
    public static String IntToIp(int addr) {
        return (addr & 255) + "." + ((addr >>>= 8) & 255) + "." + ((addr >>>= 8) & 255) + "." + (addr >>> 8 & 255);
    }
}
