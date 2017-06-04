package com.mattaniahbeezy.hangup.receivers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.mattaniahbeezy.common.Constants;
import com.mattaniahbeezy.common.WearMessageUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Beezy Works Studios on 6/3/2017.
 */

public class CallBroadcastReceiver extends BroadcastReceiver implements WearMessageUtil.WearMessageOnReadyCallback {
    private static final String TAG = "CallBrodcast";
    private static final String STORE_NUMBER_KEY = "key_storenumber";
    private WearMessageUtil messageUtil;
    private Context context;

    private enum Action {START_WATCH, UPDATE_NUMBER, END_WATCH, NOTHING}

    private Action action;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        messageUtil = new WearMessageUtil(context, this);
        if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            Log.d(TAG, "PhoneStateReceiver**Call State=" + state);

            if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                storePhoneNumber(null);
                action = Action.END_WATCH;
                Log.d(TAG, "PhoneStateReceiver**Idle");
            } else if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                // Incoming call
                action = Action.UPDATE_NUMBER;
                String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                storePhoneNumber(phoneNumber);
                Log.d(TAG, "PhoneStateReceiver**Incoming call " + phoneNumber);

            } else if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                action = Action.START_WATCH;
                Log.d(TAG, "PhoneStateReceiver **Offhook");
            }
        } else if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
            // Outgoing call
            action = Action.START_WATCH;
            String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            storePhoneNumber(phoneNumber);
            Log.d(TAG, "PhoneStateReceiver **Outgoing call " + phoneNumber);

        } else {
            action = Action.NOTHING;
            Log.d(TAG, "PhoneStateReceiver **unexpected intent.action=" + intent.getAction());
        }

        if (action != Action.NOTHING) {
            messageUtil.connect();
        }
    }

    @SuppressLint("ApplySharedPref")
    private void storePhoneNumber(String phoneNumber) {
        SharedPreferences.Editor sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context).edit();
        sharedPreferences.putString(STORE_NUMBER_KEY, phoneNumber);
        sharedPreferences.commit();
    }

    private String getStoredPhoneNumber() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(STORE_NUMBER_KEY, null);
    }

    @Override
    public void messageReadyCallback() {
        switch (action) {
            case START_WATCH:
                messageUtil.sendMessage(Constants.START_WATCH_APP_PATH);
                break;
            case END_WATCH:
                messageUtil.sendMessage(Constants.END_WATCH_APP_PATH);
                break;
        }
        String phoneNumber = getStoredPhoneNumber();
        Log.d(TAG, "Phone number " + phoneNumber);
        if (phoneNumber != null) {
            Log.d(TAG, "getting contact information");
            getContactInformation(phoneNumber);
        } else {
            putNullBitmapAsset();
        }
    }

    private void getContactInformation(String phoneNumber) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            putContactName(phoneNumber);
        }

        // encode the phone number and build the filter URI
        Uri contactUri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));

        // query time
        Cursor cursor = context.getContentResolver().query(contactUri, new String[]{
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup._ID}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            // Get values from contacts database:
            String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            putContactName(name);
            // Get photo of contactId as input stream:
            try {
                InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(),
                        ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.valueOf(contactId)));

                if (inputStream != null) {
                    Bitmap imageBitmap = BitmapFactory.decodeStream(inputStream);
                    if (imageBitmap != null) {
                        putBitmapAsset(imageBitmap);
                    } else {
                        putNullBitmapAsset();
                    }
                }

                assert inputStream != null;
                inputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // contact not found
            putContactName(phoneNumber);
            Log.v(TAG, "No contact found for number " + phoneNumber);
        }
        if (cursor != null) {
            cursor.close();
        }
    }

    private void putBitmapAsset(Bitmap bitmap) {
        Log.d(TAG, "sending bitmap asset");
        Asset asset = createAssetFromBitmap(bitmap);
        Log.d(TAG, bitmap.toString());
        PutDataMapRequest dataMap = PutDataMapRequest.create(Constants.DATA_CONTACT_PATH);
        dataMap.getDataMap().putAsset(Constants.CONTACT_IMAGE_ASSET, asset);
        PutDataRequest request = dataMap.asPutDataRequest();
        request.setUrgent();
        Wearable.DataApi.putDataItem(messageUtil.getGoogleApiClient(), request);
    }

    private void putNullBitmapAsset() {
        Log.d(TAG, "Sending null bitmap asset");
        PutDataMapRequest dataMap = PutDataMapRequest.create(Constants.DATA_CONTACT_PATH);
        dataMap.getDataMap().putAsset(Constants.CONTACT_IMAGE_ASSET, null);
        PutDataRequest request = dataMap.asPutDataRequest();
        request.setUrgent();
        Wearable.DataApi.putDataItem(messageUtil.getGoogleApiClient(), request);
    }

    private void putContactName(String contactName) {
        Log.d(TAG, "sending contact information " + contactName);
        PutDataMapRequest dataMap = PutDataMapRequest.create(Constants.DATA_CONTACT_PATH);
        dataMap.getDataMap().putString(Constants.CONTACT_NAME_ASSET, contactName);
        PutDataRequest request = dataMap.asPutDataRequest();
        request.setUrgent();
        Wearable.DataApi.putDataItem(messageUtil.getGoogleApiClient(), request);
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }
}
