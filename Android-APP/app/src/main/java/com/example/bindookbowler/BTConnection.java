package com.example.bindookbowler;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class BTConnection extends Fragment {
    private static BTConnection connectionObj;

    private final String TAG = "BluetoothConnection";
    private Handler mHandler;
    public BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevice;
    private ArrayAdapter<String> mBTArrayAdapter;

    // #defines for identifying shared types between calling functions
    public final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    public final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    public final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    public BTConnection(){
        mBTAdapter = (BluetoothAdapter) BluetoothAdapter.getDefaultAdapter();
    }

    private String btHardwareAdress =  "A4:CF:12:25:CE:E2";
    private String btHardwareName = "VP_Voor_President";

    public static BTConnection getInstance() {
        if(connectionObj == null) {
            connectionObj = new BTConnection();
        }
        return connectionObj;
    }

    public void setHandler(Handler h) {
        mHandler = h;
    }

    public void setmBTArrayAdapter(ArrayAdapter<String> adapter) {
        mBTArrayAdapter = adapter;
    }



    public void makeConnection(Context context) {
        if(!mBTAdapter.isEnabled()) {
            Toast.makeText(context, "Bluetooth not on", Toast.LENGTH_SHORT).show();
            return;
        }

        if(mBTAdapter.isEnabled()) {
            mBTAdapter.startDiscovery();
            Toast.makeText(context, "Discovery started", Toast.LENGTH_SHORT).show();
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            context.registerReceiver(blReceiverDataRecord, new IntentFilter(filter));
        }

    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    final BroadcastReceiver blReceiverDataRecord = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            Log.d("Bluetooth broadcast receiver", action);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {


                try {
                    if( device.getName().equals(btHardwareName)) {
                        Toast.makeText(context, "BlueTooth Found: " + device.getName(), Toast.LENGTH_LONG).show();
                        // Spawn a new thread to avoid blocking the GUI one

                        new Thread()
                        {
                            public void run() {
                                boolean fail = false;

                                BluetoothDevice device = mBTAdapter.getRemoteDevice(btHardwareAdress);

                                try {
                                    mBTSocket = createBluetoothSocket(device);
                                } catch (IOException e) {
                                    fail = true;
                                    // Toast.makeText(context, "Socket creation failed", Toast.LENGTH_SHORT).show();
                                }
                                // Establish the Bluetooth socket connection.
                                try {
                                    mBTSocket.connect();

                                } catch (IOException e) {
                                    try {
                                        fail = true;
                                        mBTSocket.close();
                                        mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                                .sendToTarget();
                                    } catch (IOException e2) {
                                        //insert code to deal with this
                                        //  Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                if(fail == false) {
                                    mConnectedThread = new ConnectedThread(mBTSocket);
                                    mConnectedThread.start();

                                    mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, btHardwareName)
                                            .sendToTarget();
                                }
                            }
                        }.start();
                    }
                } catch(NullPointerException e) {

                }

            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                 //Device is now connected
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                mBTAdapter.startDiscovery();
                if(device.getName() == btHardwareName) {
                    Toast.makeText(context, "BlueTooth discounted trying to reconnect: " + device.getName(), Toast.LENGTH_LONG).show();
                    new Thread() {
                        public void run() {
                            boolean fail = false;

                            BluetoothDevice device = mBTAdapter.getRemoteDevice(btHardwareAdress);

                            try {
                                mBTSocket = createBluetoothSocket(device);
                            } catch (IOException e) {
                                fail = true;
                                // Toast.makeText(context, "Socket creation failed", Toast.LENGTH_SHORT).show();
                            }
                            // Establish the Bluetooth socket connection.
                            try {
                                mBTSocket.connect();

                            } catch (IOException e) {
                                try {
                                    fail = true;
                                    mBTSocket.close();
                                    mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                            .sendToTarget();
                                } catch (IOException e2) {
                                    //insert code to deal with this
                                    //  Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                                }
                            }
                            if (fail == false) {
                                mConnectedThread = new ConnectedThread(mBTSocket);
                                mConnectedThread.start();

                                mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, btHardwareName)
                                        .sendToTarget();
                            }
                        }
                    }.start();
                }
                 //Device has disconnected
            }
        }
    };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, BTMODULEUUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection",e);
        }
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[3200];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    while(bytes < 1) {
                        bytes = mmInStream.available();
                    }
                    SystemClock.sleep(100);
//                    while(bytes < 20) {
//                        bytes = mmInStream.available();
//
//                    }

                    Log.d("BLT", "Am receving data");

                    if(bytes != 0) {
                        buffer = new byte[5000];
                         //pause and wait for rest of data. Adjust this depending on your sending speed.
                        // bytes = mmInStream.available(); // how many bytes are ready to be read?
                        // Log.d("Byte counter", "Num of bytes available: " + bytes);
                        bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch(ArrayIndexOutOfBoundsException e){
                    e.printStackTrace();
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }


}
