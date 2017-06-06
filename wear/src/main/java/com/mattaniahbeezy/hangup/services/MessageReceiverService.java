package com.mattaniahbeezy.hangup.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.mattaniahbeezy.common.Constants;
import com.mattaniahbeezy.hangup.MainActivity;
import com.mattaniahbeezy.hangup.R;

/**
 * Created by Beezy Works Studios on 6/4/2017.
 */

public class MessageReceiverService extends WearableListenerService {
    private static final String TAG = "MessageReciever";
    private static final int NOTIFICAITON_ID = 0;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        switch (messageEvent.getPath()) {
            case Constants.START_WATCH_APP_PATH:
                startMainActivity();
                break;
            case Constants.END_WATCH_APP_PATH:
                endMainActivity();
                break;
        }
    }

    private Intent getMainActivityIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private void startMainActivity() {
        Log.d(TAG, "received start activity command");
        startActivity(getMainActivityIntent());
        createNotification();
    }

    private void endMainActivity() {
        Log.d(TAG, "received kill activity command");
        Intent intent = getMainActivityIntent();
        intent.putExtra(MainActivity.KEY_KILL_SELF, true);
        startActivity(intent);
        killNotification();
    }

    private void createNotification() {
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, getMainActivityIntent(), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Ongoing Call")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICAITON_ID, notification);
    }

    private void killNotification(){
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(NOTIFICAITON_ID);
    }
}
