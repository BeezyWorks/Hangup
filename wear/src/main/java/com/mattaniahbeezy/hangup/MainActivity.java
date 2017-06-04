package com.mattaniahbeezy.hangup;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.mattaniahbeezy.common.Constants;
import com.mattaniahbeezy.common.WearMessageUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class MainActivity extends WearableActivity implements FourSideSlideLayout.SlideCompleteListener, DataApi.DataListener, WearMessageUtil.WearMessageOnReadyCallback {
    private static final String TAG = "MainActivity";
    public static final String KEY_NEW_PHONE = "key_new_phone";
    public static final String KEY_KILL_SELF = "key_kill_self";
    private TextView mTextView;
    private ImageView contactImageView;
    private WearMessageUtil messageUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();
        messageUtil = new WearMessageUtil(this, this);
        messageUtil.connect();

        mTextView = (TextView) findViewById(R.id.phoneNumber);
        contactImageView = (ImageView) findViewById(R.id.contactImageView);

        FourSideSlideLayout fourSideSlideLayout = (FourSideSlideLayout) findViewById(R.id.fourSideSlide);
        fourSideSlideLayout.setCompletionListener(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra(KEY_KILL_SELF) && intent.getBooleanExtra(KEY_KILL_SELF, false)) {
            Log.d(TAG, "received kill self command");
            finish();
        }
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
        contactImageView.setVisibility(isAmbient() ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(messageUtil.getGoogleApiClient(), this);
        messageUtil.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        messageUtil.connect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "Data changed");
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED &&
                    event.getDataItem().getUri().getPath().equals(Constants.DATA_CONTACT_PATH)) {
                processDataItem(event.getDataItem());
            }
        }
    }

    private void processDataItem(DataItem dataItem) {
        DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
        Asset profileAsset = dataMapItem.getDataMap().getAsset(Constants.CONTACT_IMAGE_ASSET);
        if(profileAsset!=null) {
            new LoadBitmapAssetTask().execute(profileAsset);
        }
        else{
            contactImageView.setImageDrawable(null);
        }
        String contactName = dataMapItem.getDataMap().getString(Constants.CONTACT_NAME_ASSET);
        Log.d(TAG, "Contact name from data " + contactName);
        if (contactName != null) {
            mTextView.setText(contactName);
        }
    }

    @Override
    public void onLeftSlide() {
        messageUtil.sendMessage(Constants.HANGUP_PATH);
    }

    @Override
    public void onRightSlide() {

    }

    @Override
    public void onTopSlide() {
        messageUtil.sendMessage(Constants.VOLUME_DOWN_PATH);
    }

    @Override
    public void onBottomSlide() {
        messageUtil.sendMessage(Constants.VOLUME_UP_PATH);
    }


    private class LoadBitmapAssetTask extends AsyncTask<Asset, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Asset... assets) {
            Asset asset = assets[0];
            Bitmap bitmap = null;
            Log.d(TAG, "Loading bitmap asset");
            if (asset == null) {
                return null;
            }
            final long timeOutMS = 60 * 60 * 1000;
            ConnectionResult result = messageUtil.getGoogleApiClient().blockingConnect(timeOutMS, TimeUnit.MILLISECONDS);
            if (!result.isSuccess()) {
                return null;
            }
            try {
                // convert asset into a file descriptor and block until it's ready
                InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                        messageUtil.getGoogleApiClient(), asset).await().getInputStream();

                if (assetInputStream == null) {
                    Log.w(TAG, "Requested an unknown Asset.");
                    return null;
                }
                // decode the stream into a bitmap
                bitmap = BitmapFactory.decodeStream(assetInputStream);
                assetInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            contactImageView.setImageBitmap(bitmap);
        }
    }

    @Override
    public void messageReadyCallback() {
        Wearable.DataApi.addListener(messageUtil.getGoogleApiClient(), this);
        Wearable.DataApi.getDataItems(messageUtil.getGoogleApiClient()).setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(@NonNull DataItemBuffer dataItems) {
                for (DataItem dataItem : dataItems) {
                    if (dataItem.getUri().getPath().contains(Constants.DATA_CONTACT_PATH)) {
                        processDataItem(dataItem);
                    }
                }
            }
        });
    }
}
