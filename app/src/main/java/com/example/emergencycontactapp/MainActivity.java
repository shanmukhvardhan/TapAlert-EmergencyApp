
package com.example.emergencycontactapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;


import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private Button btnAddContact, btnEmergency;
    private ListView listContacts;
    private DatabaseReference contactsRef;
    private String userId;

    private List<Contact> contactList = new ArrayList<>();
    private ArrayAdapter<Contact> adapter;

    private static final int PERMISSION_REQUEST_CODE = 123;
    private FusedLocationProviderClient fusedLocationClient;


    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float lastX, lastY, lastZ;
    private static final float SHAKE_THRESHOLD = 15f;

    private TextView greetingText, timeDateText;
    private String userName;

    private ImageView remove;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        greetingText = findViewById(R.id.greetingText);
        timeDateText = findViewById(R.id.timeDateText);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // ✅ FIXED: Initialize userRef BEFORE using it
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(userId);
        userRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String nameFromDb = snapshot.getValue(String.class);
                if (nameFromDb != null && !nameFromDb.isEmpty()) {
                    userName = nameFromDb;
                }
                updateGreetingAndTime();  // Call only after name is retrieved
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                updateGreetingAndTime();  // Fallback in case of error
            }
        });


        ImageView settingsIcon = findViewById(R.id.settingsIcon);
        settingsIcon.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        });

        remove = findViewById(R.id.btnRemoveContact);
        remove.setOnClickListener(v -> showRemoveContactDialog());

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();





        contactsRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(userId).child("contacts");


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        if (checkAndRequestPermissions()) {
            startShakeService();
        }


        btnAddContact = findViewById(R.id.add_contact_button);
        btnEmergency = findViewById(R.id.emergencyButton);
        listContacts = findViewById(R.id.contactsView);


        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, contactList);
        listContacts.setAdapter(adapter);


        btnAddContact.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddContactActivity.class)));
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        btnEmergency.setOnClickListener(v -> {
            if (checkAndRequestPermissions()) {
                sendEmergencyAlert();
            }
        });


        loadContacts();


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, "Accelerometer not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void startShakeService() {
        Intent shakeServiceIntent = new Intent(this, ShakeDetectionService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(shakeServiceIntent);
        } else {
            startService(shakeServiceIntent);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float delta = Math.abs(x + y + z - (lastX + lastY + lastZ));

        if (delta > SHAKE_THRESHOLD) {
            triggerEmergencyAlert();
        }
        lastX = x;
        lastY = y;
        lastZ = z;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    private void triggerEmergencyAlert() {
        if (checkAndRequestPermissions()) {
            sendEmergencyAlert();
        }
    }
    private void updateGreetingAndTime() {
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                // Update greeting
                String greeting;
                int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
                if (hour >= 0 && hour < 12) {
                    greeting = "Good Morning";
                } else if (hour < 18) {
                    greeting = "Good Afternoon";
                } else {
                    greeting = "Good Evening";
                }
                greetingText.setText(greeting + ", " + userName + "!");

                // Update time and date
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE, MMM d, yyyy hh:mm:ss a", java.util.Locale.getDefault());
                String currentTime = sdf.format(new java.util.Date());
                timeDateText.setText(currentTime);

                handler.postDelayed(this, 1000); // Update every second
            }
        };
        handler.post(runnable);
    }


    private boolean checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Basic permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.SEND_SMS);
        }

        // Add FOREGROUND_SERVICE_LOCATION for Android 10+ (Q)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
            return false;
        }

        return true;
    }

    private void loadContacts() {
        contactsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                contactList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Contact contact = snapshot.getValue(Contact.class);
                    if (contact != null) {
                        contactList.add(contact);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendEmergencyAlert() {
        if (contactList.isEmpty()) {
            Toast.makeText(this, "No emergency contacts added",
                    Toast.LENGTH_SHORT).show();
            return;
        }


        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        sendSMSWithLocation(location);
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Could not get location. Sending alert without location.",
                                Toast.LENGTH_SHORT).show();
                        sendSMSWithoutLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this,
                            "Location error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    sendSMSWithoutLocation();
                });
    }

    private void showRemoveContactDialog() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference contactsRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("contacts");

        contactsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(MainActivity.this, "No contacts to remove", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<String> contactNames = new ArrayList<>();
                List<String> contactKeys = new ArrayList<>();

                for (DataSnapshot contactSnapshot : snapshot.getChildren()) {
                    String name = contactSnapshot.child("name").getValue(String.class);
                    contactNames.add(name);
                    contactKeys.add(contactSnapshot.getKey());
                }

                String[] namesArray = contactNames.toArray(new String[0]);

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Select contact to remove");
                builder.setItems(namesArray, (dialog, which) -> {
                    String selectedKey = contactKeys.get(which);
                    contactsRef.child(selectedKey).removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Contact removed", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Failed to remove contact", Toast.LENGTH_SHORT).show());
                });
                builder.setNegativeButton("Cancel", null);
                builder.show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error fetching contacts", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void sendSMSWithLocation(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        String locationLink = "http://maps.google.com/?q=" + latitude + "," + longitude;
        String message = "EMERGENCY ALERT: I need help! My current location: " + locationLink;


        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(userId);
        userRef.child("emergencyStatus").child("isActive").setValue(true);
        userRef.child("emergencyStatus").child("message").setValue(message);
        userRef.child("lastLocation").child("latitude").setValue(latitude);
        userRef.child("lastLocation").child("longitude").setValue(longitude);


        new Thread(() -> {
            for (Contact contact : contactList) {
                runOnUiThread(() -> sendSMS(contact.getPhone(), message));
                try {

                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        Toast.makeText(this, "Emergency alert sent to " + contactList.size() + " contacts",
                Toast.LENGTH_LONG).show();
    }

    private void sendSMSWithoutLocation() {
        String message = "EMERGENCY ALERT: I need help!";


        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(userId);
        userRef.child("emergencyStatus").child("isActive").setValue(true);
        userRef.child("emergencyStatus").child("message").setValue(message);


        new Thread(() -> {
            for (Contact contact : contactList) {
                runOnUiThread(() -> sendSMS(contact.getPhone(), message));
                try {
                    // Add delay between messages to prevent throttling
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        Toast.makeText(this, "Emergency alert sent to " + contactList.size() + " contacts",
                Toast.LENGTH_LONG).show();
    }

    private void sendSMS(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            String SENT = "SMS_SENT";
            String DELIVERED = "SMS_DELIVERED";

            Intent sentIntent = new Intent(SENT);
            Intent deliveredIntent = new Intent(DELIVERED);

            PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, sentIntent, PendingIntent.FLAG_IMMUTABLE);
            PendingIntent deliveredPI = PendingIntent.getBroadcast(
                    this, 0, deliveredIntent, PendingIntent.FLAG_IMMUTABLE);

            BroadcastReceiver sentReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                }
            };

            BroadcastReceiver deliveredReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                }
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                registerReceiver(sentReceiver, new IntentFilter(SENT), Context.RECEIVER_NOT_EXPORTED);
                registerReceiver(deliveredReceiver, new IntentFilter(DELIVERED), Context.RECEIVER_NOT_EXPORTED);
            }

            List<String> dividedMessage = smsManager.divideMessage(message);
            ArrayList<PendingIntent> sentIntents = new ArrayList<>(dividedMessage.size());
            ArrayList<PendingIntent> deliveredIntents = new ArrayList<>(dividedMessage.size());

            for (int i = 0; i < dividedMessage.size(); i++) {
                sentIntents.add(sentPI);
                deliveredIntents.add(deliveredPI);
            }

            smsManager.sendMultipartTextMessage(phoneNumber, null, new ArrayList<>(dividedMessage), sentIntents, deliveredIntents);


            new Handler().postDelayed(() -> {
                try {
                    unregisterReceiver(sentReceiver);
                    unregisterReceiver(deliveredReceiver);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 30000);

        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),
                    "SMS failed to " + phoneNumber + ": " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {

                startShakeService();
            } else {
                Toast.makeText(this,
                        "App needs permissions to function properly",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }
}