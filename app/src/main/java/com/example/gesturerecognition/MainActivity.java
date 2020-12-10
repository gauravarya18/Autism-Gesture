package com.example.gesturerecognition;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
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

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    Switch st;
    TextView tv_acc,tv_gyro;

    Boolean newGestureAdded;
    float[][][] Training_Gyro;
    float[][] Recognition_Gyro = new float[3][];
    float[][][] Training_Acc;
    float[][] Recognition_Acc = new float[3][];
    float[][] Shapes_score = new float[2][2];


    private static final String MODEL_PATH_GYRO = "model_Gyro.tflite";
    private static final String MODEL_PATH_ACC = "modelAcc.tflite";
    private static final String LABEL_PATH = "labels.txt";

    private Classifier classifierAcc;
    private Classifier classifierGyro;
    private Executor executor = Executors.newSingleThreadExecutor();
    int frameSize =80;
    int hopSize =40;
    private List<String> labelList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cacheManager = new CacheManager(getApplicationContext());
        et =  findViewById(R.id.filename);
        st =  findViewById(R.id.switch1);

        tv_acc =  findViewById(R.id.results_acc);
        tv_gyro = findViewById(R.id.results_gyro);

        try {
            labelList = loadLabelList(getAssets(), LABEL_PATH);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        newGestureAdded=true;
        initTensorFlowAndLoadModelAcc();
        initTensorFlowAndLoadModelGyro();
    }

    void load(String name,Boolean isGyro)
    {


            try {

                if(isGyro)
                {
                    name="Gyro" + name;
                }

                CSVReader reader = new CSVReader(new InputStreamReader(getAssets().open(name+".csv")));
                String[] nextLine;

                while ((nextLine = reader.readNext()) != null) {
                    Object obj = new Object();
                    obj.setType(Integer.parseInt(nextLine[0]));
                    obj.setX(Float.parseFloat(nextLine[1]));
                    obj.setY(Float.parseFloat(nextLine[2]));
                    if(Integer.parseInt(nextLine[0])==2) {
                        obj.setZ(Float.parseFloat(nextLine[3])-10);
                    }
                    else
                    {
                        obj.setZ(Float.parseFloat(nextLine[3]));
                    }
                    cacheManager.addEntry(obj,1);

                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "The specified file was not found", Toast.LENGTH_SHORT).show();
            }



    }
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            Object obj = new Object();
            obj.setType(1);
            obj.setX(sensorEvent.values[0]);
            obj.setY(sensorEvent.values[1]);
            obj.setZ(sensorEvent.values[2]);
            long x= cacheManager.addEntry(obj,0);
            Log.d("hey1", Long.toString(x));
            Log.d("hey", " OnSensorChanged : Degree X " + sensorEvent.values[0] + " OnSensorChanged : Degree Y " + sensorEvent.values[1] + " OnSensorChanged : Degree Z " + sensorEvent.values[2]);
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            Log.d("hey", " OnSensorChanged : X " + sensorEvent.values[0] + " OnSensorChanged : Y " + sensorEvent.values[1] + " OnSensorChanged : Z " + sensorEvent.values[2]);
            Object obj = new Object();
            obj.setType(2);
            obj.setX(sensorEvent.values[0]);
            obj.setY(sensorEvent.values[1]);
            obj.setZ(sensorEvent.values[2]-10);
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

        if(st.isChecked())
        {
            Toast.makeText(this, "Cannot Add file in Release Mode", Toast.LENGTH_SHORT).show();

        }
        else {
            String name = et.getText().toString();
            if (name.matches("")) {
                Toast.makeText(this, "Enter a file name", Toast.LENGTH_SHORT).show();
            } else {
                checkPermission(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        STORAGE_PERMISSION_CODE);
//        String csv = (getExternalFilesDir().getAbsolutePath() + "/MyCsvFile.csv"); // Here csv file name is MyCsvFile.csv
                File file = new File(this.getFilesDir(), "Test" + et.getText() + ".csv");

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

    private String getModelResults(float[][] data,Boolean isGyro)
    {
        float [][][][]  AccData = transpose(data);
        int[] Ans = new int[labelList.size()+1];
        for(int i=0;i<AccData.length;i++)
        {
            float[][][][] Data = new float[1][frameSize][3][1];
            Data[0]=AccData[i];
            int x = classifierAcc.gestureRecognitionModel(Data,this,isGyro);
            Ans[x]+=1;
            Log.d("hey_resultsQuant",String.valueOf(i)+" "+String.valueOf(x));
        }

        String ans="";
        for(int i=0;i<labelList.size();i++)
        {
            double  d= (Ans[i]*100.0/AccData.length);
            float f = typeCasting(d);
            Log.d("hey",String.valueOf(i)+" "+String.valueOf(f));
            ans = ans + labelList.get(i) + "- " + String.valueOf(f)+"%" + "\n";
        }

        return ans;
    }
    public  void recognition()
    {
        Recognition_Gyro = cacheManager.getTestingData("1",0);
        Recognition_Acc = cacheManager.getTestingData("2",0);
        cacheManager.afterSync(0);


//        final List<Classifier.Recognition> results = classifierAcc.recognizeGesture(transpose((Recognition_Acc)));
//        final List<Classifier.Recognition> resultsGyro = classifierGyro.recognizeGesture(transpose((Recognition_Gyro)));

        String resultsAcc = getModelResults(Recognition_Acc,false);
        String resultsGyro = getModelResults(Recognition_Gyro,true);


        final DTW xx= new DTW();
        Double DTW_score_gyro ;
        Double DTW_score_acc ;
        List<String> Shapes;
        if(st.isChecked()) {
            Shapes = labelList;
        }
        else
        {
            Shapes = cacheManager.getAllGestures();
        }
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
                Shapes_score[j][0] = typeCasting(DTW_score_gyro);
                Shapes_score[j][1] = typeCasting(DTW_score_acc);

                results_gyro = results_gyro + Shapes.get(j) + " " + String.format("%.4f ", Shapes_score[j][0]) + "\n";
                results_acc = results_acc + Shapes.get(j) + " " + String.format("%.4f ", Shapes_score[j][1]) + "\n";

                if (Shapes_score[j][0] < minGyro) {
                    minGyro = Shapes_score[j][0];
                    indexForMinGyro = j;
                }

                if (Shapes_score[j][1] < minAcc) {
                    minAcc = Shapes_score[j][1];
                    indexForMinAcc = j;
                }

        }

        if(Shapes.size()!=0) {
            results_gyro = results_gyro + "Min :: " + Shapes.get(indexForMinGyro);
            results_acc = results_acc + "Min :: " + Shapes.get(indexForMinAcc);
        }
        else
        {
            results_acc = "No Data";
            results_gyro = "No Data";
        }

//            String display = "DTW_Scores_Gyro :: " + String.format("%.4f ",DTW_score_gyro[0]) + String.format("%.4f ",DTW_score_gyro[1]) + String.format("%.4f ",DTW_score_gyro[2]) + "\n" + "DTW_Scores_Acc :: " + String.format("%.4f ",DTW_score_acc[0]) + String.format("%.4f ",DTW_score_acc[1]) + String.format("%.4f ",DTW_score_acc[2]);
//        String display = "DTW-Gyro: Circle :" + String.format("%.4f ",Shapes_score[0][0]) + "  Line :" + String.format("%.4f ",Shapes_score[1][0]) +"\n"+"DTW-Acc: Circle :" + String.format("%.4f ",Shapes_score[0][1]) + "  Line :" + String.format("%.4f ",Shapes_score[1][1]) ;
//        tv.setText(display);

//        final List<Classifier.Recognition> results = classifierAcc.recognizeGesture(transpose(normalize(Recognition_Acc)));
//        final List<Classifier.Recognition> resultsGyro = classifierGyro.recognizeGesture(transpose(normalize(Recognition_Gyro)));

        tv_gyro.setText(results_gyro + "\n" + resultsGyro);
        tv_acc.setText(results_acc + "\n" + resultsAcc);

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


        float [][][][]  AccData = transpose(Recognition_Acc);
        int[] Ans = new int[labelList.size()+1];
        for(int i=0;i<AccData.length;i++)
        {
            float[][][][] Data = new float[1][frameSize][3][1];
            Data[0]=AccData[i];
            int x = classifierAcc.gestureRecognitionModel(Data,this,false);
            Ans[x]+=1;
            Log.d("hey_resultsQuant",String.valueOf(i)+" "+String.valueOf(x));
        }

        String ans="";
        for(int i=0;i<labelList.size();i++)
        {
            double  f= (Ans[i]*100.0/AccData.length);
            Log.d("hey",String.valueOf(i)+" "+String.valueOf(f));
            ans = ans + labelList.get(i) + "- " + String.valueOf(f)+"%" + "\n";
        }
        tv_acc.setText(ans);
        Log.d("hey_final",String.valueOf(Ans[0])+"    "+ String.valueOf(Ans[1])+"    "+String.valueOf(Ans[2]));
//        final List<Classifier.Recognition> results = classifierAcc.recognizeGesture(transpose((Recognition_Acc)));
//        tv_acc.setText(results.toString());
////        classifierAcc.close();
//
//
//        final List<Classifier.Recognition> resultsGyro = classifierGyro.recognizeGesture(transpose((Recognition_Gyro)));
//        tv_gyro.setText(resultsGyro.toString());
//        classifierGyro.close();



        OnSyncAction(view);


//        float[][] data = new float[3][6];
//        for(int i=0;i<3;i++)
//        {
//            for(int j=0;j<6;j++)
//            {
//                data[i][j]=j+1;
//            }
//        }
//
//        data = normalize(data);
//
//        for(int i=0;i<3;i++)
//        {
//            for(int j=0;j<6;j++)
//            {
//                Log.d("hey_nor",String.valueOf(data[i][j]));
//            }
//        }

    }


    private float[][] reverse(float[][] data)
    {
        float[][] Data = new float[data.length][data[0].length];
        for(int i=0;i<3;i++)
        {
            for(int j=data[0].length-1;j>=0;j--)
            {
                Data[i][j]=data[i][data[0].length-1-j];
            }
        }

        return Data;
    }
    private float[][][][] transpose(float[][] data)
    {
        data=reverse(data);
//        Log.d("hey+",String.valueOf(data.length)+" "+ String.valueOf(data[0].length));
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

        int numOfFrame = ((data[0].length-frameSize)/(frameSize-hopSize))+1;
        if(numOfFrame<1)
        {
            return null;
        }
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
//                    Log.d("hey++",String.valueOf(i)+" "+ String.valueOf(k)+" "+" "+ String.valueOf(0)+" == "+ " "+ String.valueOf(j+k) + " " + String.valueOf(data[0][j+k]));

//                    Log.d("hey++",String.valueOf(i)+" "+ String.valueOf(k)+" "+" "+ String.valueOf(0)+" == "+ " "+ String.valueOf(j+k));

                }
            i++;
        }


//        Log.d("hey++",String.valueOf(i)+" "+ String.valueOf(Data.length)+" "+ String.valueOf(Data[0][0].length)+" "+ String.valueOf(Data[0][0][0].length));

        return Data;
    }

    private class FetchingDataAsyncTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;
        Boolean checked;
        public FetchingDataAsyncTask() {

        }

        @Override
        protected void onPreExecute() {
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setMessage("Reading Data");
            dialog.show();
            checked =st.isChecked();
        }
        @Override
        protected Void doInBackground(Void... args) {
            // do background work here
            List<String> Shapes;
            if(checked) {
                 Shapes = labelList;
            }
            else
            {
                Shapes = cacheManager.getAllGestures();
            }
            Training_Gyro = new float[Shapes.size()][3][];
            Training_Acc = new float[Shapes.size()][3][];
            int minAccLength = 1000000;
            int minGyroLength = 1000000;

            if(checked)
            {
                for (int i = 0; i < Shapes.size(); i++) {

                        load(Shapes.get(i), false);
                        Training_Gyro[i] = cacheManager.getTestingData("1", 1);
                        Training_Acc[i] = cacheManager.getTestingData("2", 1);

                        if(Training_Acc[i][0].length<minAccLength)
                        {
                            minAccLength = Training_Acc[i][0].length;
                        }

                        if(Training_Gyro[i][0].length<minGyroLength)
                        {
                            minGyroLength = Training_Gyro[i][0].length;
                        }

                        cacheManager.afterSync(1);

                }

//                  Specially for GyroFiles !!
//                for (int i = 0; i < Shapes.size(); i++) {
//
//                        load(Shapes.get(i), true);
//                        Training_Gyro[i] = cacheManager.getTestingData("1", 1);
//                        if(Training_Gyro[i][0].length<minGyroLength)
//                        {
//                            minGyroLength = Training_Gyro[i][0].length;
//                        }
//                        cacheManager.afterSync(1);
//
//                }

                Log.d("hey",String.valueOf(minAccLength)+" "+String.valueOf(minGyroLength));
                float [][][] tempAcc =  new float[Shapes.size()][3][minAccLength];
                float [][][] tempGyro =  new float[Shapes.size()][3][minGyroLength];

                for(int i=0;i<Shapes.size();i++)
                {
                    for(int j=0;j<minAccLength;j++)
                    {
                        for(int k=0;k<3;k++)
                        {
                            tempAcc[i][k][j]=Training_Acc[i][k][j];
                        }
                    }
                }

                for(int i=0;i<Shapes.size();i++)
                {
                    for(int j=0;j<minGyroLength;j++)
                    {
                        for(int k=0;k<3;k++)
                        {
                            tempGyro[i][k][j]=Training_Gyro[i][k][j];
                        }
                    }
                }

                Training_Gyro = new float[Shapes.size()][3][];
                Training_Acc = new float[Shapes.size()][3][];

                for(int i=0;i<Shapes.size();i++)
                {
                    Training_Gyro[i]=tempGyro[i];
                    Training_Acc[i]=tempAcc[i];
                }

            }
            else {
                for (int i = 0; i < Shapes.size(); i++) {

                        CSVReader("Test"+Shapes.get(i));
                        Training_Gyro[i] = cacheManager.getTestingData("1", 1);
                        Training_Acc[i] = cacheManager.getTestingData("2", 1);
                        cacheManager.afterSync(1);

                }
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

    private float[][] removeStartingAndEnding(float[][] data)
    {
        float[][] Data = new float[data.length][data[0].length-50];
        for(int i=0;i<Data.length;i++)
        {
            for(int j=0;j<Data.length;j++)
            {
                Data[j][i]=data[j][i+25];
            }
        }

        return Data;
    }
    private float[][] normalize(float[][] data)
    {

//        data = removeStartingAndEnding(data);

        float[][] norData = new float[data.length][data[0].length];
//        float meanX = 0;
//        float meanY = 0;
//        float meanZ = 0;
//
//        float VarX = 0;
//        float VarY = 0;
//        float VarZ = 0;
//
////        Log.d("hey-Length",String.valueOf(data[0].length));
//
//        for(int j=0;j<data[0].length;j++)
//        {
//            meanX = meanX + data[0][j];
//            meanY = meanY + data[1][j];
//            meanZ = meanZ + data[2][j];
////            Log.d("hey-valZ",String.valueOf(data[2][j]));
//        }
//        meanX = meanX/data[0].length;
//        meanY = meanY/data[0].length;
//        meanZ = meanZ/data[0].length;
//
//        Log.d("hey-mean",String.valueOf(meanX)+" "+String.valueOf(meanY)+" "+String.valueOf(meanZ));
//        double power =2;
//        for(int j=0;j<data[0].length;j++)
//        {
//            VarX = VarX + (data[0][j]-meanX)*(data[0][j]-meanX);
//            VarY = VarY + (data[1][j]-meanY)*(data[1][j]-meanY);
//            VarZ = VarZ + (data[2][j]-meanZ)*(data[2][j]-meanZ);
//        }
//
//        VarX = (float)Math.sqrt(VarX/data[0].length);
//        VarY = (float)Math.sqrt(VarY/data[0].length);
//        VarZ = (float)Math.sqrt(VarZ/data[0].length);
//
//        Log.d("hey-Var",String.valueOf(VarX)+" "+String.valueOf(VarY)+" "+String.valueOf(VarZ));
        for(int j=0;j<data[0].length;j++)
        {
            norData[0][j] = (data[0][j]);
            norData[1][j] = (data[1][j]);
            norData[2][j] = (data[2][j]);
        }

//        Log.d("hey-mean",String.valueOf(meanX)+" "+String.valueOf(meanY)+" "+String.valueOf(meanZ));
        return norData;
    }

    private List<String> loadLabelList(AssetManager assetManager, String labelPath) throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }
}

