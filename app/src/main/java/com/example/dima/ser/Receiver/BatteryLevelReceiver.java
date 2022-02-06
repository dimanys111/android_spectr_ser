package com.example.dima.ser.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.dima.ser.WalkingIconService;

public class BatteryLevelReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.ACTION_BATTERY_LOW")) {
            WalkingIconService.zariad = false;
        }
        else
        {
            WalkingIconService.zariad = true;
        }
    }
}