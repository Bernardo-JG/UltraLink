package org.feup.apm.cmeb_login;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.feup.apm.cmeb_login.adapter.DeviceListAdapter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AcquireFile extends AppCompatActivity implements AdapterView.OnItemClickListener{
    private static final String TAG = "AcquireFile";

    BluetoothAdapter mBluetoothAdapter;

    BluetoothConnectionService mBluetoothConnection;

    Button btnSend;
    TextView status;

    private UUID UUID_device;

    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothDevice mBTDevice;

    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    public ArrayList<BluetoothDeviceInfo> mBTDevicesInfo = new ArrayList<>();

    public DeviceListAdapter mDeviceListAdapter;

    ListView lvNewDevices;

    public static final int STATE_LISTENING = 1;
    public static final int STATE_CONNECTING=2;
    public static final int STATE_CONNECTED=3;
    public static final int STATE_CONNECTION_FAILED=4;
    public static final int STATE_IMAGE_RECEIVED=6;
    public static final int STATE_VIDEO_RECEIVED=7;
    public static final int SHOW_PROGRESS_BAR=8;
    public static final int UPDATE_PROGRESS_BAR=9;
    public static final int HIDE_PROGRESS_BAR=10;
    private ProgressBar progressBar;

    ActivityResultLauncher<Intent> resultLauncher;


    // BroadcastReceiver used to detect bluetooth state changes
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            assert action != null;
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        Intent offIntent = new Intent(AcquireFile.this, AcquireFile.class);
                        startActivity(offIntent);
                        Log.d("ImageEditorActivity", "Back button pressed");
                        finish(); // Finish the activity
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");

                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        handler.postDelayed(checkBluetoothStateRunnable, 2000); // Retry after 2 seconds
                        break;
                }
            }
        }
    };

    private final Runnable checkBluetoothStateRunnable = new Runnable() {
        @Override
        public void run() {
            if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
                Log.d(TAG, "Handler: Bluetooth is ON after delay");
                mBluetoothConnection = new BluetoothConnectionService(AcquireFile.this, handler);
                displayPairedDevices();
            } else {
                Log.d(TAG, "Handler: Bluetooth is still OFF, retrying...");
                handler.postDelayed(this, 2000); // Retry again after 2 seconds
            }
        }
    };

    /**
     * Broadcast Receiver for changes made to bluetooth states such as:
     * 1) Discoverability mode on/off or expire.
     */
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            assert action != null;
            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }

            }
        }
    };






    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            assert action != null;
            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(context, "This app doesn't have the needed permissions", Toast.LENGTH_LONG).show();
                        finish();
                    }
                assert mDevice != null;
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    //inside BroadcastReceiver4
                    mBTDevice = mDevice;
                }
                //case2: creating a bone
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };

    //Attempt at making UUID retrieval dynamic (doesn't work because, even though connection is established,
    // I can't setup the bluetooth service in the device it was connected to)
    private final BroadcastReceiver mUUIDReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_UUID.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                if (uuidExtra != null) {
                    List<UUID> uuids = new ArrayList<>();
                    for (Parcelable parcelable : uuidExtra) {
                        UUID uuid = ((ParcelUuid) parcelable).getUuid();
                        uuids.add(uuid);
                        Log.d(TAG, "UUID: " + uuid.toString());
                    }
                    // Connect using the first discovered UUID (or any other logic to select a UUID)
                    if (!uuids.isEmpty()) {
                        UUID_device = uuids.get(0);
                        //UUID_device = MY_UUID_INSECURE;
                        //mBluetoothConnection = new BluetoothConnectionService(AcquireFile.this, handler);
                        mBluetoothConnection = new BluetoothConnectionService(AcquireFile.this, handler);
                        startConnection(device);
                    }
                }
                else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(context, "This app doesn't have the needed permissions", Toast.LENGTH_LONG).show();
                            return;
                        }
                    assert device != null;
                    Log.d(TAG, "No UUIDs found for device: " + device.getName());
                }
            }
        }
    };



    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        try {
            unregisterReceiver(mBroadcastReceiver1);
            unregisterReceiver(mBroadcastReceiver2);
            unregisterReceiver(mBroadcastReceiver4);
            unregisterReceiver(mUUIDReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Receiver not registered: " + e.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acquire_file);

        // Set the status bar color to match your toolbar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.bars_color));
        }

        // Optional: Ensure the text/icons are dark for better visibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        lvNewDevices = findViewById(R.id.lvNewDevices);
        lvNewDevices.setVerticalScrollBarEnabled(true);
        lvNewDevices.setSmoothScrollbarEnabled(true);
        mBTDevices = new ArrayList<>();
        btnSend = findViewById(R.id.btnSend);
        status = findViewById(R.id.status);
        progressBar = findViewById(R.id.progressBar);

        registerResult();

        // Request Bluetooth permissions
        requestBluetooth();

        // Register Broadcast Receivers
        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver1, BTIntent);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);

        // Initialize Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        btnSend.setOnClickListener(view -> {
            Log.d("AcquireFile", "Not uploading");
            pickMedia();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(AcquireFile.this, MainActivity.class);
                intent.putExtra("video_path", getIntent().getStringExtra("video_path")); // Pass the video path back
                startActivity(intent);
                Log.d("ImageEditorActivity", "Back button pressed");
                finish();
            }
        });
    }

    // Request to enable bluetooth. Can be called at any moment
    private void requestBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            });
        } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            requestEnableBluetooth.launch(enableBtIntent);
        }
    }

    // Request to enable bluetooth
    private final ActivityResultLauncher<Intent> requestEnableBluetooth =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Log.d("AcquireFile", "Bluetooth enabled");
                    initializeBluetooth();
                } else {
                    Toast.makeText(this, "Bluetooth permissions denied", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    finish(); // Exit the activity if permissions are not granted
                }
            });

    private final ActivityResultLauncher<String[]> requestMultiplePermissions =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = true;
                for (Boolean isGranted : permissions.values()) {
                    if (!isGranted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    Log.d("AcquireFile", "All permissions granted");
                    initializeBluetooth();
                } else {
                    Toast.makeText(this, "Permissions are required for Bluetooth features", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                    finish(); // Exit the activity if permissions are not granted
                }
            });
    


    // Ask to initialize Bluetooth if the user turns it off or opens the activity with it off. If on, start the bluetooth connection service
    private void initializeBluetooth() {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            mBluetoothConnection = new BluetoothConnectionService(AcquireFile.this, handler);
            displayPairedDevices();
        } else {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                // Your Bluetooth initialization code here
                Log.d("AcquireFile", "Enabling Bluetooth...");
            } else {
                Log.e("AcquireFile", "BLUETOOTH_CONNECT permission not granted");
                return;
            }
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "This app doesn't have the needed permissions", Toast.LENGTH_LONG).show();
                    finish();
                }
            startActivity(enableBTIntent);
        }

        lvNewDevices.setOnItemClickListener(AcquireFile.this);
    }

    // Allows the user to pick the media he wants to send
    private void pickMedia() {
        if (resultLauncher != null) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*"); // Allow all types
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Allow multiple selection
            resultLauncher.launch(intent);
        } else {
            Log.e("AcquireFile", "pickMedia: resultLauncher is not initialized");
            Toast.makeText(this, "Media picker not initialized. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerResult() {
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        try {
                            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                                Intent data = result.getData();

                                // Handle multiple media items
                                if (data.getClipData() != null) {
                                    int count = data.getClipData().getItemCount();
                                    for (int i = 0; i < count; i++) {
                                        Uri mediaUri = data.getClipData().getItemAt(i).getUri();
                                        handleMediaUri(mediaUri);
                                    }
                                }
                                // Handle single media item
                                else if (data.getData() != null) {
                                    Uri mediaUri = data.getData();
                                    handleMediaUri(mediaUri);
                                }
                            }
                        } catch (Exception e) {
                            Toast.makeText(AcquireFile.this, "Error processing media selection", Toast.LENGTH_SHORT).show();
                        }
                    }

                    private void handleMediaUri(Uri mediaUri) {
                        if (mediaUri != null) {
                            String mimeType = getContentResolver().getType(mediaUri);
                            if (mimeType != null) {
                                    if (mimeType.startsWith("image")) {
                                        // Handle image selection
                                        sendImage(mediaUri);
                                    } else if (mimeType.startsWith("video")) {
                                        // Handle video selection
                                        sendVideo(mediaUri);
                                    }

                            }
                        }
                    }
                }
        );
    }

    private void sendImage(Uri imageUri) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Convert image to byte array
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while (true) {
                    assert inputStream != null;
                    if ((bytesRead = inputStream.read(buffer)) == -1) break;
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();

                byte[] imageBytes = byteArrayOutputStream.toByteArray();

                // Set the size for each chunk (you can adjust the size as necessary)
                int subArraySize = 1024;

                // Send the image length first
                mBluetoothConnection.write(String.valueOf(imageBytes.length).getBytes(StandardCharsets.UTF_8), "IMAGESIZE");
                Thread.sleep(100); // thread sleep so that the first chunk isn't sent before the actual size

                int j = 0;
                // Send the image in chunks
                for (int i = 0; i < imageBytes.length; i += subArraySize) {
                    byte[] tempArray = Arrays.copyOfRange(imageBytes, i, Math.min(imageBytes.length, i + subArraySize));
                    mBluetoothConnection.write(tempArray);
                    j++;
                }
                Log.d(TAG, "sendImage: Sent " + j + " chunks");

            } catch (Exception e) {
                Log.e(TAG, "sendImage: Error sending image. " + e.getMessage());
            }
        });
    }


    private void sendVideo(Uri videoUri) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Get the video file as a byte array
                InputStream inputStream = getContentResolver().openInputStream(videoUri);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while (true) {
                    assert inputStream != null;
                    if ((bytesRead = inputStream.read(buffer)) == -1) break;
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();

                byte[] videoBytes = byteArrayOutputStream.toByteArray();

                // Send the video length first
                mBluetoothConnection.write(String.valueOf(videoBytes.length).getBytes(StandardCharsets.UTF_8), "VIDEOSIZE");
                Thread.sleep(100); // thread sleep so that the first chunk isn't sent before the actual size


                int j = 0;
                // Send the video in chunks
                for (int i = 0; i < videoBytes.length; i += 1024) {
                    byte[] tempArray = Arrays.copyOfRange(videoBytes, i, Math.min(videoBytes.length, i + 1024));
                    mBluetoothConnection.write(tempArray);
                    j++;
                }
                Log.d(TAG, "sendVideo: Sent " + j + " chunks");

            } catch (Exception e) {
                Log.e(TAG, "sendVideo: Error sending video. " + e.getMessage());
            }
        });
    }


    private void displayPairedDevices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            int check = checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT);
            if (check != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "This app doesn't have the needed permissions", Toast.LENGTH_LONG).show();
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1001); // Fixed closing parenthesis
                Log.d(TAG, "startConnection: didn't allow connection.");
                return; // Ensure the method exits if permission is not granted
            }
        }
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices(); // gets the paired devices with the device
        if (pairedDevices != null && !pairedDevices.isEmpty()) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                BluetoothDeviceInfo deviceInfo = new BluetoothDeviceInfo(device, deviceName, deviceAddress);
                mBTDevicesInfo.add(deviceInfo);
                mBTDevices.add(device);
            }
            mDeviceListAdapter = new DeviceListAdapter(this, R.layout.device_adapter_view, mBTDevicesInfo);
            lvNewDevices.setAdapter(mDeviceListAdapter);



        } else {
            Toast.makeText(this, "No paired devices found.", Toast.LENGTH_SHORT).show();
        }
    }

    //create method for starting connection
    public void startConnection(BluetoothDevice mBTDevice){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            if (mBTDevice == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    int check = checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT);
                    if (check != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "This app doesn't have the needed permissions", Toast.LENGTH_LONG).show();
                        requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1001); // Fixed closing parenthesis
                        Log.d(TAG, "startConnection: didn't allow connection.");
                        return; // Ensure the method exits if permission is not granted
                    }
                }
            }

            if (mBTDevice != null) {
                if (MY_UUID_INSECURE != null)
                    runOnUiThread(() -> startBTConnection(mBTDevice,UUID_device));
                else
                    Log.d(TAG, "startConnection: UUID null.");
            } else {
                Toast.makeText(this, "No paired device found.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * starting chat service method
     */
    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");

        if(mBluetoothConnection == null)
            mBluetoothConnection = new BluetoothConnectionService(AcquireFile.this, handler);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1001);
                return;
            }
        }

        mBluetoothConnection.startClient(device, uuid);

    }



    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "This app doesn't have the needed permissions", Toast.LENGTH_LONG).show();
                finish();
            }
        mBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: You Clicked on a device.");
        String deviceName = mBTDevices.get(i).getName();
        String deviceAddress = mBTDevices.get(i).getAddress();

        Log.d(TAG, "onItemClick: deviceName = " + deviceName);
        Log.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        //create the bond.
        Log.d(TAG, "Trying to connect with " + deviceName);
        //mBTDevices.get(i).createBond();


        //Attempt at making UUID retrieval and connection dynamic (doesn't work)
        mBTDevice = mBTDevices.get(i);
        ParcelUuid[] uuids = mBTDevice.getUuids(); // Retrieve the UUIDs

        if (uuids != null && uuids.length > 0) {
            UUID_device = MY_UUID_INSECURE; // since dynamic UUID doesnt work
            //UUID_device = uuids[0].getUuid();
            Log.d("Bluetooth", "First UUID: " + UUID_device.toString());
            startBTConnection(mBTDevice, UUID_device);
        } else {
            Log.d("Bluetooth", "No UUIDs found for the device. Fetching UUIDs...");

            if (mBTDevice.fetchUuidsWithSdp()) {
                Log.d(TAG, "Fetching UUIDs for device: " + deviceName);

                // Register a new BroadcastReceiver for the UUID fetching
                IntentFilter filterUUID = new IntentFilter(BluetoothDevice.ACTION_UUID);
                registerReceiver(mUUIDReceiver, filterUUID);
            } else {
                Log.d(TAG, "Failed to fetch UUIDs for device: " + deviceName);
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001) {
            boolean allGranted = true;
            Log.d(TAG,"Checking results");
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG,"Permission not granted");
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                // If any permission is denied, show a toast and navigate back to MainActivity
                Toast.makeText(this, "Permissions are required for this feature", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            }
            else{
                // If permissions granted, restart activity
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, AcquireFile.class);
                startActivity(intent);
                finish();
            }
        }
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case STATE_LISTENING:
                    // Update UI to indicate the device is in listening mode
                    status.setText(R.string.listening);
                    btnSend.setVisibility(View.GONE); // Hide send button while listening
                    break;

                case STATE_CONNECTING:
                    // Update UI to indicate the device is trying to connect
                    status.setText(R.string.connecting);
                    btnSend.setVisibility(View.GONE); // Hide send button while connecting
                    break;

                case STATE_CONNECTED:
                    // Update UI to indicate successful connection
                    status.setText(R.string.connected);
                    btnSend.setVisibility(View.VISIBLE); // Show send button after connection
                    Toast.makeText(AcquireFile.this, "Connected to device!", Toast.LENGTH_LONG).show();
                    break;

                case STATE_CONNECTION_FAILED:
                    // Update UI to indicate connection failure
                    status.setText(R.string.connection_failed);
                    btnSend.setVisibility(View.GONE); // Hide send button on failure
                    Toast.makeText(AcquireFile.this, "Connection failed! Check device proximity", Toast.LENGTH_LONG).show();
                    break;

                case STATE_IMAGE_RECEIVED:
                    // Handle received image bytes
                    byte[] imageBytes = (byte[]) msg.obj;
                    if (imageBytes != null) {
                        try {
                            // Save the image bytes to a file
                            File picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                            File imageFile = new File(picturesDir, "received_image_" + System.currentTimeMillis() + ".png");
                            FileOutputStream fos = new FileOutputStream(imageFile);
                            fos.write(imageBytes);
                            fos.flush();
                            fos.close();

                            // Start ImageEditorActivity with the saved image path
                            Intent imageIntent = new Intent(AcquireFile.this, ImageEditorActivity.class);
                            imageIntent.putExtra("image_path", imageFile.getAbsolutePath());
                            startActivity(imageIntent);
                        } catch (IOException e) {
                            Log.e("AcquireFile", "Error saving image: " + e.getMessage());
                        }
                    } else {
                        Log.e("AcquireFile", "No image bytes received!");
                    }
                    break;

                case STATE_VIDEO_RECEIVED:
                    // Handle received video bytes
                    byte[] videoBytes = (byte[]) msg.obj;
                    File videoFile = new File(AcquireFile.this.getFilesDir(), "received_video.mp4"); // Specify filename and directory
                    try (FileOutputStream fos = new FileOutputStream(videoFile)) {
                        fos.write(videoBytes);
                        fos.flush();
                    } catch (IOException e) {
                        Log.e(TAG, "Error saving video: " + e.getMessage());
                    }

                    // Start VideoEditorActivity with the saved video path
                    Intent videoIntent = new Intent(AcquireFile.this, VideoEditorActivity.class);
                    videoIntent.putExtra("video_path", videoFile.getAbsolutePath());
                    startActivity(videoIntent);

                    Log.d(TAG, "Video saved and playing: " + videoFile.getAbsolutePath());
                    break;

                case SHOW_PROGRESS_BAR:
                    // Show progress bar during file transfer
                    status.setText(R.string.receiving);
                    btnSend.setVisibility(View.GONE); // Hide send button while receiving
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setMax(msg.arg1); // Set maximum value for progress
                    progressBar.setProgress(0); // Reset progress
                    break;

                case UPDATE_PROGRESS_BAR:
                    // Update the progress bar with current progress
                    status.setText(R.string.receiving);
                    btnSend.setVisibility(View.GONE); // Hide send button while receiving
                    progressBar.setProgress(msg.arg1);
                    break;

                case HIDE_PROGRESS_BAR:
                    // Hide the progress bar after file transfer completes
                    progressBar.setVisibility(View.GONE);
                    break;

                default:
                    Log.e("Handler", "Unknown state: " + msg.what);
                    break;
            }
            return true;
        }
    });


}