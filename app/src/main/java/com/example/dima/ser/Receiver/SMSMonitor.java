package com.example.dima.ser.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;

import com.example.dima.ser.WalkingIconService;

public class SMSMonitor extends BroadcastReceiver {
    private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (WalkingIconService.Ser!=null) {
            if (intent != null && intent.getAction() != null &&
                    ACTION.compareToIgnoreCase(intent.getAction()) == 0) {
                Object[] pduArray = (Object[]) intent.getExtras().get("pdus");
                SmsMessage[] messages = new SmsMessage[pduArray.length];
                for (int i = 0; i < pduArray.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pduArray[i]);

                }
                String sms_from = messages[0].getDisplayOriginatingAddress();
                StringBuilder bodyText = new StringBuilder();
                for (int i = 0; i < messages.length; i++) {
                    bodyText.append(messages[i].getMessageBody());
                }
                String body = bodyText.toString();
                WalkingIconService.Ser.sms(sms_from, body);
            }
        }
    }
}