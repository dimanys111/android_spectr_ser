package com.example.dima.ser.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class KeyControlReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
//        int volume = (Integer)intent.getExtras().get("android.media.EXTRA_VOLUME_STREAM_VALUE");
//        if (WalkingIconService.Ser.volumeOld-volume>0) {
//            WalkingIconService.Ser.nachZapVid();
//            Toast.makeText(WalkingIconService.Ser, "Началось" + String.valueOf(volume), Toast.LENGTH_SHORT)
//                    .show();
//        }
//        else{
//            if (WalkingIconService.Ser.volumeOld-volume<0) {
//                WalkingIconService.Ser.konZapVid();
//                Toast.makeText(WalkingIconService.Ser, "Кончилось" + String.valueOf(volume), Toast.LENGTH_SHORT)
//                        .show();
//            }
//        }
//        WalkingIconService.Ser.volumeOld=volume;
    }
}