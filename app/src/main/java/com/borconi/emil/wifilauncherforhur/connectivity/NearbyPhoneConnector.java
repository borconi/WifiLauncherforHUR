package com.borconi.emil.wifilauncherforhur.connectivity;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.borconi.emil.wifilauncherforhur.services.WifiService;
import com.borconi.emil.wifilauncherforhur.streams.InOutStream;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

public class NearbyPhoneConnector extends Connector {

    private static final String TAG = "NearbyPhoneConnector";
    private static final String SERVICE_ID = "gb.xxy.hr"; // Must match Car's Discovery ID

    private InOutStream inoutstream;
    private String connectedEndpointId;

    public NearbyPhoneConnector(NotificationManager notificationManager, NotificationCompat.Builder notification, Context context) {
        super(notificationManager, notification, context);
        start();
    }

    public void start() {
        Log.d(TAG, "Starting Nearby Connections Advertising...");
        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_POINT_TO_POINT).build();

        // Broadcast the phone's model name (e.g. "Pixel 7") so the car UI looks nice
        String deviceName = Build.MODEL;

        Nearby.getConnectionsClient(context)
                .startAdvertising(deviceName, SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener((Void unused) -> {
                    Log.d(TAG, "Successfully started advertising as " + deviceName);
                })
                .addOnFailureListener((Exception e) -> {
                    Log.e(TAG, "Failed to start advertising.", e);
                });
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    Log.d(TAG, "Car requested connection: " + connectionInfo.getEndpointName());

                    // 1. Create the stream object (It acts as the PayloadCallback)
                    inoutstream = new InOutStream(context, endpointId);

                    // Create an anonymous PayloadCallback just to catch the incoming stream
                    PayloadCallback payloadCallback = new PayloadCallback() {
                        @Override
                        public void onPayloadReceived(String endpointId, Payload payload) {
                            Log.d(TAG, "Phone onPayloadReceived type=" + payload.getType());
                            if (payload.getType() == Payload.Type.STREAM) {
                                Log.d(TAG, "Phone received car->phone STREAM payload");
                                inoutstream.setIncomingStream(payload.asStream().asInputStream());
                            }
                        }

                        @Override
                        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {}
                    };
                    Nearby.getConnectionsClient(context).acceptConnection(endpointId, payloadCallback);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            Log.d(TAG, "Connected successfully to Car: " + endpointId);
                            connectedEndpointId = endpointId;

                            // Stop advertising so other cars can't try to connect to us
                            Nearby.getConnectionsClient(context).stopAdvertising();

                            if (inoutstream != null) {
                                inoutstream.startOutgoingStream();
                            }

                            // 3. Start routing Android Auto data
                            try {
                                if (context instanceof WifiService) {
                                    ((WifiService) context).startNearbyProxy(inoutstream);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error starting Nearby Proxy", e);
                            }
                            break;

                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            Log.w(TAG, "Connection rejected by Car.");
                            break;

                        case ConnectionsStatusCodes.STATUS_ERROR:
                            Log.e(TAG, "Connection error.");
                            break;
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    Log.d(TAG, "Disconnected from Car: " + endpointId);
                    connectedEndpointId = null;
                }
            };

    @Override
    public void stop() {
        Log.d(TAG, "Stopping Nearby Connections Advertising...");
        Nearby.getConnectionsClient(context).stopAdvertising();
        Nearby.getConnectionsClient(context).stopAllEndpoints();
        connectedEndpointId = null;
    }
}