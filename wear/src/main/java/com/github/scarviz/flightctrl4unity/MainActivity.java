package com.github.scarviz.flightctrl4unity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends WearableActivity {
    private static final String TAG = "MainActivity";

    /**
     * タッチ動作を示す値
     */
    public static final String ACTION_DOWN = "ACTION_DOWN";
    public static final String ROLL = "ROLL";
    public static final String PITCH = "PITCH";

    private static final String PATH_TOUCH = "/touch";
    private static final String PATH_ROLL = "/roll";
    private static final String PATH_PITCH = "/pitch";

    private GoogleApiClient mGoogleApiClient;

    private BoxInsetLayout mContainerView;
    private TextView mTextView;

    String mAzimuth;
    String mPitch;
    String mRoll;

    private SensorService mBoundService;
    private boolean mIsBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(mConnectionCallbacks)
                .addOnConnectionFailedListener(mOnConnectionFailedListener)
                .build();

        mAzimuth = getString(R.string.azimuth);
        mPitch = getString(R.string.pitch);
        mRoll = getString(R.string.roll);

        startService();
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");

        if (mTimer != null) {
            Log.d(TAG, "mTimer stop");
            //タイマーの停止処理
            mTimer.cancel();
            mTimer = null;
        }

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.d(TAG, "mGoogleApiClient disconnect");
            mGoogleApiClient.disconnect();
        }

        stopService();

        super.onDestroy();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mTextView.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            mContainerView.setBackground(null);
            mTextView.setTextColor(getResources().getColor(android.R.color.black));
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Log.d(TAG, "dispatchTouchEvent");
        switch (event.getAction()) {
            // 画面タッチ時
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "dispatchTouchEvent : ACTION_DOWN");
                SendMessage(PATH_TOUCH, ACTION_DOWN);
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    /**
     * サービスを起動する
     */
    private void startService() {
        Log.d(TAG, "startService");
        Intent intent = new Intent(this, SensorService.class);
        startService(intent);
        doBindService();
    }

    /**
     * サービスを停止する
     */
    private void stopService() {
        Log.d(TAG, "stopService");
        doUnbindService();
        stopService(new Intent(this, SensorService.class));
    }

    /**
     * ServiceのBind処理
     */
    private void doBindService() {
        // Serviceとの接続を確立
        bindService(new Intent(MainActivity.this, SensorService.class), mConnection, BIND_AUTO_CREATE);
    }

    /**
     * ServiceのUnbind処理
     */
    private void doUnbindService() {
        if (mIsBound) {
            // Serviceとの接続を解除
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    private Timer mTimer = null;

    /**
     * Serviceと接続するためのコネクション
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        /**
         * Serviceと接続できた場合に呼ばれる
         * @param name
         * @param service
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            // TextToSpeechServiceのインスタンスを取得する
            mBoundService = ((SensorService.SensorServiceIBinder) service).getService();
            mIsBound = true;

            if (mBoundService != null) {
                mBoundService.SetCallback(new SensorService.Callback() {
                    @Override
                    public void Update(float azimuth, float pitch, float roll) {
                        setText(azimuth, pitch, roll);
                        setPitch(pitch);
                        setRoll(roll);
                    }
                });

                mTimer = new Timer(true);
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        SendMessage(PATH_ROLL, ROLL + "," + String.valueOf(getRoll()));
                        SendMessage(PATH_PITCH, PITCH + "," + String.valueOf(getPitch()));
                    }
                }, 300, 300);
            }
        }

        private float mPitch = 0f;
        private float mRoll = 0f;

        /**
         * Pitchを取得する
         * @return
         */
        private float getPitch() {
            synchronized (this) {
                return mPitch;
            }
        }

        /**
         * Pitchを設定する
         * @param val
         */
        private void setPitch(float val) {
            synchronized (this) {
                mPitch = val;
            }
        }

        /**
         * Rollを取得する
         * @return
         */
        private float getRoll() {
            synchronized (this) {
                return mRoll;
            }
        }

        /**
         * Rollを設定する
         * @param val
         */
        private void setRoll(float val) {
            synchronized (this) {
                mRoll = val;
            }
        }

        /**
         * Serviceとの接続が意図しないタイミングで切断された(異常系)場合に呼ばれる
         * @param name
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            mBoundService = null;
            mIsBound = false;
        }
    };

    /**
     * テキスト設定
     *
     * @param azimuth
     * @param pitch
     * @param roll
     */
    private void setText(float azimuth, float pitch, float roll) {
        String text = mAzimuth + " " + String.valueOf(azimuth)
                + "\n" + mPitch + " " + String.valueOf(pitch)
                + "\n" + mRoll + " " + String.valueOf(roll);
        mTextView.setText(text);
    }

    /**
     * 端末へメッセージを送る
     *
     * @param path
     * @param message
     */
    private void SendMessage(final String path, final String message) {
        PendingResult<NodeApi.GetConnectedNodesResult> nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);

        nodes.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                for (Node node : getConnectedNodesResult.getNodes()) {
                    PendingResult<MessageApi.SendMessageResult> messageResult =
                            Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, message.getBytes());

                    messageResult.setResultCallback(mResultCallback);
                }
            }
        });
    }

    /**
     * SendMessageコールバック
     */
    ResultCallback<MessageApi.SendMessageResult> mResultCallback = new ResultCallback<MessageApi.SendMessageResult>() {
        @Override
        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
            Status status = sendMessageResult.getStatus();
            Log.d(TAG, "Status:" + status.toString());
        }
    };

    /**
     * Google Play Services接続コールバック
     */
    GoogleApiClient.ConnectionCallbacks mConnectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            Log.d(TAG, "GoogleApiClient onConnected");
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "GoogleApiClient onConnectionSuspended");
        }
    };

    /**
     * Google Play Services接続失敗時リスナー
     */
    GoogleApiClient.OnConnectionFailedListener mOnConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.d(TAG, "GoogleApiClient onConnectionFailed");
        }
    };
}
