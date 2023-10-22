package com.dakual.gpstracker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

//import org.apache.http.HttpEntity;
//import org.apache.http.HttpResponse;
//import org.apache.http.HttpStatus;
//import org.apache.http.HttpVersion;
//import org.apache.http.client.ClientProtocolException;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.entity.UrlEncodedFormEntity;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.conn.ClientConnectionManager;
//import org.apache.http.conn.scheme.PlainSocketFactory;
//import org.apache.http.conn.scheme.Scheme;
//import org.apache.http.conn.scheme.SchemeRegistry;
//import org.apache.http.conn.scheme.SocketFactory;
//import org.apache.http.conn.ssl.SSLSocketFactory;
//import org.apache.http.entity.BasicHttpEntity;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.impl.client.DefaultHttpClient;
//import org.apache.http.NameValuePair;
//import org.apache.http.impl.conn.SingleClientConnManager;
//import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
//import org.apache.http.message.BasicNameValuePair;
//import org.apache.http.params.BasicHttpParams;
//import org.apache.http.params.HttpParams;
//import org.apache.http.params.HttpProtocolParams;
//import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity implements ServiceConnection {
    TextView deviceId;
    private LocationService mServiceConn;
    public static String TAG = "GPS Tracker";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String api = sp.getString("apiUrl",null);
        if (api == null) {
            String did = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (did == null) {
                did = UUID.randomUUID().toString();
            }

            SharedPreferences.Editor editor = sp.edit();
            editor.putString("apiUrl", "http://192.168.1.100:8080/api");
            editor.putString("apiToken", "test");
            editor.putString("deviceId", did);
            editor.commit();
        }

        String did = sp.getString("deviceId",null);
        deviceId   = (TextView) findViewById(R.id.textView);
        deviceId.setText(did);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, LocationService.class);
        startService(intent);
        bindService(intent, this, 0);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mServiceConn != null) {
            unbindService(this);
            mServiceConn = null;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        LocationService.MyBinder b = (LocationService.MyBinder) service;
        mServiceConn = b.getService();
        Toast.makeText(MainActivity.this, "Service Connected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mServiceConn = null;
        Toast.makeText(MainActivity.this, "Service Disconnected", Toast.LENGTH_SHORT).show();
    }
}