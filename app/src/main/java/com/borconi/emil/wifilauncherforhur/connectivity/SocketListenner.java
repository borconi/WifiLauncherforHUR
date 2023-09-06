package com.borconi.emil.wifilauncherforhur.connectivity;

import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;


public class SocketListenner extends Connector implements Runnable{


    Context context;
    Socket transportSocket = null;

    public SocketListenner(NotificationManager notificationManager, NotificationCompat.Builder notification, Context context){
        super(notificationManager,notification,context);
        this.context=context;
    }
    @Override
    public void run() {

        while (transportSocket==null ) {
            try {
                ServerSocket serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(5289));
                Log.w("HU", "Listening for incoming connection");
                transportSocket = serverSocket.accept();
                String hostIpAddress = (((InetSocketAddress) transportSocket.getRemoteSocketAddress()).getAddress()).toString().replace("/", "");
                Log.d("HU","Headunit reloaded on ip: "+hostIpAddress);
                startAA(hostIpAddress,true);


                Log.d("HU","Start Activity called: "+getAAIntent(hostIpAddress, true));
            } catch (Exception e) {
            }
        }

    }




}
