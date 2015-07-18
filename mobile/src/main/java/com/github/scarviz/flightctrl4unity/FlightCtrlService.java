package com.github.scarviz.flightctrl4unity;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.unity3d.player.UnityPlayer;

public class FlightCtrlService extends Service {
    private static final String TAG = "FlightCtrlService";
    /**
     * GameObject名のIntentキー
     */
    public static final String KEY_GAME_OBJ_NM = "KEY_GAME_OBJ_NM";
    /**
     * タッチ動作を示す値
     */
    public static final String ACTION_DOWN = "ACTION_DOWN";

    private static final String CALLBACK_NM = "onCallBack";

    private static final String DEF_GAME_OBJ_NM = "GameObject";
    private String mGameObjName;

    private GoogleApiClient mGoogleApiClient;
    private static final String PATH_TOUCH = "/touch";

    public FlightCtrlService() {
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(mConnectionCallbacks)
                .addOnConnectionFailedListener(mOnConnectionFailedListener)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if (intent != null) {
            mGameObjName = intent.getStringExtra(KEY_GAME_OBJ_NM);
        }

        // mGameObjNameが未設定の場合はデフォルト値を設定する
        if (TextUtils.isEmpty(mGameObjName)) {
            mGameObjName = DEF_GAME_OBJ_NM;
        }

        mGoogleApiClient.connect();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Wearable.MessageApi.removeListener(mGoogleApiClient, mMessageListener);
            mGoogleApiClient.disconnect();
            Log.d(TAG, "GoogleApiClient disconnect");
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Google Play Services接続コールバック
     */
    GoogleApiClient.ConnectionCallbacks mConnectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            Log.d(TAG, "GoogleApiClient onConnected");
            Wearable.MessageApi.addListener(mGoogleApiClient, mMessageListener);
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

    /**
     * メッセージリスナー
     */
    MessageApi.MessageListener mMessageListener = new MessageApi.MessageListener() {
        @Override
        public void onMessageReceived(MessageEvent messageEvent) {
            String path = messageEvent.getPath();
            Log.d(TAG, "path:" + path);
            if (TextUtils.isEmpty(path)) {
                return;
            }

            // タッチイベント発生時
            if (path.equals(PATH_TOUCH)) {
                // コールバックする
                UnityPlayer.UnitySendMessage(mGameObjName, CALLBACK_NM, ACTION_DOWN);
            }
        }
    };
}
