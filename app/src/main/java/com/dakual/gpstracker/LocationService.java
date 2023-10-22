package com.dakual.gpstracker;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import android.provider.Settings.Secure;

public class LocationService extends Service implements LocationListener {
    private final IBinder mBinder = new MyBinder();
    private boolean isGPSEnabled = false;
    private boolean isNetworkEnabled = false;
    private boolean canGetLocation = false;
    public Location lastLocation;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 5 * 1;
    protected LocationManager locationManager;
    private String apiUrl;
    private String apiToken;
    private String deviceId;

    private String LOG = "GPS Tracker Service";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOG, "service created");

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        apiUrl   = sp.getString("apiUrl",null);
        apiToken = sp.getString("apiToken",null);
        deviceId = sp.getString("deviceId",null);

        listenLocation();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOG, "service started");

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG, "service destroyed");

        Intent intent = new Intent();
        intent.setAction("com.dakual.StartLocationService");
        sendBroadcast(intent);
    }

    private void listenLocation() {
        try {
            locationManager  = (LocationManager)getSystemService(LOCATION_SERVICE);
            isGPSEnabled     = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // Null
            } else {
                canGetLocation = true;

                // Network Location
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    if (locationManager != null) {
                        lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                }

                //GPS Location
                if (isGPSEnabled) {
                    if (lastLocation == null) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        if (locationManager != null) {
                            lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(LOG, location.toString());

        lastLocation  = location;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                postLocation(location);
            }
        });
        thread.start();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        listenLocation();
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    void postLocation(Location location) {
        HttpClient httpClient = MyHttpClient.newInstance();
        HttpPost httpPost     = new HttpPost(apiUrl);

        try {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("device", deviceId);
            jsonObj.put("time", location.getTime());
            jsonObj.put("altitude", location.getAltitude());
            jsonObj.put("speed", location.getSpeed());
            jsonObj.put("provider", location.getProvider());
            jsonObj.put("accuracy", location.getAccuracy());
            jsonObj.put("latitude", location.getLatitude());
            jsonObj.put("longitude", location.getLongitude());
            Log.d(LOG, jsonObj.toString());

            httpPost.setEntity(new StringEntity(jsonObj.toString(), "UTF-8"));
            httpPost.setHeader("Authorization", "Bearer " + apiToken);
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Accept-Encoding", "application/json");
            httpPost.setHeader("User-Agent", "GPS Tracker v1.0");
            httpPost.setHeader("Accept-Language", "en-US");

            HttpResponse response = httpClient.execute(httpPost);
            int statusCode        = response.getStatusLine().getStatusCode();
            if(statusCode == HttpStatus.SC_OK) {
                HttpEntity responseEntity = response.getEntity();
                String responseBody       = EntityUtils.toString(responseEntity);
                Log.d(LOG, responseBody);

                // JSONObject jsonObject     = new JSONObject(responseBody);

            } else {
                Log.e(LOG, "Failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class MyBinder extends Binder {
        LocationService getService() {
            return LocationService.this;
        }
    }
}
