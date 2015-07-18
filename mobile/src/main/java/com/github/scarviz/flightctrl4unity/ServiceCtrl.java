package com.github.scarviz.flightctrl4unity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

/**
 * Created by satoshi on 2015/07/18.
 */
public class ServiceCtrl {
    private static final String TAG = "ServiceCtrl";

    /**
     * サービスを起動する
     *
     * @param gameObjectNm
     */
    public void startService(String gameObjectNm) {
        Log.d(TAG, "startService");
        Activity activity = UnityPlayer.currentActivity;
        Context context = activity.getApplicationContext();

        Intent intent = new Intent(context, FlightCtrlService.class);
        intent.putExtra(FlightCtrlService.KEY_GAME_OBJ_NM, gameObjectNm);
        activity.startService(intent);
    }

    /**
     * サービスを停止する
     */
    public void stopService() {
        Log.d(TAG, "stopService");
        Activity activity = UnityPlayer.currentActivity;
        Context context = activity.getApplicationContext();
        activity.stopService(new Intent(context, FlightCtrlService.class));
    }
}
