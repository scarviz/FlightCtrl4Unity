package com.github.scarviz.flightctrl4unity;

import android.os.Bundle;
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

public class MainActivity extends WearableActivity {
    private static final String TAG = "MainActivity";

    /**
     * タッチ動作を示す値
     */
    public static final String ACTION_DOWN = "ACTION_DOWN";

    private static final String PATH_TOUCH = "/touch";

    private GoogleApiClient mGoogleApiClient;

    private BoxInsetLayout mContainerView;
    private TextView mTextView;

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

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Log.d(TAG, "mGoogleApiClient disconnect");
            mGoogleApiClient.disconnect();
        }

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
