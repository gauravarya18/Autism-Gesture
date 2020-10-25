package com.example.gesturerecognition;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.SQLException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import android.app.Activity;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


public class MainActivity extends AppCompatActivity  implements SensorEventListener {

    private SensorManager sensorManager = null  ;
//    private SensorManager sensorManager1 = null  ;
    Sensor accelerometer;
    Sensor gyroscope;
    CacheManager cacheManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cacheManager=new CacheManager(getApplicationContext());


    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if(sensorEvent.sensor.getType()==Sensor.TYPE_GYROSCOPE) {
            Object obj=new Object();
            obj.setType(1);
            obj.setX(sensorEvent.values[0]);
            obj.setY(sensorEvent.values[1]);
            obj.setZ(sensorEvent.values[2]);
            cacheManager.addEntry(obj);
            Log.d("hey", " OnSensorChanged : Degree X " + sensorEvent.values[0] + " OnSensorChanged : Degree Y " + sensorEvent.values[1] + " OnSensorChanged : Degree Z " + sensorEvent.values[2]);
        }
        if(sensorEvent.sensor.getType()==Sensor.TYPE_ACCELEROMETER) {
            Log.d("hey", " OnSensorChanged : X " + sensorEvent.values[0] + " OnSensorChanged : Y " + sensorEvent.values[1] + " OnSensorChanged : Z " + sensorEvent.values[2]);
            Object obj=new Object();
            obj.setType(2);
            obj.setX(sensorEvent.values[0]);
            obj.setY(sensorEvent.values[1]);
            obj.setZ(sensorEvent.values[2]);
            long x=cacheManager.addEntry(obj);
            Log.d("hey1",Long.toString(x));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void OnStopAction(View view) {

        sensorManager.unregisterListener(this);

    }

    public void OnStartAction(View view) {

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        sensorManager1 = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        assert sensorManager != null;
        accelerometer =sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

//        assert sensorManager1 != null;
        gyroscope=  sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        sensorManager.registerListener(MainActivity.this,accelerometer,SensorManager.SENSOR_DELAY_NORMAL);

        sensorManager.registerListener(MainActivity.this,gyroscope,SensorManager.SENSOR_DELAY_NORMAL);


    }
}
