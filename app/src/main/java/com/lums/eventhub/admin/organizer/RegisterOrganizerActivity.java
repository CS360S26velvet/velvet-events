package com.lums.eventhub.admin.organizer;

/**
 * RegisterOrganizerActivity.java
 *
 * Role: Allows admin to register a new organizer/society.
 * Saves to Firestore users/ collection with fields:
 *   username    (e.g. #ORG_spades)
 *   password    (allocated by admin)
 *   societyName (e.g. SPADES Society)
 *   role        ("organizer")
 *
 * Admin US: Register New Organizer
 */

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lums.eventhub.R;
import java.util.HashMap;
import java.util.Map;

public class RegisterOrganizerActivity extends AppCompatActivity {

    private EditText etSocietyName, etUsername, etPassword;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_organizer);

        db = FirebaseFirestore.getInstance();

        etSocietyName = findViewById(R.id.etSocietyName);
        etUsername    = findViewById(R.id.etUsername);
        etPassword    = findViewById(R.id.etPassword);

        // Back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Save
        findViewById(R.id.btnRegister).setOnClickListener(v -> registerOrganizer());
    }

    private void registerOrganizer() {
        String societyName = etSocietyName.getText().toString().trim();
        String username    = etUsername.getText().toString().trim();
        String password    = etPassword.getText().toString().trim();

        // Validation
        if (societyName.isEmpty()) {
            etSocietyName.setError("Society name is required");
            return;
        }
        if (username.isEmpty()) {
            etUsername.setError("Username is required");
            return;
        }
        if (!username.startsWith("#ORG")) {
            etUsername.setError("Username must start with #ORG (e.g. #ORG_spades)");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        // Check username not already taken
        db.collection("users")
                .get()
                .addOnSuccessListener(query -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : query) {
                        String existing = doc.getString("username");
                        if (username.equals(existing)) {
                            etUsername.setError("Username already exists");
                            return;
                        }
                    }
                    // Username is free — save to Firestore
                    saveOrganizer(societyName, username, password);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void saveOrganizer(String societyName, String username, String password) {
        Map<String, Object> data = new HashMap<>();
        data.put("username",    username);
        data.put("password",    password);
        data.put("societyName", societyName);
        data.put("role",        "organizer");

        db.collection("users")
                .add(data)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this,
                            "Organizer registered: " + username,
                            Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}