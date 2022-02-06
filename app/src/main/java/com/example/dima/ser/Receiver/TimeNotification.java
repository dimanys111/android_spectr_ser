package com.example.dima.ser.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.dima.ser.WalkingIconService;

public class TimeNotification extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Этот метод будет вызываться по событию, сочиним его позже
        if (WalkingIconService.Ser!=null) {
            if (WalkingIconService.Ser.onoff) {
                if (!WalkingIconService.Ser.nachVid && !WalkingIconService.Ser.zvon) {
                WalkingIconService.Ser.svAddwindowManager();
                WalkingIconService.Ser.konZvuk();
                WalkingIconService.Ser.setZvuk();

                    //WalkingIconService.Ser.setGPS();
                }
            } else {
                if (!WalkingIconService.Ser.zvon) {
                    WalkingIconService.Ser.konZapVid();
                }
                WalkingIconService.Ser.readStop();
                WalkingIconService.Ser.recordStop();
            }
            WalkingIconService.Ser.otprSoket();
        }
    }
}