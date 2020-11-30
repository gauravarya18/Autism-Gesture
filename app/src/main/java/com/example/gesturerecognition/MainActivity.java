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
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity  implements SensorEventListener {

    private SensorManager sensorManager = null;
    private static final int STORAGE_PERMISSION_CODE = 101;
    Sensor accelerometer;
    Sensor gyroscope;
    CacheManager cacheManager;
    EditText et;
//    Switch st;
    TextView tv_acc,tv_gyro;

    Boolean newGestureAdded;
    float[][][] Training_Gyro;
    float[][] Recognition_Gyro = new float[3][];
    float[][][] Training_Acc;
    float[][] Recognition_Acc = new float[3][];
    float[][] Shapes_score = new float[2][2];


    private static final String MODEL_PATH_GYRO = "model_Gyro.tflite";
    private static final String MODEL_PATH_ACC = "model.tflite";
    private static final String LABEL_PATH = "labels.txt";

    private Classifier classifierAcc;
    private Classifier classifierGyro;
    private Executor executor = Executors.newSingleThreadExecutor();
    int frameSize =80;
    int hopSize =40;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cacheManager = new CacheManager(getApplicationContext());
        et =  findViewById(R.id.filename);
//        st =  findViewById(R.id.switch1);
        tv_acc =  findViewById(R.id.results_acc);
        tv_gyro = findViewById(R.id.results_gyro);

        newGestureAdded=true;
        initTensorFlowAndLoadModelAcc();
        initTensorFlowAndLoadModelGyro();
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

        String name=et.getText().toString();
        if(name.matches(""))
        {
            Toast.makeText(this, "Enter a file name", Toast.LENGTH_SHORT).show();
        }
        else {
            checkPermission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    STORAGE_PERMISSION_CODE);
//        String csv = (getExternalFilesDir().getAbsolutePath() + "/MyCsvFile.csv"); // Here csv file name is MyCsvFile.csv
            File file = new File(this.getFilesDir(), et.getText() + ".csv");

            CSVWriter writer = null;
            try {
                writer = new CSVWriter(new FileWriter(file.getAbsolutePath()));

                List<String[]> data = new ArrayList<String[]>();
                data = cacheManager.getDataComplete();
                writer.writeAll(data); // data is adding to csv

                writer.close();
                cacheManager.afterSync(0);

            } catch (IOException e) {
                e.printStackTrace();
            }

            cacheManager.addGesture(name);
            et.setText("");
            newGestureAdded = true;
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
            if(newGestureAdded) {
                FetchingDataAsyncTask readData = new FetchingDataAsyncTask();
                readData.execute();
            }
            else
            {
                recognition();
            }
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
        Double DTW_score_gyro ;
        Double DTW_score_acc ;

        List<String> Shapes = cacheManager.getAllGestures();
        Shapes_score = new float[Shapes.size()][2];
        String results_gyro="";
        String results_acc="";

        float minGyro = 10000;
        int indexForMinGyro = 0;
        float minAcc = 10000;
        int indexForMinAcc = 0;
        for(int  j=0;j<Shapes.size();j++) {
//            for (int i = 0; i < 3; i++) {

                DTW_score_gyro = xx.compute(Recognition_Gyro, Training_Gyro[j]).getDistance();
                DTW_score_acc = xx.compute(Recognition_Acc, Training_Acc[j]).getDistance();
//            }
            Shapes_score[j][0]= typeCasting(DTW_score_gyro);
            Shapes_score[j][1]= typeCasting(DTW_score_acc);

            results_gyro = results_gyro + Shapes.get(j) + " "+ String.format("%.4f ",Shapes_score[j][0]) + "\n";
            results_acc = results_acc + Shapes.get(j) +" "+  String.format("%.4f ",Shapes_score[j][1]) + "\n";

            if(Shapes_score[j][0]<minGyro)
            {
                minGyro = Shapes_score[j][0];
                indexForMinGyro = j;
            }

            if(Shapes_score[j][1]<minAcc)
            {
                minAcc = Shapes_score[j][1];
                indexForMinAcc = j;
            }

        }
        results_gyro = results_gyro + "Min :: " + Shapes.get(indexForMinGyro);
        results_acc = results_acc + "Min :: " + Shapes.get(indexForMinAcc);


//            String display = "DTW_Scores_Gyro :: " + String.format("%.4f ",DTW_score_gyro[0]) + String.format("%.4f ",DTW_score_gyro[1]) + String.format("%.4f ",DTW_score_gyro[2]) + "\n" + "DTW_Scores_Acc :: " + String.format("%.4f ",DTW_score_acc[0]) + String.format("%.4f ",DTW_score_acc[1]) + String.format("%.4f ",DTW_score_acc[2]);
//        String display = "DTW-Gyro: Circle :" + String.format("%.4f ",Shapes_score[0][0]) + "  Line :" + String.format("%.4f ",Shapes_score[1][0]) +"\n"+"DTW-Acc: Circle :" + String.format("%.4f ",Shapes_score[0][1]) + "  Line :" + String.format("%.4f ",Shapes_score[1][1]) ;
//        tv.setText(display);
        tv_gyro.setText(results_gyro);
        tv_acc.setText(results_acc);
    }
    Float typeCasting(double d)
    {
//        double d=(scores[0]+scores[1]+scores[2])/3;
        return (float)d;
    }

    public void DeleteAllTables(View view) {
        cacheManager.deleteAll(et.getText().toString());
        File csvFile =  new File(this.getFilesDir(), et.getText().toString()+".csv");
        csvFile.delete();
        et.setText("");
    }

    public void Model(View view) {

        Recognition_Gyro = cacheManager.getTestingData("1",0);
        Recognition_Acc = cacheManager.getTestingData("2",0);
        cacheManager.afterSync(0);

        final List<Classifier.Recognition> results = classifierAcc.recognizeGesture(transpose((Recognition_Acc)));
        tv_acc.setText(results.toString());
//        classifierAcc.close();


//        final List<Classifier.Recognition> resultsGyro = classifierAcc.recognizeGesture(transpose(Recognition_Acc));
//        tv_gyro.setText(resultsGyro.toString());
//        classifierGyro.close();

    }


    private float[][][][] transpose(float[][] data)
    {
        Log.d("hey+",String.valueOf(data.length)+" "+ String.valueOf(data[0].length));
//        float[][][][] Data = new float[data[0].length/frameSize][frameSize][data.length][1];
//        for(int i=0;i<data[0].length/frameSize;i++)
//        {
//            for(int k=0;k<frameSize;k++) {
//                for (int j = 0; j < data.length; j++) {
//                    Data[i][k][j][0]=data[j][i*frameSize+k];
//                }
//            }
//        }
//        Log.d("hey++",String.valueOf(Data.length)+" "+ String.valueOf(Data[0].length)+" "+ String.valueOf(Data[0][0].length)+" "+ String.valueOf(Data[0][0][0].length));

        float[][][][] Data = new float[((data[0].length-frameSize)/(frameSize-hopSize))+1][frameSize][data.length][1];
        int i=0;
        for(int j=0;j<data[0].length-frameSize;j+=hopSize)
        {
            for(int k=0;k<frameSize;k++)
                {
                    for(int l=0;l<data.length;l++)
                    {
                        Data[i][k][l][0]=data[l][j+k];

                    }
//                    Log.d("hey++",String.valueOf(i)+" "+ String.valueOf(k)+" "+" "+ String.valueOf(0)+" == "+ " "+ String.valueOf(j+k));

                }
            i++;
        }


        Log.d("hey++",String.valueOf(i)+" "+ String.valueOf(Data.length)+" "+ String.valueOf(Data[0][0].length)+" "+ String.valueOf(Data[0][0][0].length));

        return Data;
    }

    private class FetchingDataAsyncTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;

        public FetchingDataAsyncTask() {

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

            List<String> Shapes = cacheManager.getAllGestures();
            Training_Gyro = new float[Shapes.size()][3][];
            Training_Acc = new float[Shapes.size()][3][];
            for(int i=0;i<Shapes.size();i++) {

                CSVReader(Shapes.get(i));
                Training_Gyro[i] = cacheManager.getTestingData("1", 1);
                Training_Acc[i] = cacheManager.getTestingData("2", 1);
                cacheManager.afterSync(1);
            }

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
            newGestureAdded=false;
            recognition();
        }
    }

    private void initTensorFlowAndLoadModelAcc() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifierAcc = TensorFlowGestureClassifier.create(
                            getAssets(),
                            MODEL_PATH_ACC,
                            LABEL_PATH);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });


    }

    private void initTensorFlowAndLoadModelGyro() {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifierGyro = TensorFlowGestureClassifier.create(
                            getAssets(),
                            MODEL_PATH_GYRO,
                            LABEL_PATH);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    private float[][] normalize(float[][] data)
    {
        float[][] norData = new float[data.length][data[0].length];
        float meanX = 0;
        float meanY = 0;
        float meanZ = 0;

        float VarX = 0;
        float VarY = 0;
        float VarZ = 0;

        for(int j=0;j<data[0].length;j++)
        {
            meanX = meanX + data[0][j];
            meanY = meanY + data[1][j];
            meanZ = meanZ + data[2][j];
        }
        meanX = meanX/data[0].length;
        meanY = meanY/data[0].length;
        meanZ = meanZ/data[0].length;

        double power =2;
        for(int j=0;j<data[0].length;j++)
        {
            VarX = VarX + (float)Math.pow(data[0][j]-meanX,power);
            VarY = VarY + (float)Math.pow(data[1][j]-meanY,power);
            VarZ = VarZ + (float)Math.pow(data[2][j]-meanZ,power);
        }

        VarX = (float)Math.sqrt(VarX/data[0].length);
        VarY = (float)Math.sqrt(VarY/data[0].length);
        VarZ = (float)Math.sqrt(VarZ/data[0].length);

        for(int j=0;j<data[0].length;j++)
        {
            norData[0][j] = (data[0][j] - meanX)/VarX;
            norData[1][j] = (data[1][j] - meanY)/VarY;
            norData[2][j] = (data[2][j] - meanZ)/VarZ;
        }

        return norData;
    }
}

