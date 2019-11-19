package com.example.bindookbowler;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class DataDir extends AppCompatActivity {

    private Button btnMenu, btnBT, btnRecord;
    private GraphView graphAcc, graphGyro;

    private ListView dirList;
    private ArrayAdapter<String> dirListTArrayAdapter;

    private Boolean onePlusOne = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_dir);


        dirListTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        dirList = (ListView) findViewById(R.id.dirListView);
        dirList.setAdapter(dirListTArrayAdapter);
        dirList.setOnItemClickListener(mDeviceClickListener);

        btnMenu = (Button) findViewById(R.id.btnMenu);
        btnBT = (Button) findViewById(R.id.btnBT);
        btnRecord = (Button) findViewById(R.id.btnRecord);
        initialize_buttons();

        graphAcc = (GraphView) findViewById(R.id.graphAcc);
        graphGyro = (GraphView) findViewById(R.id.graphGyro);

        File dataDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File fileDir = new File(dataDirectory.getAbsolutePath());

        File[] listItems = fileDir.listFiles();
        if(onePlusOne) {
            dirListTArrayAdapter.add(dataDirectory.getAbsolutePath() + "/" + Environment.DIRECTORY_DOCUMENTS);
        } else {
            dirListTArrayAdapter.add(dataDirectory.getAbsolutePath());
        }

        dirListTArrayAdapter.add(String.valueOf(listItems.length));


        for(int i = 0; i < listItems.length; i++) {
            if(listItems[i].getName().contains("csv")) {
                dirListTArrayAdapter.add(listItems[i].getName());
            }



        }
        dirListTArrayAdapter.notifyDataSetChanged();


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

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getApplicationContext();
                Intent record = new Intent(context, DataView.class);
                startActivity(record);
            }
        });

    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            Context context = getBaseContext();
            String info = ((TextView) v).getText().toString();
            String[] fileNum = info.split(";");

            File dataDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            File fileDir = new File(dataDirectory.getAbsolutePath(),info);
            try{
                FileReader fileReader = new FileReader(fileDir);
                BufferedReader csvReader = new BufferedReader(fileReader);
                String row;
                String data2 = "";

                Boolean first = false;
                ArrayList<String[]> list = new ArrayList<String[]>();
                while ((row = csvReader.readLine()) != null) {
                    String[] data = row.split(",");
                    if(first == false) {
                        first = true;
                    } else {
                        if (data.length > 1) {

                            list.add(data);
                        }
                    }
                }

                csvReader.close();
                make_graphs(list);

                Toast t = Toast.makeText(context, data2, Toast.LENGTH_LONG);
                t.show();

            } catch (FileNotFoundException e) {
                return;
            } catch (IOException e) {
                return;
            }



        }
    };

    private void make_graphs(ArrayList<String[]> data) {

        graphAcc.removeAllSeries();
        graphGyro.removeAllSeries();

        Log.d("STATE", String.valueOf(data.size()));

        DataPoint[] dataListAx = new DataPoint[data.size()];
        DataPoint[] dataListAy = new DataPoint[data.size()];
        DataPoint[] dataListAz = new DataPoint[data.size()];

        DataPoint[] dataListGx = new DataPoint[data.size()];
        DataPoint[] dataListGy = new DataPoint[data.size()];
        DataPoint[] dataListGz = new DataPoint[data.size()];

        double tI = Double.parseDouble(data.get(0)[0]);

        for(int i = 0; i < data.size(); i++) {

            try {
                // dirListTArrayAdapter.add(data.get(i)[6]);
                double t2 = Double.parseDouble(data.get(i)[0]);

                dataListAx[i] = new DataPoint(t2 - tI, Double.parseDouble(data.get(i)[1]));
                dataListAy[i] = new DataPoint(t2 - tI, Double.parseDouble(data.get(i)[2]));
                dataListAz[i] = new DataPoint(t2 - tI, Double.parseDouble(data.get(i)[3]));

                dataListGx[i] = new DataPoint(t2 - tI, Double.parseDouble(data.get(i)[4]));
                dataListGy[i] = new DataPoint(t2 - tI, Double.parseDouble(data.get(i)[5]));
                dataListGz[i] = new DataPoint(t2 - tI, Double.parseDouble(data.get(i)[6]));
            } catch (IndexOutOfBoundsException e) {
                dataListAx[i] = dataListAx[i-1];
                dataListAy[i] = dataListAy[i-1];
                dataListAz[i] = dataListAz[i-1];

                dataListGx[i] = dataListAx[i-1];
                dataListGy[i] = dataListGy[i-1];
                dataListGz[i] = dataListGz[i-1];
            }


        }

        Log.d("STATE", String.valueOf(dataListAx[0].getX()));

        LineGraphSeries<DataPoint> seriesX = new LineGraphSeries<DataPoint>(dataListAx);
        LineGraphSeries<DataPoint> seriesY = new LineGraphSeries<DataPoint>(dataListAy);
        LineGraphSeries<DataPoint> seriesZ = new LineGraphSeries<DataPoint>(dataListAz);
        seriesX.setTitle("aX");
        seriesY.setTitle("aY");
        seriesZ.setTitle("aZ");
        seriesX.setColor(Color.GREEN);
        seriesY.setColor(Color.RED);
        seriesZ.setColor(Color.BLUE);

        // activate horizontal zooming and scrolling;
        graphAcc.getViewport().setScalable(true);
        // activate vertical scrolling
        graphAcc.getViewport().setScrollable(true);
        // set manual X bounds
        graphAcc.getViewport().setXAxisBoundsManual(true);
        graphAcc.getViewport().setMinX(dataListAx[0].getX());
        graphAcc.getViewport().setMaxX(dataListAx[data.size()-1].getX());
        graphAcc.addSeries(seriesX);
        graphAcc.addSeries(seriesY);
        graphAcc.addSeries(seriesZ);
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
        graphGyro.getViewport().setMaxX(dataListGx[data.size()-1].getX());
        // activate vertical scrolling
        graphGyro.getViewport().setScrollable(true);
        // activate horizontal zooming and scrolling
        graphGyro.getViewport().setScalable(true);
        graphGyro.addSeries(seriesGx);
        graphGyro.addSeries(seriesGy);
        graphGyro.addSeries(seriesGz);
        graphGyro.getLegendRenderer().setVisible(true);
    }
}
