package com.glaciersecurity.glaciermessenger.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class PhonecallReceiver extends BroadcastReceiver {
    private static int lastState = TelephonyManager.CALL_STATE_IDLE;

    private PhonecallReceiverListener phonecallReceiverListener;

    public PhonecallReceiver(PhonecallReceiverListener phonecallReceiverListener) {
        super();
        this.phonecallReceiverListener = phonecallReceiverListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
            int state = 0;
            if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                state = TelephonyManager.CALL_STATE_IDLE;
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                state = TelephonyManager.CALL_STATE_OFFHOOK;
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                state = TelephonyManager.CALL_STATE_RINGING;
            }
            onCallStateChanged(context, state, number);
        }
    }

    public interface PhonecallReceiverListener {
        void onIncomingNativeCallAnswered();
        void onIncomingNativeCallRinging(int call_act);
    }

    //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
    //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
    public void onCallStateChanged(Context context, int state, String number) {
        int call_act;
        if(lastState == state){
            return;
        }
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                //Transition of idle -> ringing = starting to ring for an incoming call.
                call_act = 1;
                if (phonecallReceiverListener != null) {
                    phonecallReceiverListener.onIncomingNativeCallRinging(call_act);
                }
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                //Transition of ringing -> offhook = answering an incoming call.
                if(lastState == TelephonyManager.CALL_STATE_RINGING){
                    if (phonecallReceiverListener != null) {
                        phonecallReceiverListener.onIncomingNativeCallAnswered();
                    }
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                //Transition of offhook -> idle = hanging up at the end of a call.
                //Transition of ringing -> idle = rejecting an incoming call.
                call_act = 0;
                if(lastState == TelephonyManager.CALL_STATE_RINGING) {
                    if (phonecallReceiverListener != null) {
                        phonecallReceiverListener.onIncomingNativeCallRinging(call_act);
                    }
                }
                break;
        }
        lastState = state;
    }
}


