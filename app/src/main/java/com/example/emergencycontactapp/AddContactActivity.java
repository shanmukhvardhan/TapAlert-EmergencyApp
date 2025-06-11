package com.example.emergencycontactapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class AddContactActivity extends AppCompatActivity {

    private EditText editName, editPhone;
    private Button btnSave;
    private DatabaseReference contactsRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_contact);

        // Initialize views
        editName = findViewById(R.id.edit_contact_name);
        editPhone = findViewById(R.id.edit_contact_phone);
        btnSave = findViewById(R.id.btn_save_contact);

        // Get current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            userId = user.getUid();
            // Now it's safe to initialize contactsRef
            contactsRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(userId).child("contacts");
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish(); // close the activity
            return;
        }

        // Save contact on button click
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveContact();
            }
        });
    }

    private void saveContact() {
        String name = editName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate unique contact ID
        String contactId = contactsRef.push().getKey();

        // Create contact object
        Contact contact = new Contact(contactId, name, phone);

        // Save contact to Firebase
        contactsRef.child(contactId).setValue(contact)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddContactActivity.this,
                            "Contact saved successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to previous screen
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddContactActivity.this,
                            "Failed to save contact: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Optional: Secure SharedPreferences if needed later
    public static class SecurityUtils {
        public static String encrypt(Context context, String keyAlias, String data) throws GeneralSecurityException, IOException {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    "secret_shared_prefs",
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(keyAlias, data);
            editor.apply();
            return data;
        }
    }
}
