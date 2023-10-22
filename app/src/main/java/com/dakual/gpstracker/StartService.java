package com.dakual.gpstracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartService extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, LocationService.class);
        context.startService(i);

        Log.i("StartService", "BroadcastReceiver:Location service started");
    }
}
