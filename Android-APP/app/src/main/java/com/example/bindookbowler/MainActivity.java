package com.example.bindookbowler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import static java.lang.Math.abs;


public class MainActivity extends AppCompatActivity {

    private Button buttonSwitchToBluetooth, buttonSwitchToViewData, buttonSwitchToDirView;

    private BTConnection btConnection;
    private Handler mMHandler; // Our main handler that will receive callback notifications

    private static String STARTJSONTAG = "[";
    private static String TIMETAG = "{time:";
    private static String ENDTIMETAG = "},";

    private Button btnResetMax;
    private EditText edtThreshold, editGy, editAx;
    private TextView ballSpeedText, txtGy, txtAx, txtBallCounter, txtMaxAy;
    private double threshold;
    private long startTime;
    private int ballCounter;

    private boolean bowling;
    private double maxGy;
    private double maxAy;
    private double minAx;
    private double ballSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonSwitchToBluetooth = (Button) findViewById(R.id.buttonBlueTooth);
        buttonSwitchToViewData = (Button) findViewById(R.id.buttonViewData);
        buttonSwitchToDirView = (Button) findViewById(R.id.buttonDirData);

        editGy = (EditText) findViewById(R.id.editGy);
        editAx = (EditText) findViewById(R.id.editAx);

        btnResetMax = (Button) findViewById(R.id.btnResetMax);

        ballSpeedText = (TextView) findViewById(R.id.txtBallSpeed);
        txtGy = (TextView) findViewById(R.id.txtGy);
        txtAx = (TextView) findViewById(R.id.txtAx);
        txtBallCounter = (TextView) findViewById(R.id.txtBowlCounter);
        txtMaxAy = (TextView) findViewById(R.id.txtMaxAy);

        // On change function
        edtThreshold = (EditText) findViewById(R.id.edtThreshold);
        threshold = Double.valueOf(edtThreshold.getText().toString());
        bowling = false;
        maxGy = 0.0;
        minAx = 0.0;
        maxAy = 0.0;
        ballSpeed = 0.0;
        ballCounter = 0;

        mMHandler = new Handler() {
            public void handleMessage(android.os.Message msg){
                handle_message(msg);
            }
        };

        // Bluetooth interface
        btConnection = BTConnection.getInstance();
        btConnection.setHandler(mMHandler);

        Context context = getApplicationContext();
        btConnection.makeConnection(context);



        buttonSwitchToBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getApplicationContext();

                Intent blueToothManager = new Intent(context, BlueToothManager.class);
                startActivity(blueToothManager);
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

        buttonSwitchToDirView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getApplicationContext();

                Intent dataDir = new Intent(context, DataDir.class);
                startActivity(dataDir);
            }


        });

        btnResetMax.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                maxAy = 0.0;
                txtMaxAy.setText(String.valueOf(maxAy));
            }

        });

    }

    private void handle_message(android.os.Message msg) {
        //Read message

        Log.d("Main", "Should be here");
        read_incoming_data(msg);

        if(read_incoming_data(msg) < 0 ) {
            return;
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

            readMessage = new String((byte[]) msg.obj);

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

                        Log.d("main", "Reading data");

                        txtAx.setText(ax);
                        txtGy.setText(gy);

                        if(abs(Double.valueOf(ay)) > maxAy) {
                            maxAy = abs(Double.valueOf(ay));
                            txtMaxAy.setText(String.valueOf(maxAy));
                        }

                        if(abs(Double.valueOf(ay)) > Double.valueOf(edtThreshold.getText().toString()) &&
                            bowling == false) {
                            bowling = true;
                            maxGy = abs(Double.valueOf(gy));
                            minAx = Double.valueOf(ax);
                            ballSpeed = 0.0;
                            startTime = System.currentTimeMillis();
                        }

                        if(bowling) {
                            if(maxGy < abs(Double.valueOf(gy))) {
                                maxGy = abs(Double.valueOf(gy));
                            }
                            if(minAx > Double.valueOf(ax)) {
                                minAx = Double.valueOf(ax);
                            }

                            if(startTime + 7500 < System.currentTimeMillis()) {
                                bowling = false;
                                ballCounter++;
                                txtBallCounter.setText("Number Of Bowls: " + String.valueOf(ballCounter));
                                double comp1 = Double.valueOf(editGy.getText().toString()) * maxGy;
                                double comp2 = Double.valueOf(editAx.getText().toString()) * minAx;
                                ballSpeed = comp1 + comp2;
                                ballSpeedText.setText("Ballspeed = " + String.valueOf(abs(ballSpeed)) + " km/h" +
                                                      "\n Max Gy = " + String.valueOf(maxGy) +
                                                      "\n Min ax = " + String.valueOf(minAx));
                            }
                        }

                    }
                }

            } catch (JSONException e) {
                Log.d("ERROR", "Packet dropped due to JSON error");
                //e.printStackTrace();
            }

        }

        if(t != "") {
            return Integer.valueOf(t);
        } else {
            return -1;
        }
    }


}
