package org.feup.apm.cmeb_login;


import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BluetoothConnectionService {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final String TAG = "BluetoothConnectionServ";

    private static final String appName = "UltraLink_BluetoothService";

    private final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;

    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    AlertDialog mprogressDialog;


    private ConnectedThread mConnectedThread;
    private final Handler mHandler;
    public static final int STATE_MESSAGE_RECEIVED=5;
    public static final int STATE_IMAGE_RECEIVED=6;
    public static final int STATE_VIDEO_RECEIVED=7;
    public static final int SHOW_PROGRESS_BAR=8;
    public static final int UPDATE_PROGRESS_BAR=9;
    public static final int HIDE_PROGRESS_BAR=10;

    public BluetoothConnectionService(Context context, Handler handler) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        start();
    }


    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {

        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(){
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try{
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    if (mContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "AcceptThread: This app doesn't have the needed permissions ");
                    }
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);

                Log.d(TAG, "AcceptThread: Setting up Server using: " + MY_UUID_INSECURE);
            }catch (IOException e){
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage() );
            }

            mmServerSocket = tmp;
        }

        public void run(){
            Log.d(TAG, "run: AcceptThread Running.");

            BluetoothSocket socket = null;

            try{
                // This is a blocking call and will only return on a
                // successful connection or an exception
                Log.d(TAG, "run: RFCOM server socket start.....");

                socket = mmServerSocket.accept();
                Log.d("BluetoothConnection", "Socket Accepted: " + socket.toString());


                Log.d(TAG, "run: RFCOM server socket accepted connection.");
                Message msg = mHandler.obtainMessage(AcquireFile.STATE_CONNECTED, socket);
                mHandler.sendMessage(msg);

            }catch (IOException e){
                Log.e(TAG, "AcceptThread: IOException: " + e.getMessage() );
                Message msg = mHandler.obtainMessage(AcquireFile.STATE_CONNECTION_FAILED, e.getMessage());
                if (mprogressDialog.isShowing()) {
                    mprogressDialog.dismiss();
                }
                mHandler.sendMessage(msg);
            }

            if(socket != null){
                connected(socket,mmDevice);
            }

            Log.i(TAG, "END mAcceptThread ");
        }

        public void cancel() {
            Log.d(TAG, "cancel: Canceling AcceptThread.");
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage() );
            }
        }

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    if (mContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "AcceptThread: This app doesn't have the needed permissions ");
                    }
                tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }

            mmSocket = tmp;

            // Always cancel discovery because it will slow down a connection
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (mContext.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "BluetoothConnectionService: Permission BLUETOOTH_SCAN is not granted.");
                    // Return early as permissions are missing
                    return;
                }
            }
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            if (mmSocket != null && mmSocket.isConnected()) {
                Log.d(TAG, "Device is already connected.");
                return;
            }

            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception

                assert mmSocket != null;
                mmSocket.connect();

                Message msg = mHandler.obtainMessage(AcquireFile.STATE_CONNECTED, mmSocket);
                mHandler.sendMessage(msg);

                Log.d(TAG, "run: ConnectThread connected.");
                Log.d("BluetoothConnection", "Socket Connected: " + mmSocket.toString());
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                    Log.d(TAG, "run: Closed Socket.");
                } catch (IOException e1) {
                    Log.e(TAG, "mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                }
                Message msg = mHandler.obtainMessage(AcquireFile.STATE_CONNECTION_FAILED, e.getMessage());
                if (mprogressDialog.isShowing()) {
                    mprogressDialog.dismiss();
                }
                mHandler.sendMessage(msg);
                Log.d(TAG, "run: ConnectThread: Could not connect to UUID: " + MY_UUID_INSECURE );
                return;
            }

            if (mmSocket.isConnected()) {
                connected(mmSocket, mmDevice);
            } else {
                Log.e(TAG, "connected: Socket is not connected. Aborting.");
            }
        }
        public void cancel() {
            try {
                Log.d(TAG, "cancel: Closing Client Socket.");
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: close() of mmSocket in Connectthread failed. " + e.getMessage());
            }
        }
    }



    /**
     * Start the "chat" service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    /**
     AcceptThread starts and sits waiting for a connection.
     Then ConnectThread starts and attempts to make a connection with the other devices AcceptThread.
     **/

    public void startClient(BluetoothDevice device,UUID uuid){
        Log.d(TAG, "startClient: Started.");

        //initprogress dialog
        // Show a custom dialog with a ProgressBar
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setCancelable(false); // Prevent dialog dismissal on touch outside or back press
        builder.setView(R.layout.dialog_progress); // Custom layout for progress dialog

        mprogressDialog = builder.create();
        mprogressDialog.show();

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
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

            //dismiss the progressdialog when connection is established
            try {
                if (mprogressDialog.isShowing()) {
                    mprogressDialog.dismiss();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error dismissing progress dialog: " + e.getMessage());
            }


            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){

            if (!mmSocket.isConnected()) {
                Log.e(TAG, "write: Socket is not connected.");
                return;
            }
            int dataLength=512, headerLength=0;
            boolean flag = true;
            boolean size = false;
            String type = "TEXT";


            // Keep listening to the InputStream until an exception occurs
            while (true) {
                // Read from the InputStream
                Log.d(TAG, "run: Waiting for incoming messages...");
                if(flag) {
                    try {
                        if(!size) {
                            ByteArrayOutputStream headerBuffer = new ByteArrayOutputStream();
                            int ch;
                            while ((ch = mmInStream.read()) != '\n') {
                                headerBuffer.write(ch);
                            }
                            String header = null;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                header = headerBuffer.toString(StandardCharsets.UTF_8);
                                Log.d(TAG, "run: Header received: " + header);
                            }

                            // Parse header
                            assert header != null;
                            String[] headerParts = header.split(":");
                            type = headerParts[0];
                            if (Objects.equals(type, "IMAGESIZE") || Objects.equals(type, "VIDEOSIZE")){
                                headerLength = Integer.parseInt(headerParts[1]);
                                Log.d(TAG, "run: Header received");
                                size = true;
                            }
                            else if(Objects.equals(type, "TEXT")){
                                dataLength = Integer.parseInt(headerParts[1]);
                                flag = false;
                            }

                        }
                        else {
                            byte[] payload = new byte[headerLength];
                            int totalBytesRead = 0;
                            int bytesRead;
                            while (totalBytesRead < headerLength) {
                                bytesRead = mmInStream.read(payload, totalBytesRead, headerLength - totalBytesRead);
                                if (bytesRead == -1) {
                                    throw new IOException("End of stream reached prematurely.");
                                }
                                totalBytesRead += bytesRead;
                            }
                            Log.d(TAG, "run: getting datalength");
                            String sizeString = new String(payload, StandardCharsets.UTF_8);
                            dataLength = Integer.parseInt(sizeString);
                            Log.d(TAG, "run: Image/Video size: " + dataLength);
                            Message showMsg = mHandler.obtainMessage(SHOW_PROGRESS_BAR, dataLength, 0);
                            mHandler.sendMessage(showMsg);
                            flag = false;
                            size = false;
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage());
                        break;
                    }
                }
                else {
                    try {
                        byte[] buffer = new byte[dataLength]; // Buffer to hold the complete data
                        int totalBytesRead = 0;                 // Tracks total bytes read

                        while (totalBytesRead < dataLength) {
                            // Calculate remaining bytes to read
                            int bytesRemaining = dataLength - totalBytesRead;

                            // Read into a temporary array
                            byte[] chunk = new byte[Math.min(bytesRemaining, 1024)]; // Adjust chunk size as needed
                            int bytesRead = mmInStream.read(chunk, 0, chunk.length);

                            if (bytesRead == -1) {
                                Log.e(TAG, "run: End of stream reached prematurely.");
                                break; // End of stream
                            }

                            // Copy chunk into buffer
                            System.arraycopy(chunk, 0, buffer, totalBytesRead, bytesRead);
                            totalBytesRead += bytesRead; // Update total bytes read
                            if(!Objects.equals(type, "TEXT")) {
                                Message updateMsg = mHandler.obtainMessage(UPDATE_PROGRESS_BAR, totalBytesRead, 0);
                                mHandler.sendMessage(updateMsg);
                            }
                        }

                        if (totalBytesRead == dataLength && !Objects.equals(type, "TEXT")) {

                            Message hideMsg = mHandler.obtainMessage(HIDE_PROGRESS_BAR);
                            mHandler.sendMessage(hideMsg);

                            if(Objects.equals(type, "IMAGESIZE"))
                                mHandler.obtainMessage(STATE_IMAGE_RECEIVED, buffer).sendToTarget();
                            else
                                mHandler.obtainMessage(STATE_VIDEO_RECEIVED, buffer).sendToTarget();
                            flag = true;
                        }

                        if (type.equals("TEXT")) {
                            String incomingMessage = new String(buffer, StandardCharsets.UTF_8);
                            Log.d(TAG, "run: Text received: " + incomingMessage);
                            flag = true;
                            mHandler.obtainMessage(STATE_MESSAGE_RECEIVED, incomingMessage).sendToTarget();
                        } else if (type.equals("IMAGE")) {
                            Log.d(TAG, "run: Image received, size: " + buffer.length);
                            Log.d(TAG, "run: Image received, size: " + dataLength);
                            mHandler.obtainMessage(STATE_IMAGE_RECEIVED, buffer).sendToTarget();
                        }

                    } catch (IOException e) {
                        Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage());
                        break;
                    }
                }
            }
        }

        //Called from the main activity to send data to the remote device
        public synchronized void write(byte[] bytes, String type) {
            executorService.execute(() -> {
                if (!mmSocket.isConnected()) {
                    Log.e(TAG, "write: Socket is not connected. Unable to send data.");
                    return;
                }


                String text = new String(bytes, StandardCharsets.UTF_8);
                if(Objects.equals(type, "Text"))
                    Log.d(TAG, "write: Writing to outputstream: " + text);


                try {
                    String header = type + ":" + bytes.length + "\n";
                    Log.d(TAG, "write: header is " + header);
                    mmOutStream.write(header.getBytes(StandardCharsets.UTF_8));
                    mmOutStream.flush();

                    // Send the actual data
                    mmOutStream.write(bytes);
                    mmOutStream.flush();
                } catch (IOException e) {
                    Log.e(TAG, "write: Error writing to output stream. " + e.getMessage());
                }
            });
        }

        public synchronized void write(byte[] bytes) {
            if (!mmSocket.isConnected()) {
                Log.e(TAG, "write: Socket is not connected. Unable to send data.");
                return;
            }

            String text = new String(bytes, StandardCharsets.UTF_8);
            Log.d(TAG, "write: Writing to outputstream: " + text);
            try {
                // Send the actual data
                mmOutStream.write(bytes);
                mmOutStream.flush();
                Log.d(TAG, "write: Data flushed to output stream.");
            } catch (IOException e) {
                Log.e(TAG, "write: Error writing to output stream. " + e.getMessage() );
            }
        }

        /* Can be called from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        Log.d(TAG, "connected: Starting.");

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    /**
     * Write to the ConnectedThread in an assynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[], String)
     */
    public void write(byte[] out, String head) {

        // Synchronize a copy of the ConnectedThread
        Log.d(TAG, "write: Write Called.");
        //perform the write
        mConnectedThread.write(out, head);
    }

    public void write(byte[] out) {

        // Synchronize a copy of the ConnectedThread
        Log.d(TAG, "write: Write2 Called.");
        //perform the write
        mConnectedThread.write(out);
    }


}
