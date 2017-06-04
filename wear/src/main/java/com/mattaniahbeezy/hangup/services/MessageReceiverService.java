package com.mattaniahbeezy.hangup.services;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.mattaniahbeezy.common.Constants;
import com.mattaniahbeezy.hangup.MainActivity;

import java.nio.charset.StandardCharsets;

/**
 * Created by Beezy Works Studios on 6/4/2017.
 */

public class MessageReceiverService extends WearableListenerService {
    private static final String TAG = "MessageReciever";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        switch (messageEvent.getPath()) {
            case Constants.START_WATCH_APP_PATH:
                startMainActivity();
                break;
            case Constants.PHONE_NUMBER_PATH:
                updatePhoneNumber(messageEvent);
                break;
            case Constants.END_WATCH_APP_PATH:
                endMainActivity();
                break;
        }
    }

    private Intent getMainActivityIntent(){
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private void startMainActivity() {
        Log.d(TAG, "received start activity command");
        startActivity(getMainActivityIntent());
    }

    private void endMainActivity(){
        Log.d(TAG, "received kill activity command");
        Intent intent = getMainActivityIntent();
        intent.putExtra(MainActivity.KEY_KILL_SELF, true);
        startActivity(intent);
    }

    private void updatePhoneNumber(MessageEvent messageEvent) {
        String phoneNumber = new String(messageEvent.getData(), StandardCharsets.UTF_8);
        Log.d(TAG, "received phone number " + phoneNumber);
        Intent intent = getMainActivityIntent();
        intent.putExtra(MainActivity.KEY_NEW_PHONE, phoneNumber);
        startActivity(intent);
    }
}
