package com.example.gesturerecognition;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity  implements SensorEventListener {

    private SensorManager sensorManager = null;
    private static final int STORAGE_PERMISSION_CODE = 101;
    Sensor accelerometer;
    Sensor gyroscope;
    CacheManager cacheManager;
    EditText et;
    Switch st;
    TextView tv;

    float[][][] Training_Gyro = new float[2][3][];
    float[][] Recognition_Gyro = new float[3][];
    float[][][] Training_Acc = new float[2][3][];
    float[][] Recognition_Acc = new float[3][];

    float[][] Shapes_score = new float[2][2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cacheManager = new CacheManager(getApplicationContext());
        et =  findViewById(R.id.filename);
        st =  findViewById(R.id.switch1);
        tv =  findViewById(R.id.results);
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            Object obj = new Object();
            obj.setType(1);
            obj.setX(sensorEvent.values[0]);
            obj.setY(sensorEvent.values[1]);
            obj.setZ(sensorEvent.values[2]);
            cacheManager.addEntry(obj,0);

            Log.d("hey", " OnSensorChanged : Degree X " + sensorEvent.values[0] + " OnSensorChanged : Degree Y " + sensorEvent.values[1] + " OnSensorChanged : Degree Z " + sensorEvent.values[2]);
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            Log.d("hey", " OnSensorChanged : X " + sensorEvent.values[0] + " OnSensorChanged : Y " + sensorEvent.values[1] + " OnSensorChanged : Z " + sensorEvent.values[2]);
            Object obj = new Object();
            obj.setType(2);
            obj.setX(sensorEvent.values[0]);
            obj.setY(sensorEvent.values[1]);
            obj.setZ(sensorEvent.values[2]);
            long x = cacheManager.addEntry(obj,0);
            Log.d("hey1", Long.toString(x));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void OnStopAction(View view) {

        Toast.makeText(MainActivity.this,
                "Stop",
                Toast.LENGTH_SHORT)
                .show();
        sensorManager.unregisterListener(this);
    }

    public void OnStartAction(View view) {

        Toast.makeText(MainActivity.this,
                "Start",
                Toast.LENGTH_SHORT)
                .show();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert sensorManager != null;
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        sensorManager.registerListener(MainActivity.this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);


    }

    public void OnSyncAction(View view) {
        checkPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                STORAGE_PERMISSION_CODE);
//        String csv = (getExternalFilesDir().getAbsolutePath() + "/MyCsvFile.csv"); // Here csv file name is MyCsvFile.csv
        File file = new File(this.getFilesDir(), et.getText()+".csv");

        CSVWriter writer = null;
        try {
            writer = new CSVWriter(new FileWriter(file.getAbsolutePath()));

            List<String[]> data = new ArrayList<String[]>();
            data=cacheManager.getDataComplete();
            writer.writeAll(data); // data is adding to csv

            writer.close();
            cacheManager.afterSync(0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void CSVReader(String name)
    {
        try {
            File csvFile =  new File(this.getFilesDir(), name+".csv");
            CSVReader reader = new CSVReader(new FileReader(csvFile.getAbsolutePath()));
            String[] nextLine;

            while ((nextLine = reader.readNext()) != null) {
                Object obj = new Object();
                obj.setType(Integer.parseInt(nextLine[0]));
                obj.setX(Float.parseFloat(nextLine[1]));
                obj.setY(Float.parseFloat(nextLine[2]));
                obj.setZ(Float.parseFloat(nextLine[3]));
                cacheManager.addEntry(obj,1);

            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "The specified file was not found", Toast.LENGTH_SHORT).show();
        }
    }
    public void checkPermission(String permission, int requestCode)
    {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission)
                == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] { permission },
                    requestCode);
        }
        else {
            Toast.makeText(MainActivity.this,
                    "Permission already granted",
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super
                .onRequestPermissionsResult(requestCode,
                        permissions,
                        grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this,
                        "Storage Permission Granted",
                        Toast.LENGTH_SHORT)
                        .show();
            }
            else {
                Toast.makeText(MainActivity.this,
                        "Storage Permission Denied",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    public void testing(View view) {

//        if(!st.isChecked())
//        {
            YourAsyncTask readData = new YourAsyncTask();
            readData.execute();
//        }
//        else
//        {

//        }

    }
    public  void recognition()
    {
        Recognition_Gyro = cacheManager.getTestingData("1",0);
        Recognition_Acc = cacheManager.getTestingData("2",0);
        cacheManager.afterSync(0);
        final DTW xx= new DTW();
        Double[] DTW_score_gyro = new Double[3];
        Double[] DTW_score_acc = new Double[3];
        for(int  j=0;j<2;j++) {
            for (int i = 0; i < 3; i++) {

                DTW_score_gyro[i] = xx.compute(Recognition_Gyro[i], Training_Gyro[j][i]).getDistance();
                DTW_score_acc[i] = xx.compute(Recognition_Acc[i], Training_Acc[j][i]).getDistance();
            }
            Shapes_score[j][0]=Average(DTW_score_gyro);
            Shapes_score[j][1]=Average(DTW_score_acc);
        }


//            String display = "DTW_Scores_Gyro :: " + String.format("%.4f ",DTW_score_gyro[0]) + String.format("%.4f ",DTW_score_gyro[1]) + String.format("%.4f ",DTW_score_gyro[2]) + "\n" + "DTW_Scores_Acc :: " + String.format("%.4f ",DTW_score_acc[0]) + String.format("%.4f ",DTW_score_acc[1]) + String.format("%.4f ",DTW_score_acc[2]);
        String display = "DTW-Gyro: Circle :" + String.format("%.4f ",Shapes_score[0][0]) + "  Line :" + String.format("%.4f ",Shapes_score[1][0]) +"\n"+"DTW-Acc: Circle :" + String.format("%.4f ",Shapes_score[0][1]) + "  Line :" + String.format("%.4f ",Shapes_score[1][1]) ;
        tv.setText(display);
    }
    Float Average(Double scores[])
    {
        double d=(scores[0]+scores[1]+scores[2])/3;
        return (float)d;
    }
    private class YourAsyncTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;

        public YourAsyncTask() {

        }

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage("Reading Data");
            dialog.show();
        }
        @Override
        protected Void doInBackground(Void... args) {
            // do background work here
            CSVReader("Circle");
            Training_Gyro[0] = cacheManager.getTestingData("1",1);
            Training_Acc[0] = cacheManager.getTestingData("2",1);
            cacheManager.afterSync(1);

            CSVReader("Line");
            Training_Gyro[1] = cacheManager.getTestingData("1",1);
            Training_Acc[1] = cacheManager.getTestingData("2",1);
            cacheManager.afterSync(1);
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            // do UI work here
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            Toast.makeText(MainActivity.this,
                    "Data Trained",
                    Toast.LENGTH_SHORT)
                    .show();
            recognition();
        }
    }
}

