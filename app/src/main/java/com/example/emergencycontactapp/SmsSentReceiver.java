// SmsSentReceiver.java
package com.example.emergencycontactapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class SmsSentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        switch (getResultCode()) {
            case android.app.Activity.RESULT_OK:
                Toast.makeText(context, "SMS sent successfully", Toast.LENGTH_SHORT).show();
                break;
            case android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                Toast.makeText(context, "Generic failure", Toast.LENGTH_SHORT).show();
                break;
            case android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE:
                Toast.makeText(context, "No service", Toast.LENGTH_SHORT).show();
                break;
            case android.telephony.SmsManager.RESULT_ERROR_NULL_PDU:
                Toast.makeText(context, "Null PDU", Toast.LENGTH_SHORT).show();
                break;
            case android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF:
                Toast.makeText(context, "Radio off", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
