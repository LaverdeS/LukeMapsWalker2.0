package com.example.sebastianlaverde.lukemapwalker;

/**
 * Code strongly inspired from https://github.com/pocmo/SensorDashboard
 * Consulting also the following web pages:
 *  https://developers.google.com/android/reference/com/google/android/gms/wearable/DataMapItem
 *  https://developer.android.com/training/wearables/data-layer/assets
 *  https://stackoverflow.com/questions/39954776/send-accelerometer-data-from-android-wear-to-smarthphone
 */
import android.app.Notification;
import android.app.Service;
import android.os.IBinder;
import android.util.Log;
import android.support.wear.widget.BoxInsetLayout;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.text.DecimalFormat;
import java.util.concurrent.ScheduledExecutorService;

public class WearActivity extends WearableActivity implements SensorEventListener {
    //Code strongly inspired from
    //https://github.com/pocmo/SensorDashboard

    private TextView accelerometerTextView;
    private TextView gyroscopeTextView;
    private TextView rotVecTextView;
    private TextView significantMotionText;
    private static final String TAG = "WearActivity";
    private final static int SENS_ACCELEROMETER = Sensor.TYPE_ACCELEROMETER;
    private final static int SENS_GYROSCOPE = Sensor.TYPE_GYROSCOPE;
    private final static int SENS_GYROSCOPE_UNCALIBRATED = Sensor.TYPE_GYROSCOPE_UNCALIBRATED;
    private final static int SENS_LINEAR_ACCELERATION = Sensor.TYPE_LINEAR_ACCELERATION;
    private final static int SENS_ROTATION_VECTOR = Sensor.TYPE_ROTATION_VECTOR;
    private final static int SENS_SIGNIFICANT_MOTION = Sensor.TYPE_SIGNIFICANT_MOTION;
    private ScheduledExecutorService mScheduler;
    private DataClient client;
    private PutDataMapRequest accVec;
    private PutDataMapRequest gyroVec;
    private PutDataMapRequest orientVec;
    private float[] rotMat;
    private float[] orientation;

    SensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear);
        BoxInsetLayout wear_layout = findViewById(R.id.wear_layout);

        accelerometerTextView = (TextView) findViewById(R.id.accelerometer_text);
        gyroscopeTextView     = (TextView) findViewById(R.id.gyroscope_text);
        rotVecTextView        = (TextView) findViewById(R.id.rotation_text);
        significantMotionText = (TextView) findViewById(R.id.significant_motion_text);

        accelerometerTextView.setText("Accelerometer");
        gyroscopeTextView.setText("Gyroscope");

        // Enables Always-on
        setAmbientEnabled();
        startMeasurement();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopMeasurement();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //client.sendSensorData(event.sensor.getType(), event.accuracy, event.timestamp, event.values);
        switch (event.sensor.getType()) {
            case SENS_LINEAR_ACCELERATION:
                accelerometerTextView.setText("Accelerometer (no gravity):\n X " + new DecimalFormat("#.###").format(event.values[0]) +
                        " Y " +  new DecimalFormat("#.###").format(event.values[1]) +
                        " Z " +  new DecimalFormat("#.###").format(event.values[2]));
                accVec = PutDataMapRequest.create("/accVec");
                accVec.getDataMap().putFloatArray("accVec", event.values);
                PutDataRequest accVecSendable = accVec.asPutDataRequest();
                Task<DataItem> putAccVecTask = Wearable.getDataClient(this).putDataItem(accVecSendable);
                break;
            case SENS_GYROSCOPE:
                gyroscopeTextView.setText("Gyroscope:\n X " + new DecimalFormat("#.###").format(event.values[0]) +
                        " Y " +  new DecimalFormat("#.###").format(event.values[1]) +
                        " Z " +  new DecimalFormat("#.###").format(event.values[2]));
                gyroVec = PutDataMapRequest.create("/gyroVec");
                gyroVec.getDataMap().putFloatArray("gyroVec", event.values);
                PutDataRequest gyroVecSendable = gyroVec.asPutDataRequest();
                Task<DataItem> putGyroVecTask = Wearable.getDataClient(this).putDataItem(gyroVecSendable);
                break;
            case SENS_ROTATION_VECTOR:
                SensorManager.getRotationMatrixFromVector(rotMat, event.values);
                SensorManager.getOrientation(orientation, rotMat);
                rotVecTextView.setText("Orientation:\n Azimuth " + new DecimalFormat("#.###").format(event.values[0]) +
                        " Pitch " +  new DecimalFormat("#.###").format(event.values[1]) +
                        " Roll "  +  new DecimalFormat("#.###").format(event.values[2]));
                orientVec = PutDataMapRequest.create("/orientVec");
                orientVec.getDataMap().putFloatArray("orientVec", orientation);
                PutDataRequest orientVecSendable = orientVec.asPutDataRequest();
                Task<DataItem> putOrientVecTask = Wearable.getDataClient(this).putDataItem(orientVecSendable);
                break;
            case SENS_SIGNIFICANT_MOTION:
                significantMotionText.setText("Significant Motion: " + event.values);
        }
        //client.sendSensorData(event.sensor.getType(), event.accuracy, event.timestamp, event.values);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected void startMeasurement() {
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));

        //if(BuildConfig.DEBUG) { logAvailableSensors(); }

        Sensor accelerometerSensor = mSensorManager.getDefaultSensor((SENS_ACCELEROMETER));
        Sensor linearAccelerationSensor = mSensorManager.getDefaultSensor((SENS_LINEAR_ACCELERATION));
        Sensor gyroscopeSensor = mSensorManager.getDefaultSensor((SENS_GYROSCOPE));
        Sensor uncaibratedGyroscopeSensor = mSensorManager.getDefaultSensor((SENS_GYROSCOPE_UNCALIBRATED));
        Sensor rotationVectorSensor = mSensorManager.getDefaultSensor((SENS_ROTATION_VECTOR));

        //Register the listener
        if (mSensorManager != null) {
            if (accelerometerSensor != null) {
                mSensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.w(TAG, "No Accelerometer found");
            }
            if (linearAccelerationSensor != null) {
                mSensorManager.registerListener(this, linearAccelerationSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.d(TAG, "No Linear Acceleration Sensor found");
            }
            if (gyroscopeSensor != null) {
                mSensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.w(TAG, "No Gyroscope Sensor found");
            }
            if (uncaibratedGyroscopeSensor != null) {
                mSensorManager.registerListener(this, uncaibratedGyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.w(TAG, "No Uncalibrated Gyroscope Sensor found");
            }
            if (rotationVectorSensor != null) {
                mSensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.d(TAG, "No Rotation Vector Sensor found");
            }
        }
    }

    protected void stopMeasurement() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        if (mScheduler != null && !mScheduler.isTerminated()) {
            mScheduler.shutdown();
        }
    }
}
