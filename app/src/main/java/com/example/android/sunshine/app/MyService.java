package com.example.android.sunshine.app;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.ByteArrayOutputStream;

public class MyService extends WearableListenerService {


    GoogleApiClient mGoogleApiClient;

    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[] {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };

    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;


    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (messageEvent.getPath().equals(getString(R.string.alert_path))) {

            syncWearable(this);
        }

    }


    public void syncWearable(Context context) {



try {


    mGoogleApiClient = new GoogleApiClient.Builder(context)
            .addApi(Wearable.API).build();
    mGoogleApiClient.connect();

}

catch (Exception e)
{

}

        String locationQuery = Utility.getPreferredLocation(context);

        Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());

        // we'll query our contentProvider, as always
        Cursor cursor = context.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);
        if (cursor.moveToFirst()) {
            int weatherId = cursor.getInt(INDEX_WEATHER_ID);
            double high = cursor.getDouble(INDEX_MAX_TEMP);
            double low = cursor.getDouble(INDEX_MIN_TEMP);
            String desc = cursor.getString(INDEX_SHORT_DESC);

            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), Utility.getIconResourceForWeatherCondition(weatherId));
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            Asset asset = Asset.createFromBytes(byteStream.toByteArray());


            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(getString(R.string.weather_path));
            putDataMapRequest.getDataMap().putAsset(getString(R.string.icon_key), asset);
            putDataMapRequest.getDataMap().putDouble(getString(R.string.max_temp_key), high);
            putDataMapRequest.getDataMap().putDouble(getString(R.string.min_temp_key), low);
            putDataMapRequest.getDataMap().putLong(getString(R.string.time_key),System.currentTimeMillis());

            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            request.setUrgent();

            Wearable.DataApi.putDataItem(mGoogleApiClient, request).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                    Log.d("syncWearable", "Sending image was successful: " + dataItemResult.getStatus()
                            .isSuccess());
                }
            });


        }
        cursor.close();
        mGoogleApiClient.disconnect();

    }

}
