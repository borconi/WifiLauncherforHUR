package com.borconi.emil.wifilauncherforhur.streams;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Payload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class InOutStream extends Socket {

    private static final String TAG = "InOutStream";
    private final Context context;
    private final String endpointID;

    // The Pipe where we write Android Auto data to be sent to the Car
    private ParcelFileDescriptor.AutoCloseOutputStream outgoingStream;

    // The Stream where Google Play Services dumps data from the Car
    private volatile InputStream incomingStream;

    public InOutStream(Context context, String endpointId) {
        this.context = context.getApplicationContext();
        this.endpointID = endpointId;
    }

    // Called when connection reaches STATUS_OK
    public void startOutgoingStream() {
        try {
            ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createPipe();
            outgoingStream = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]);

            // ---> THE FIX: Wrap the raw PFD in a safe Java InputStream <---
            InputStream safeReadEnd = new ParcelFileDescriptor.AutoCloseInputStream(pipe[0]);

            // Give the safe Java stream to Google
            Payload streamPayload = Payload.fromStream(safeReadEnd);
            Nearby.getConnectionsClient(context).sendPayload(endpointID, streamPayload);

            Log.d(TAG, "Outgoing Phone->Car STREAM pipe created and sent.");
        } catch (IOException e) {
            Log.e(TAG, "Failed to create outgoing pipe", e);
        }
    }

    // Called by the Nearby PayloadCallback when the Car sends its stream
    public void setIncomingStream(InputStream is) {
        this.incomingStream = is;
        Log.d(TAG, "Incoming Car->Phone STREAM received.");
    }

    // --- Exposed Standard Streams for StreamCopier ---

    @Override
    public InputStream getInputStream() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                waitForStream();
                return incomingStream.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                waitForStream();
                return incomingStream.read(b, off, len);
            }

            private void waitForStream() throws IOException {
                // Safely block the StreamCopier thread until the Car's payload arrives
                while (incomingStream == null) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Stream wait interrupted", e);
                    }
                }
            }
        };
    }

    @Override
    public OutputStream getOutputStream() {
        // Ensure the pipe is built before StreamCopier tries to write to it
        while (outgoingStream == null) {
            try { Thread.sleep(10); } catch (Exception ignored) {}
        }
        return outgoingStream;
    }
}