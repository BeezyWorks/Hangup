package com.mattaniahbeezy.hangup.services;

import android.content.Context;
import android.media.AudioManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.mattaniahbeezy.common.Constants;

import java.lang.reflect.Method;

/**
 * Created by Beezy Works Studios on 6/3/2017.
 */

public class ManageCallService extends WearableListenerService {

    private static final String TAG = "ManageCallService";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "received message " + messageEvent.getPath());

        switch (messageEvent.getPath()) {
            case Constants.HANGUP_PATH:
                killCall();
                break;
            case Constants.VOLUME_UP_PATH:
                volumeUp();
                break;
            case Constants.VOLUME_DOWN_PATH:
                volumeDown();
                break;
            case Constants.EXTRA_PATH:
                extraAction();
                break;
        }
    }

    private void volumeUp() {
        Log.d(TAG, "Volume Up");
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, currentVolume + 1, 0);
    }

    private void volumeDown() {
        Log.d(TAG, "Volume Down");
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, currentVolume - 1, 0);
    }

    private void extraAction() {
        Log.d(TAG, "Extra action");
    }

    private boolean killCall() {
        try {
            // Get the boring old TelephonyManager
            TelephonyManager telephonyManager =
                    (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

            // Get the getITelephony() method
            Class classTelephony = Class.forName(telephonyManager.getClass().getName());
            Method methodGetITelephony = classTelephony.getDeclaredMethod("getITelephony");

            // Ignore that the method is supposed to be private
            methodGetITelephony.setAccessible(true);

            // Invoke getITelephony() to get the ITelephony interface
            Object telephonyInterface = methodGetITelephony.invoke(telephonyManager);

            // Get the endCall method from ITelephony
            Class telephonyInterfaceClass =
                    Class.forName(telephonyInterface.getClass().getName());
            Method methodEndCall = telephonyInterfaceClass.getDeclaredMethod("endCall");

            // Invoke endCall()
            methodEndCall.invoke(telephonyInterface);

        } catch (Exception ex) { // Many things can go wrong with reflection calls
            Log.d(TAG, "Failed to hangup " + ex.toString());
            return false;
        }
        return true;
    }
}
