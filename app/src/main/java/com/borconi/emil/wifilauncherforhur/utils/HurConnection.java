package com.borconi.emil.wifilauncherforhur.utils;

import android.content.Context;
import android.content.Intent;

public class HurConnection {

    private static final String PACKAGE_NAME_ANDROID_AUTO_WIRELESS = "com.google.android.projection.gearhead";
    private static final String CLASS_NAME_ANDROID_AUTO_WIRELESS = "com.google.android.apps.auto.wireless.setup.service.impl.WirelessStartupActivity";

    private static final String PARAM_HOST_ADDRESS_EXTRA_NAME = "PARAM_HOST_ADDRESS";
    private static final String PARAM_SERVICE_PORT_EXTRA_NAME = "PARAM_SERVICE_PORT";

    public static void connect(final Context context, final String address) {
        Intent androidAutoWirelessIntent = new Intent();

        androidAutoWirelessIntent.setClassName(PACKAGE_NAME_ANDROID_AUTO_WIRELESS, CLASS_NAME_ANDROID_AUTO_WIRELESS);
        androidAutoWirelessIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        androidAutoWirelessIntent
                .putExtra(PARAM_HOST_ADDRESS_EXTRA_NAME, address)
                .putExtra(PARAM_SERVICE_PORT_EXTRA_NAME, 5288);

        context.startActivity(androidAutoWirelessIntent);
    }
}
