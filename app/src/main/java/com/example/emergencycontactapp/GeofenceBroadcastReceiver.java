package com.example.emergencycontactapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.e("Geofence", "Geofencing event error: " + geofencingEvent.getErrorCode());
            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

        for (Geofence geofence : triggeringGeofences) {
            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                Log.e("Geofence", "Entered geofence: " + geofence.getRequestId());
                // Handle enter event (e.g., send notification)
            } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
                Log.e("Geofence", "Exited geofence: " + geofence.getRequestId());
                // Handle exit event
            }
        }
    }
}
