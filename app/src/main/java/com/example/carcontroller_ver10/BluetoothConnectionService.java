package com.example.carcontroller_ver10;

import android.app.ProgressDialog;

import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.UUID;


public class BluetoothConnectionService {
    private final String TAG = this.getClass().getSimpleName();
    Context mContext;
    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private static final UUID MY_UUID_INSECURE= UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private UUID deviceUUID;
    private ConnectedThread mConnectedThread;
    private ProgressDialog progressDialog;
    private MainActivity mainActivity;



    public BluetoothConnectionService(Context context, MainActivity ma ) {
        progressDialog = new ProgressDialog(ma);
        setupProgressDialog();
        mainActivity = ma;
        mContext = context;
    }

    private void setupProgressDialog(){
        // Setting Title
        progressDialog.setTitle("Bluetooth Connection");
        // Setting Message
        progressDialog.setMessage("Attempting to connect...");

        progressDialog.show();
        progressDialog.setCancelable(false);
    }




    public void startClient(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startClient: Started...");
        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread: started.");
            mmDevice = device;
            deviceUUID = uuid;
        }

        public void run(){
            BluetoothSocket tmp = null;
            Log.i(TAG, "RUN mConnectThread ");

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: "
                        +MY_UUID_INSECURE );
                tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }

            mmSocket = tmp;

            // Make a connection to the BluetoothSocket

            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
                Log.d(TAG, "run: ConnectThread connected.");
                connected(mmSocket);
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                    Log.d(TAG, "run: Closed Socket.");
                    try {
                        Thread.sleep(1000);
                        progressDialog.dismiss();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                } catch (IOException e1) {
                    Log.e(TAG, "mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                }
                Log.d(TAG, "run: ConnectThread: Could not connect to UUID: " + MY_UUID_INSECURE );

            }


        }
        public void cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client Socket.");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: close() of mmSocket in Connect thread failed. " + e.getMessage());
            }
        }
    }

    /**
     Finally the ConnectedThread which is responsible for maintaining the BTConnection, Sending the data, and
     receiving incoming data through input/output streams respectively.
     **/
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
                Log.d(TAG,"temp sockets created successfully");
                progressDialog.setMessage("Connected Successfully!");
                progressDialog.dismiss();
                MainActivity.CONNECTED = true;
            } catch (IOException e) {
                Log.e(TAG,"temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                // Read from the InputStream
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    incomingMessage.trim();
                    if (!incomingMessage.isEmpty()) {
                        Log.d(TAG, "InputStream: " + incomingMessage);
                        Intent incomingMessageIntent = new Intent("incomingMessage");
                        incomingMessageIntent.putExtra("theMessage", incomingMessage);
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingMessageIntent);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage() );
                    break;
                }
            }
        }

        //Call this from the main activity to send data to the remote device
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: Error writing to output stream. " + e.getMessage() );
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private void connected(BluetoothSocket mmSocket) {
        Log.d(TAG, "connected: Starting.");

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     */
    public void write(int mode) {
        if(MainActivity.BLUETOOTH_CONNECTION_SERVICE != null) {
            // Create temporary object
            byte[] out = ByteBuffer.allocate(4).putInt(mode).array();
            // Synchronize a copy of the ConnectedThread
            Log.d(TAG, "write: Writing to outputstream: " + mode);
            //perform the write
            mConnectedThread.write(out);
        }
    }

    public void write(String data){
        if(MainActivity.BLUETOOTH_CONNECTION_SERVICE != null) {
            byte[] out = data.getBytes();
            Log.d(TAG, "write: Writing to outputstream: " + data);
            mConnectedThread.write(out);
        }
    }

    public void cancel(){
        if( MainActivity.BLUETOOTH_CONNECTION_SERVICE != null) {
            mConnectedThread.cancel();
            MainActivity.CONNECTED =false;
        }
    }

}
