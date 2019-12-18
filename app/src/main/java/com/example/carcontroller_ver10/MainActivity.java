package com.example.carcontroller_ver10;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();

    private boolean POWER_ON = false;
    private boolean AUTO_PILOT = false;

    private StringBuffer stringBuffer = new StringBuffer();
    private ImageButton forwardBtn;
    private ImageButton rightBtn;
    private ImageButton reverseBtn;
    private ImageButton leftBtn;
    private ImageButton powerBtn;
    private ImageButton autoBtn;
    private ImageButton bluetoothBtn;

    private ImageView connectedOrb;
    private ImageView disconnectedOrb;

    private TextView indicator;
    private TextView rfid;

    private final int  FORWARD = 8;
    private final int  RIGHT = 6;
    private final int  REVERSE = 2;
    private final int  LEFT = 4;
    private final int STOP = 5;


    public BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public static boolean CONNECTED = false;
    public static String CAR_MAC_ADDRESS;
    public BluetoothDevice mBTDevice;
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static BluetoothConnectionService BLUETOOTH_CONNECTION_SERVICE;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference forward = database.getReference("SteeringWheel").child("forward");
    DatabaseReference right = database.getReference("SteeringWheel").child("right");
    DatabaseReference reverse = database.getReference("SteeringWheel").child("reverse");
    DatabaseReference left = database.getReference("SteeringWheel").child("left");



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
        bluetoothBtn = findViewById(R.id.bluetooth);
        indicator = findViewById(R.id.auto_indicator);
        rfid = findViewById(R.id.rfid);
        connectedOrb = findViewById(R.id.connectedORb);
        disconnectedOrb = findViewById(R.id.disconnectedOrb);

        enableBT();

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingMessage"));


        powerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!POWER_ON) {
                    if(!CONNECTED){
                        startCarBluetoothDevice();
                        startTheCar();
                    }
                } else {
                    if(CONNECTED){
                        stopCarBluetoothDevice();
                        stopTheCar();
                    }
                }
            }
        });

        bluetoothBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(CONNECTED){
                    stopCarBluetoothDevice();
                    toastForMe("Disconnected");
                } else if(!CONNECTED){
                    startCarBluetoothDevice();
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


        forwardBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    move(FORWARD);
                } else if(event.getAction() == MotionEvent.ACTION_UP){
                    move(STOP);
                }
                return false;
            }
        });

        rightBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    move(RIGHT);
                } else if(event.getAction() == MotionEvent.ACTION_UP){
                    move(STOP);
                }
                return false;
            }
        });

        reverseBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    move(REVERSE);
                } else if(event.getAction() == MotionEvent.ACTION_UP){
                    move(STOP);
                }
                return false;
            }
        });

        leftBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    move(LEFT);
                } else if(event.getAction() == MotionEvent.ACTION_UP){
                    move(STOP);
                }
                return false;
            }
        });


        /* ---------------------------------------------- SteeringWheel ------------------------------------*/

        forward.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(CONNECTED) {
                    if (dataSnapshot.getValue(int.class) > 0) {
                        move(FORWARD);
                    } else if (dataSnapshot.getValue(int.class) == 0) {
                        move(STOP);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });

        right.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(CONNECTED) {
                    if (dataSnapshot.getValue(int.class) > 0) {
                        move(RIGHT);
                    } else if (dataSnapshot.getValue(int.class) == 0) {
                        move(STOP);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });

        reverse.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(CONNECTED) {
                    if (dataSnapshot.getValue(int.class) > 0) {
                        move(REVERSE);
                    } else if (dataSnapshot.getValue(int.class) == 0) {
                        move(STOP);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });

        left.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(CONNECTED) {
                    if (dataSnapshot.getValue(int.class) > 0) {
                        move(LEFT);
                    } else if (dataSnapshot.getValue(int.class) == 0) {
                        move(STOP);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", databaseError.toException());
            }
        });

        /* -------------------------------------------------------------------------------------------------*/

    }




    /* ----------------------------------------------------- Methods --------------------------------------*/


    private void startCarBluetoothDevice() {

        for (BluetoothDevice bluetoothDevice: bluetoothAdapter.getBondedDevices()
             ) {
            CAR_MAC_ADDRESS = bluetoothDevice.getAddress();
            mBTDevice = bluetoothDevice;
        }
        bluetoothAdapter.cancelDiscovery();
        BLUETOOTH_CONNECTION_SERVICE = new BluetoothConnectionService(this.getApplicationContext(), this);
        BLUETOOTH_CONNECTION_SERVICE.startClient(mBTDevice, MY_UUID_INSECURE);
        if(CONNECTED){
            connectedOrb.setVisibility(View.VISIBLE);
            disconnectedOrb.setVisibility(View.INVISIBLE);
        }
    }


    private void stopCarBluetoothDevice() {
        if (bluetoothAdapter != null) {
            BLUETOOTH_CONNECTION_SERVICE.cancel();
        }

        if (!CONNECTED){
            connectedOrb.setVisibility(View.INVISIBLE);
            disconnectedOrb.setVisibility(View.VISIBLE);
        }
    }

    private void move(int direction){
        switch (direction) {
            case FORWARD:
                BLUETOOTH_CONNECTION_SERVICE.write("F");
                break;
            case RIGHT:
                BLUETOOTH_CONNECTION_SERVICE.write("R");
                break;
            case LEFT:
                BLUETOOTH_CONNECTION_SERVICE.write("L");
                break;
            case REVERSE:
                BLUETOOTH_CONNECTION_SERVICE.write("B");
                break;
            case STOP:
                BLUETOOTH_CONNECTION_SERVICE.write("S");
                break;
        }
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
        bluetoothBtn.setClickable(true);
        bluetoothBtn.setAlpha(1.f);
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
        bluetoothBtn.setClickable(false);
        bluetoothBtn.setAlpha(0.2f);
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


    public void stopTheCar() {
        Log.d(TAG, "stopTheCar: Stopping the car!");
        switchToRemoteControlMode();
        BLUETOOTH_CONNECTION_SERVICE.write("S");
        turnOffButtons();
        POWER_ON = false;
    }

    private void switchToAutoPilotMode() {
        indicator.setText("Auto Pilot: ON");
        AUTO_PILOT = true;
        BLUETOOTH_CONNECTION_SERVICE.write("A");
        turnOffKeypad();
    }

    private void switchToRemoteControlMode() {
        indicator.setText("Auto Pilot: OFF");
        AUTO_PILOT = false;
        BLUETOOTH_CONNECTION_SERVICE.write("C");
        turnOnKeypad();
    }

    public void toastForMe(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "toastForMe: Toast thrown for you!");
    }


    public void startTheCar() {
        Log.d(TAG, "startTheCar: Starting the car!");
        turnOnButtons();
        POWER_ON = true;
    }


    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("theMessage");
            text.trim();
            stringBuffer.append(text);
            if(stringBuffer.length() >= 10) {
                rfid.setText(stringBuffer.toString());
                stringBuffer.delete(0, stringBuffer.length());
            }
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
        if (BLUETOOTH_CONNECTION_SERVICE != null)
        stopCarBluetoothDevice();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (POWER_ON && BLUETOOTH_CONNECTION_SERVICE != null)
            stopTheCar();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (CONNECTED) {
            startTheCar();
        }
    }
}



