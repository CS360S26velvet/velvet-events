/**
 * LoginActivity.java
 * Entry point of the app. Routes users to correct dashboard
 * based on username prefix: #AD = Admin, #ORG = Organizer, #AT = Attendee
 * Implements: Admin US-01
 *
 * #ORG login: fetches societyName from users/ doc and passes it
 * along with username to OrganizerDashboardActivity.
 */
package com.lums.eventhub.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lums.eventhub.R;
import com.lums.eventhub.admin.dashboard.AdminDashboardActivity;
import com.lums.eventhub.OrganizerDashboardActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);

        etUsername.setHintTextColor(android.graphics.Color.parseColor("#C4A8B0"));
        etPassword.setHintTextColor(android.graphics.Color.parseColor("#C4A8B0"));

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch all users and match manually (avoids Firestore # symbol query issues)
        FirebaseFirestore.getInstance()
                .collection("users")
                .get()
                .addOnSuccessListener(query -> {
                    DocumentSnapshot matchedDoc = null;
                    for (DocumentSnapshot doc : query) {
                        String dbUser = doc.getString("username");
                        String dbPass = doc.getString("password");
                        if (dbUser != null && dbUser.trim().equals(username) &&
                                dbPass != null && dbPass.trim().equals(password)) {
                            matchedDoc = doc;
                            break;
                        }
                    }

                    if (matchedDoc == null) {
                        Toast.makeText(this, "Invalid username or password",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Route based on prefix
                    if (username.startsWith("#AD")) {
                        Intent intent = new Intent(this, AdminDashboardActivity.class);
                        intent.putExtra("username", username);
                        startActivity(intent);
                        finish();

                    } else if (username.startsWith("#ORG")) {
                        // Get societyName from the matched user doc
                        // organizerUsername == the username they logged in with
                        String societyName = matchedDoc.getString("societyName");
                        if (societyName == null) societyName = username; // fallback

                        Intent intent = new Intent(this, OrganizerDashboardActivity.class);
                        intent.putExtra("organizerUsername", username);  // canonical field name
                        intent.putExtra("societyName",       societyName);
                        startActivity(intent);
                        finish();

                    } else if (username.startsWith("#AT")) {
                        // Person D will wire this up
                        Toast.makeText(this, "Attendee module — coming soon",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this,
                                "Invalid ID format. Use #AD, #ORG, or #AT prefix",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Connection error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }
}