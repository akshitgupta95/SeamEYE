package com.example.bindookbowler;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;



import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DataView extends AppCompatActivity {

    private DataBuffer dataBuffer;
    private DataBuffer lastStored;

    private static int MSG_CONSTANT = 3;
    private static String ACCEL = "Acceleration:";
    private static String GYRO = "Gyroscope:";
    private static int RECORDLENGTH = 30000;

    private GraphView graphAcc, graphGyro;

    private TextView txtStatus, txtTime;
    private TextView txtAx, txtAy, txtAz, txtGx, txtGy, txtGz;

    private TextView txtDebug;

    private EditText recordLength, fileName;

    private Button record;

    private BTConnection btConnection;
    private Handler mHandler; // Our main handler that will receive callback notifications

    private Boolean recording, saveRecording;
    private int timeStartRecord, finishRecording;
    private int recordedData;

    private Button btnMenu, btnBT, btnDataDir;

    private DataPoint[] dataListAx, dataListAy, dataListAz;
    private DataPoint[] dataListGx, dataListGy, dataListGz;

    private static int RECORD_DATA_BUF = 15000;

    private int curTime;

    private String fN;
    private int fileIndex;

    private static String STARTJSONTAG = "[";
    private static String TIMETAG = "{time:";
    private static String ENDTIMETAG = "},";
    private static String ENDDATATAG = "}]";

    private ArrayList<DataPointBT> dataPointList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_view);

        recording = false;
        saveRecording = false;

        txtAx = (TextView) findViewById(R.id.txtAx);
        txtAy = (TextView) findViewById(R.id.txtAy);
        txtAz = (TextView) findViewById(R.id.txtAz);

        txtGx = (TextView) findViewById(R.id.txtGx);
        txtGy = (TextView) findViewById(R.id.txtGy);
        txtGz = (TextView) findViewById(R.id.txtGz);

        txtTime = (TextView) findViewById(R.id.txtTime);
        txtStatus = (TextView) findViewById(R.id.txtStatus);

        txtDebug = (TextView) findViewById(R.id.txtDebug);

        recordLength = (EditText) findViewById(R.id.edtRecordingLength);
        fileName = (EditText) findViewById(R.id.edtSaveFileName);
        record = (Button) findViewById(R.id.btnRecord);

        graphAcc = (GraphView) findViewById(R.id.graphAcc);
        graphGyro = (GraphView) findViewById(R.id.graphGyro);

        btnMenu = (Button) findViewById(R.id.btnMenu);
        btnBT = (Button) findViewById(R.id.btnBT);
        btnDataDir = (Button) findViewById(R.id.btnDataDir);

        dataPointList = new ArrayList<DataPointBT>();

        initialize_buttons();
        curTime = 0;
        recordedData = 0;

        fN = "";
        fileIndex = 0;

        record.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!recording) {
                    timeStartRecord = curTime;
                    recording = true;
                    saveRecording = false;
                    recordedData = 0;

                    String tEdt = recordLength.getText().toString();

                    int tEdit;
                    try {
                        tEdit = Integer.valueOf(tEdt);
                        if(tEdit > RECORDLENGTH) {
                            recordLength.setText(String.valueOf(RECORDLENGTH));
                            tEdit = RECORDLENGTH;
                        }
                    } catch (NumberFormatException e) {
                        recordLength.setText(String.valueOf(RECORDLENGTH));
                        tEdit = RECORDLENGTH;
                    }

                    finishRecording = timeStartRecord + tEdit;
                    dataBuffer.reset();
                    lastStored.reset();

                    graphAcc.removeAllSeries();
                    graphGyro.removeAllSeries();
                } else {
                    Toast toast = Toast.makeText(getBaseContext(), "Still Recording", Toast.LENGTH_LONG);
                    toast.show();
                }
            }

        }));

        // Should result in 10-15[s] of buffer
        dataBuffer = new DataBuffer(30000);
        lastStored = new DataBuffer(30000);

        mHandler = new Handler() {
            // https://github.com/google/gson/blob/master/UserGuide.md#TOC-Object-Examples
            public void handleMessage(android.os.Message msg){
                handle_message(msg);
            }
        };

        btConnection = BTConnection.getInstance();
        btConnection.setHandler(mHandler);

        Context context = getApplicationContext();
        btConnection.makeConnection(context);
    }


    // modify this function
    public void discover(View view){
        // Check if the device is already discovering
        Context context = view.getContext();

        if(btConnection.mBTAdapter.isDiscovering()){
            btConnection.mBTAdapter.cancelDiscovery();
            Toast.makeText(context,"Discovery stopped",Toast.LENGTH_SHORT).show();
        } else {
            if(btConnection.mBTAdapter.isEnabled()) {
                //mBTArrayAdapter.clear(); // clear items
                btConnection.mBTAdapter.startDiscovery();
                Toast.makeText(context, "Discovery started", Toast.LENGTH_SHORT).show();
                context.registerReceiver(btConnection.blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                Toast.makeText(context, "Bluetooth not on", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handle_message(android.os.Message msg) {
        //Read message

        curTime = read_incoming_data(msg);

        if(curTime == -1) {
            return;
        }

        if(recording) {
            if(curTime > finishRecording) {
                saveRecording = true;
                recording = false;
            }
        }

        if(saveRecording) {
            // save data
            int maxDataPoints = save_data();
            // create graph
            create_graphs(maxDataPoints);
            lastStored.reset();
        }
    }

    private int read_incoming_data(android.os.Message msg) {
        String t = "";

        String ax ="";
        String ay = "";
        String az = "";

        String gx = "";
        String gy = "";
        String gz = "";



        if(msg.what == BTConnection.MESSAGE_READ) {
            String readMessage = null;
//            try {

                readMessage = new String((byte[]) msg.obj);

                //Log.d("INCOMING DATA", readMessage);

                ArrayList<String> jsonList = new ArrayList<String>();
                Boolean notFinished = true;
                while(notFinished) {
                    int startJSON =  readMessage.indexOf(TIMETAG);
                    int endJSON = readMessage.indexOf(ENDTIMETAG);
                    if(startJSON < endJSON) {
                        jsonList.add(readMessage.substring(startJSON, endJSON+1));
                        readMessage = readMessage.substring(endJSON + 1);
                    } else if(endJSON < 1) {
                        notFinished = false;
                    } else {
                        readMessage = readMessage.substring(endJSON + 1);
                    }
                }


                String res = STARTJSONTAG;
                for(int i = 0; i < jsonList.size(); i++) {
                    res += jsonList.get(i) + ',';
                }
                res += "]";
                Log.d("DATA", res);

                try {
                    JSONArray data = new JSONArray(res);
                    //Log.d("Test", String.valueOf(data.length()) );
                    for (int i = 0; i < data.length() - 1; i++) {
                        if (data.get(i) != null) {
                            JSONObject data2 = data.getJSONObject(i);
                            t = data2.get("time").toString();

                            ax = data2.get("ax").toString();
                            ay = data2.get("ay").toString();
                            az = data2.get("az").toString();

                            gx = data2.get("gx").toString();
                            gy = data2.get("gy").toString();
                            gz = data2.get("gz").toString();

                            if(recording) {
                                recordedData++;
                                txtDebug.setText("recent time: " + t + "finish recording time: " + String.valueOf(finishRecording));
                                DataPointBT d = new DataPointBT(Integer.valueOf(t), Double.valueOf(ax),
                                        Double.valueOf(ay), Double.valueOf(az),
                                        Double.valueOf(gx), Double.valueOf(gy),
                                        Double.valueOf(gz));
                                dataPointList.add(d);
                                dataBuffer.put(d);
                            }
                        }
                    }

                    txtTime.setText(t);

                    txtAx.setText(ax);
                    txtAy.setText(ay);
                    txtAz.setText(az);

                    txtGx.setText(gx);
                    txtGy.setText(gy);
                    txtGz.setText(gz);

                } catch (JSONException e) {
                    Log.d("ERROR", "Packet dropped due to JSON error");
                    //e.printStackTrace();
                }

//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//                Log.d("JSON CHECK", "check check check");
//
//
//            }
        }

        if(t != "" && Integer.valueOf(t) > curTime) {
            return Integer.valueOf(t);
        } else {
            return -1;
        }
    }

    private int save_data() {
        int iDP = 0;

        dataListAx = new DataPoint[recordedData];
        dataListAy = new DataPoint[recordedData];
        dataListAz = new DataPoint[recordedData];

        dataListGx = new DataPoint[recordedData];
        dataListGy = new DataPoint[recordedData];
        dataListGz = new DataPoint[recordedData];

        int intTime = dataPointList.get(0).time;
        for(int i = 0; i < dataPointList.size(); i++) {
            DataPointBT d = dataPointList.get(i);
            int t2 = d.time-intTime;
            dataListAx[iDP] = new DataPoint(t2, d.ax);
            dataListAy[iDP] = new DataPoint(t2, d.ay);
            dataListAz[iDP] = new DataPoint(t2, d.az);
            dataListGx[iDP] = new DataPoint(t2, d.gx);
            dataListGy[iDP] = new DataPoint(t2, d.gy);
            dataListGz[iDP] = new DataPoint(t2, d.gz);
            iDP++;
        }

        recording = false;
        saveRecording = false;

        if(save()) {
            Toast toast = Toast.makeText(getBaseContext(), "Recording Finished, Save Successful", Toast.LENGTH_LONG);
            Log.d("SAVE SAVE", "Saving worked");
            toast.show();
        } else {
            Toast toast = Toast.makeText(getBaseContext(), "Recording Finished, Save Failed!!", Toast.LENGTH_LONG);
            toast.show();
        }

        lastStored.reset();
        dataPointList.clear();
        return iDP;
    }

    private void create_graphs(int iDP) {

        LineGraphSeries<DataPoint> seriesX = new LineGraphSeries<DataPoint>(dataListAx);
        LineGraphSeries<DataPoint> seriesY = new LineGraphSeries<DataPoint>(dataListAy);
        LineGraphSeries<DataPoint> seriesZ = new LineGraphSeries<DataPoint>(dataListAz);
        seriesX.setTitle("aX");
        seriesY.setTitle("aY");
        seriesZ.setTitle("aZ");
        seriesX.setColor(Color.GREEN);
        seriesY.setColor(Color.RED);
        seriesZ.setColor(Color.BLUE);

        // activate horizontal zooming and scrolling
        graphAcc.getViewport().setScalable(true);
        // activate vertical scrolling
        graphAcc.getViewport().setScrollableY(true);
        // set manual X bounds
        graphAcc.getViewport().setXAxisBoundsManual(true);
        graphAcc.getViewport().setMinX(dataListAx[0].getX());
        graphAcc.getViewport().setMaxX(dataListAx[iDP-1].getX());
        graphAcc.addSeries(seriesX);
        graphAcc.addSeries(seriesY);
        graphAcc.addSeries(seriesZ);
        // Display the legend.
        graphAcc.getLegendRenderer().setVisible(true);

        LineGraphSeries<DataPoint> seriesGx = new LineGraphSeries<DataPoint>(dataListGx);
        LineGraphSeries<DataPoint> seriesGy = new LineGraphSeries<DataPoint>(dataListGy);
        LineGraphSeries<DataPoint> seriesGz = new LineGraphSeries<DataPoint>(dataListGz);

        seriesGx.setTitle("gX");
        seriesGy.setTitle("gY");
        seriesGz.setTitle("gZ");
        seriesGx.setColor(Color.GREEN);
        seriesGy.setColor(Color.RED);
        seriesGz.setColor(Color.BLUE);

        // set manual X bounds
        graphGyro.getViewport().setXAxisBoundsManual(true);
        graphGyro.getViewport().setMinX(dataListGx[0].getX());
        graphGyro.getViewport().setMaxX(dataListGx[iDP-1].getX());
        // activate vertical scrolling
        graphGyro.getViewport().setScrollableY(true);
        // activate horizontal zooming and scrolling
        graphGyro.getViewport().setScalable(true);
        graphGyro.addSeries(seriesGx);
        graphGyro.addSeries(seriesGy);
        graphGyro.addSeries(seriesGz);
        graphGyro.getLegendRenderer().setVisible(true);
}

    private void initialize_buttons() {

        btnMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getApplicationContext();

                Intent mainActivity = new Intent(context, MainActivity.class);
                startActivity(mainActivity);
            }
        });

        btnBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getApplicationContext();

                Intent blueToothManager = new Intent(context, BlueToothManager.class);
                startActivity(blueToothManager);
            }


        });

        btnDataDir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getApplicationContext();
                Intent dataDir = new Intent(context, DataDir.class);
                startActivity(dataDir);
            }
        });

    }

    private boolean save() {
        int counter = 0;
        String header = "time,ax,ay,az,gx,gy,gz\n";

        if(fileName.getText().length() > 0) {
            String f = fileName.getText().toString();
            if(fN != f) {
                fN = f;
            }
        } else {
            Date currentTime = Calendar.getInstance().getTime();
            fN = "data_" + currentTime.toString();
            fileName.setText(fN);
        }

        String fileName = fN +  ".csv";
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(dir, fileName);

        try(FileWriter fileWriter = new FileWriter(file )) {
            fileWriter.append(header);
            for(int i = 0; i < dataPointList.size(); i++) {
                fileWriter.append(dataPointList.get(i).toFile());
                fileIndex++;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

}
