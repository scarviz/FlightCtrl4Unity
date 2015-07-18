package com.github.scarviz.flightctrl4unity;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class SensorService extends Service {
    private static final String TAG = "SensorService";
    protected final static double RAD2DEG = 180 / Math.PI;

    SensorManager sensorManager;
    float[] rotationMatrix = new float[9];
    float[] gravity = new float[3];
    float[] geomagnetic = new float[3];
    float[] attitude = new float[3];

    private Callback mCallback;

    public SensorService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
        initSensor();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        sensorManager.unregisterListener(listener);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    /**
     * Binder
     */
    private final IBinder mBinder = new SensorServiceIBinder();

    public class SensorServiceIBinder extends Binder {
        /**
         * Serviceインスタンスの取得
         */
        SensorService getService() {
            return SensorService.this;
        }
    }

    /**
     * コールバックの設定
     *
     * @param callback
     */
    public void SetCallback(Callback callback) {
        Log.d(TAG, "SetCallback");
        mCallback = callback;
    }

    protected void initSensor() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(listener
                , sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                , SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(listener
                , sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
                , SensorManager.SENSOR_DELAY_GAME);
    }

    private SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD:
                    geomagnetic = event.values.clone();
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    gravity = event.values.clone();
                    break;
            }
            if (geomagnetic != null && gravity != null) {
                SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic);
                SensorManager.getOrientation(rotationMatrix, attitude);

                if (mCallback != null) {
                    mCallback.Update((float) (attitude[0] * RAD2DEG)
                            , (float) (attitude[1] * RAD2DEG)
                            , (float) (attitude[2] * RAD2DEG));
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    /**
     * コールバック用
     */
    public interface Callback {
        /**
         * 更新処理
         */
        public void Update(float azimuth, float pitch, float roll);
    }
}
