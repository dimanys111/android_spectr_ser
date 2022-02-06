package com.example.dima.ser.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartServiceAtBootReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        //context.startService(new Intent(context, WalkingIconService.class));
    }
}