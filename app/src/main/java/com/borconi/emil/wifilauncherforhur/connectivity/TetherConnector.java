package com.borconi.emil.wifilauncherforhur.connectivity;

import static com.borconi.emil.wifilauncherforhur.tethering.MyTether.startTether;

import android.app.NotificationManager;
import android.content.Context;

import androidx.core.app.NotificationCompat;

public class TetherConnector extends SocketListenner{
    public TetherConnector(NotificationManager notificationManager, NotificationCompat.Builder notification, Context context) {
        super(notificationManager, notification, context);
        new Thread(this).start();
        startTether(context,true);
    }

    @Override
    public void stop(){
        startTether(context,false);
    }
}
