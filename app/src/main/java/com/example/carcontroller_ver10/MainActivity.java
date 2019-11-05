package com.example.carcontroller_ver10;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.drm.DrmStore;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.LruCache;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.util.EventListener;
import java.util.FormatFlagsConversionMismatchException;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();

    private boolean POWER_ON = false;
    private boolean AUTO_PILOT = false;


    private ImageButton forwardBtn;
    private ImageButton rightBtn;
    private ImageButton reverseBtn;
    private ImageButton leftBtn;
    private ImageButton powerBtn;
    private ImageButton autoBtn;

    private TextView indicator;
    private TextView rfid;

    private int RFID_reading;

    private final int  FORWARD = 8;
    private final int  RIGHT = 6;
    private final int  REVERSE = 2;
    private final int  LEFT = 4;
    private final int STOP = 5;


    public BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public static boolean CONNECTED = false;
    public static final String CAR_MAC_ADDRESS = "98:D3:A1:FD:4B:1E";
    public BluetoothDevice mBTDevice;
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static StringBuilder messages = new StringBuilder();

    BluetoothConnectionService bluetoothConnectionService;
    public static final int AUTO_PILOT_MODE = 1;
    public static final int REMOTE_CONTROLLED_MODE = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        forwardBtn = findViewById(R.id.forward);
        rightBtn = findViewById(R.id.right);
        reverseBtn = findViewById(R.id.reverse);
        leftBtn = findViewById(R.id.left);
        powerBtn = findViewById(R.id.power);
        autoBtn = findViewById(R.id.auto);
        indicator = findViewById(R.id.auto_indicator);
        rfid = findViewById(R.id.rfid);

        enableBT();

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingMessage"));


        powerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!POWER_ON) {
                    if(!CONNECTED){startCarBluetoothDevice();}
                    startTheCar();
                } else {
                    stopTheCar();
                    if(CONNECTED){stopCarBluetoothDevice();}
                }
            }
        });

        autoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!AUTO_PILOT) {
                    switchToAutoPilotMode();
                } else {
                    switchToRemoteControlMode();
                }
            }
        });


        forwardBtn.setOnTouchListener(new RepeatListener(400, 100, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                move(FORWARD);
            }
        }));


        rightBtn.setOnTouchListener(new RepeatListener(400, 100, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                move(RIGHT);
            }
        }));

        reverseBtn.setOnTouchListener(new RepeatListener(400, 100, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                move(REVERSE);
            }
        }));


        leftBtn.setOnTouchListener(new RepeatListener(400, 100, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                move(LEFT);
            }
        }));

    }


    /* ----------------------------------------------------- Methods --------------------------------------*/


    private void startCarBluetoothDevice() {
        mBTDevice = bluetoothAdapter.getRemoteDevice(CAR_MAC_ADDRESS);
        bluetoothAdapter.cancelDiscovery();
        bluetoothConnectionService = new BluetoothConnectionService(this.getApplicationContext());
        bluetoothConnectionService.startClient(mBTDevice, MY_UUID_INSECURE);
        CONNECTED = true;
        toastForMe("Connected!");
    }


    private void stopCarBluetoothDevice() {
        if (bluetoothAdapter != null) {
            bluetoothConnectionService.cancel();
            CONNECTED = false;
        }
    }

    private void move(int direction){
        bluetoothConnectionService.write(direction);
    }


    private void turnOnButtons() {
        Log.d(TAG, "turnOnButtons: Turning on the buttons");
        forwardBtn.setClickable(true);
        forwardBtn.setAlpha(1.f);
        rightBtn.setClickable(true);
        rightBtn.setAlpha(1.f);
        reverseBtn.setClickable(true);
        reverseBtn.setAlpha(1.f);
        leftBtn.setClickable(true);
        leftBtn.setAlpha(1.f);
        autoBtn.setClickable(true);
        autoBtn.setAlpha(1.f);
        indicator.setVisibility(View.VISIBLE);
    }

    private void turnOffButtons() {
        Log.d(TAG, "turnOffButtons: Turning off the buttons");
        forwardBtn.setClickable(false);
        forwardBtn.setAlpha(0.2f);
        rightBtn.setClickable(false);
        rightBtn.setAlpha(0.2f);
        reverseBtn.setClickable(false);
        reverseBtn.setAlpha(0.2f);
        leftBtn.setClickable(false);
        leftBtn.setAlpha(0.2f);
        autoBtn.setClickable(false);
        autoBtn.setAlpha(0.2f);
        indicator.setVisibility(View.INVISIBLE);
    }

    private void turnOnKeypad() {
        Log.d(TAG, "turnOnButtons: Turning on the Keypad");
        forwardBtn.setClickable(true);
        forwardBtn.setAlpha(1.f);
        rightBtn.setClickable(true);
        rightBtn.setAlpha(1.f);
        reverseBtn.setClickable(true);
        reverseBtn.setAlpha(1.f);
        leftBtn.setClickable(true);
        leftBtn.setAlpha(1.f);
    }

    private void turnOffKeypad() {
        Log.d(TAG, "turnOffButtons: Turning off the Keypad");
        forwardBtn.setClickable(false);
        forwardBtn.setAlpha(0.2f);
        rightBtn.setClickable(false);
        rightBtn.setAlpha(0.2f);
        reverseBtn.setClickable(false);
        reverseBtn.setAlpha(0.2f);
        leftBtn.setClickable(false);
        leftBtn.setAlpha(0.2f);
    }


    private void stopTheCar() {
        Log.d(TAG, "stopTheCar: Stopping the car!");
        switchToRemoteControlMode();
        bluetoothConnectionService.write(STOP);
        turnOffButtons();
        POWER_ON = false;
    }

    private void switchToAutoPilotMode() {
        indicator.setText("Auto Pilot: ON");
        AUTO_PILOT = true;
        bluetoothConnectionService.write(AUTO_PILOT_MODE);
        turnOffKeypad();
    }

    private void switchToRemoteControlMode() {
        indicator.setText("Auto Pilot: OFF");
        AUTO_PILOT = false;
        bluetoothConnectionService.write(REMOTE_CONTROLLED_MODE);
        turnOnKeypad();
    }

    private void toastForMe(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
        Log.d(TAG, "toastForMe: Toast thrown for you!");
    }


    private void startTheCar() {
        Log.d(TAG, "startTheCar: Starting the car!");
        turnOnButtons();
        POWER_ON = true;
    }


    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String text = intent.getStringExtra("theMessage");
            messages.replace(0, 0, text);
            RFID_reading = Integer.parseInt(messages.toString());
            rfid.setText(RFID_reading);
            Log.d(TAG, "Incoming : " + text);
        }
    };

    public void enableBT() {
        if (bluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);
        } else {
            Toast.makeText(this, "Connected to " + bluetoothAdapter.getBondedDevices(), Toast.LENGTH_SHORT).show();
        }
    }


    /* --------------------------------------------------------------------------------------- */

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (POWER_ON)
            stopTheCar();
        if (bluetoothConnectionService != null)
        stopCarBluetoothDevice();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (POWER_ON)
            stopTheCar();
    }


}



