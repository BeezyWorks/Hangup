package com.mattaniahbeezy.common;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Created by Beezy Works Studios on 6/4/2017.
 */

public class WearMessageUtil implements GoogleApiClient.ConnectionCallbacks {
    private static final String TAG = "MessageUtil";
    private GoogleApiClient mGoogleApiClient;
    private static final long CONNECTION_TIME_OUT_MS = 60 * 60 * 1000;
    private String nodeId;
    private WearMessageOnReadyCallback readyCallback;
    private boolean hasConnected;

    public WearMessageUtil(Context context, @Nullable WearMessageOnReadyCallback readyCallback) {
        this.readyCallback = readyCallback;
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addApi(Wearable.API)
                .build();
    }

    public GoogleApiClient getGoogleApiClient(){
        return mGoogleApiClient;
    }

    public void sendMessage(String message) {
        sendMessage(message, null);
    }

    public void sendMessage(final String message, @Nullable final byte[] extraData) {
        Log.d(TAG, "attempt to send message " + message);
        if (!mGoogleApiClient.isConnected()) {
            Log.d(TAG, "googleClient not connected");
            return;
        }
        if (nodeId != null) {
            Log.d(TAG, "sending message " + message);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, message, extraData);
                }
            }).start();
        } else {
            Log.d(TAG, "no node!");
        }
    }

    public void connect() {
        mGoogleApiClient.connect();
    }

    public void disconnect() {
        mGoogleApiClient.disconnect();
        hasConnected = false;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        findNode();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private void findNode() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                nodeId = pickBestNodeId(result.getNodes());
                Log.d(TAG, "Connected to node " + nodeId);
                if (readyCallback != null && mGoogleApiClient.isConnected() && !hasConnected) {
                    readyCallback.messageReadyCallback();
                    hasConnected = true;
                }
            }
        }).start();
    }

    private String pickBestNodeId(Collection<Node> nodes) {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    public interface WearMessageOnReadyCallback {
        void messageReadyCallback();
    }
}
