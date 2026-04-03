package com.borconi.emil.wifilauncherforhur.streams;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class InOutStream extends PayloadCallback {

    private static final String TAG = "InOutStream";

    private final Context context; // Now passed securely via constructor
    private final String endpointID;
    private final ConnectionInfo connectionInfo;

    // Increased buffer size to handle high-bandwidth Android Auto video streams
    private final BlockingQueue<byte[]> freeInputBuffers = new ArrayBlockingQueue<>(2048);

    public static InOutStream currentStream = null;

    // --- The Standard Java I/O Stream Adapters ---
    private final InputStream inputStreamAdapter;
    private final OutputStream outputStreamAdapter;

    // Constructor MUST require Context to initialize Nearby Connections
    public InOutStream(Context context, String endpointId, ConnectionInfo connectionInfo) {
        this.context = context.getApplicationContext();
        this.endpointID = endpointId;
        this.connectionInfo = connectionInfo;
        currentStream = this;

        // Initialize the adapters that WifiService's StreamCopier will use
        this.inputStreamAdapter = createInputStream();
        this.outputStreamAdapter = createOutputStream();
    }

    @Override
    public void onPayloadReceived(String endpointId, Payload payload) {
        // This receives the BYTES payload from the car and queues it for the InputStream to consume
        if (payload.getType() == Payload.Type.BYTES) {
            byte[] bytes = payload.asBytes();
            if (bytes != null) {
                freeInputBuffers.offer(bytes);
            }
        }
    }

    @Override
    public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
        // Bytes payloads fire SUCCESS immediately, nothing needed here.
    }

    // --- Provide Standard Streams for WifiService ---

    public InputStream getInputStream() {
        return inputStreamAdapter;
    }

    public OutputStream getOutputStream() {
        return outputStreamAdapter;
    }

    // --- The Adapter Implementations ---

    /**
     * Converts incoming Nearby Payload byte arrays back into a continuous InputStream.
     */
    private InputStream createInputStream() {
        return new InputStream() {
            private byte[] currentChunk = null;
            private int chunkPosition = 0;

            @Override
            public int read() throws IOException {
                if (!ensureChunk()) return -1;
                return currentChunk[chunkPosition++] & 0xFF;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (!ensureChunk()) return -1;

                int available = currentChunk.length - chunkPosition;
                int toCopy = Math.min(len, available);

                System.arraycopy(currentChunk, chunkPosition, b, off, toCopy);
                chunkPosition += toCopy;
                return toCopy;
            }

            private boolean ensureChunk() {
                while (currentChunk == null || chunkPosition >= currentChunk.length) {
                    try {
                        // Blocks until the car sends a new Payload over Nearby Connections
                        currentChunk = freeInputBuffers.take();
                        chunkPosition = 0;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
                return true;
            }
        };
    }

    /**
     * Converts continuous OutputStream writes into outgoing Nearby Payloads.
     */
    private OutputStream createOutputStream() {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                write(new byte[]{(byte) b});
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                if (connectionInfo == null) return;

                // Extract the exact byte bounds
                byte[] dataToSend = new byte[len];
                System.arraycopy(b, off, dataToSend, 0, len);

                try {
                    Payload bytesPayload = Payload.fromBytes(dataToSend);
                    Nearby.getConnectionsClient(context).sendPayload(endpointID, bytesPayload);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to send payload to car", e);
                    throw new IOException("Failed to send Nearby payload", e);
                }
            }
        };
    }
}