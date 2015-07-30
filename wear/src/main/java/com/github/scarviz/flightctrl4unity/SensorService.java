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
        // センサー解除
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

    /**
     * センサー初期化処理
     */
    protected void initSensor() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // 加速度センサー
        sensorManager.registerListener(listener
                , sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                , SensorManager.SENSOR_DELAY_GAME);
        // 地磁気センサー
        sensorManager.registerListener(listener
                , sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
                , SensorManager.SENSOR_DELAY_GAME);
    }

    private SensorEventListener listener = new SensorEventListener() {
        /**
         * センサー値変更時処理
         * @param event
         */
        @Override
        public void onSensorChanged(SensorEvent event) {
            // 精度の低いデータは捨てる
            if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                Log.d(TAG, "SensorManager.SENSOR_STATUS_UNRELIABLE");
                return;
            }

            // 地磁気センサー、加速度センサーの値を取得する
            switch (event.sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD:
                    geomagnetic = event.values.clone();
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    gravity = event.values.clone();
                    break;
            }

            // 両方データがそろっていない場合は無視する
            if (geomagnetic == null || gravity == null) {
                Log.d(TAG, "geomagnetic or gravity is null");
                return;
            }
            SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic);
            SensorManager.getOrientation(rotationMatrix, attitude);

            float azimuth = (float) (attitude[0] * RAD2DEG);
            float pitch = (float) (attitude[1] * RAD2DEG);
            float roll = (float) (attitude[2] * RAD2DEG);
            if (mCallback != null) {
                mCallback.Update(azimuth, pitch, roll);
            }
        }

        /**
         * 精度変更時処理
         * @param sensor
         * @param accuracy
         */
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "onAccuracyChanged");
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
