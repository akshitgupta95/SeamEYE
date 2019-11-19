package com.example.bindookbowler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public class BlueToothManager extends AppCompatActivity {




    private ArrayAdapter<String> mBTArrayAdapter;
    private TextView textRc, textStatus;
    private ListView mDevicesListView;
    private Button buttonSwitchToMain, buttonSwitchToViewData, buttonDiscoverDevices;

    private Handler mHandler; // Our main handler that will receive callback notifications
    private BTConnection btConnection;
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue_tooth_manager);

        mBTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        mDevicesListView = (ListView)findViewById(R.id.devicesListView);
        mDevicesListView.setAdapter(mBTArrayAdapter);
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);

        buttonDiscoverDevices = (Button) findViewById(R.id.buttonNewDevice);
        textRc = (TextView) findViewById(R.id.txtRxBuffer);
        textStatus = (TextView) findViewById(R.id.txtStatus);

        manangeContextSwitch();

        // Ask for location permission if not already allowed
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);


        mHandler = new Handler() {

            public void handleMessage(android.os.Message msg){
                if(msg.what == MESSAGE_READ){
                    String readMessage = "Trying to read msg";

                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                        if(readMessage == "") {
                            textRc.setText("Bluetooth data is not working");
                        } else {
                            textRc.setText(readMessage);
                        }
                    } catch (UnsupportedEncodingException e) {
                        Log.d("bluetooth", "Not working");
                        e.printStackTrace();
                    }
                    textRc.setText(readMessage);
                }

                if(msg.what == CONNECTING_STATUS){
                    if(msg.arg1 == 1)
                        textStatus.setText("Connected to Device: " + (String)(msg.obj));
                    else
                        textStatus.setText("Connection Failed");
                }
            }
        };

        btConnection = BTConnection.getInstance();
        btConnection.setHandler(mHandler);
        btConnection.setmBTArrayAdapter(mBTArrayAdapter);

        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            textStatus.setText("Status: Bluetooth not found");
            Toast.makeText(getApplicationContext(),"Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        else {
            buttonDiscoverDevices.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    discover(v);
                }
            });
        }

        Context context = getApplicationContext();
        btConnection.makeConnection(context);

    }

    private void manangeContextSwitch() {
        buttonSwitchToMain = (Button) findViewById(R.id.buttonMainBLE);
        buttonSwitchToViewData = (Button) findViewById(R.id.buttonViewDataBLE);

        buttonSwitchToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getApplicationContext();
                Intent mainActivity = new Intent(context, MainActivity.class);
                startActivity(mainActivity);
            }
        });

        buttonSwitchToViewData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getApplicationContext();

                Intent dataView = new Intent(context, DataView.class);
                startActivity(dataView);
            }
        });
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            Context context = getBaseContext();
            //btConnection.makeConnection(v, context);
        }
    };

    public void discover(View view){
        // Check if the device is already discovering
        Context context = view.getContext();

        if(btConnection.mBTAdapter.isDiscovering()){
            btConnection.mBTAdapter.cancelDiscovery();
            Toast.makeText(context,"Discovery stopped",Toast.LENGTH_SHORT).show();
        } else {
            if(btConnection.mBTAdapter.isEnabled()) {
                mBTArrayAdapter.clear(); // clear items
                btConnection.mBTAdapter.startDiscovery();
                Toast.makeText(context, "Discovery started", Toast.LENGTH_SHORT).show();
                context.registerReceiver(btConnection.blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                Toast.makeText(context, "Bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
