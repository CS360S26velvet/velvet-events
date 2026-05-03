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
import com.lums.eventhub.attendee.dashboard.AttendeeActivity;
import com.lums.eventhub.R;
import com.lums.eventhub.admin.dashboard.AdminDashboardActivity;
import com.lums.eventhub.organizer.dashboard.OrganizerDashboardActivity;

/**
 * LoginActivity.java
 *
 * Role: Entry point of the application. Displays the login screen and
 * routes authenticated users to the correct dashboard based on their
 * username prefix.
 *
 * Routing logic:
 *   #AD  → AdminDashboardActivity     (must exist in Firestore users/)
 *   #ORG → OrganizerDashboardActivity (must exist in Firestore users/)
 *   #AT  → AttendeeActivity           (auto-registered if first login)
 *
 * Implements: Admin US-01
 */

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;

    // SharedPreferences key for persistent login
    private static final String PREFS_NAME  = "VelvetLoginPrefs";
    private static final String KEY_USER    = "saved_username";
    private static final String KEY_PASS    = "saved_password";
    private static final String KEY_ROLE    = "saved_role";
    private static final String KEY_SOCIETY = "saved_society";
    private static final String KEY_USERID  = "saved_userId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Check for saved session — if found, skip login screen
        android.content.SharedPreferences prefs =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedUser    = prefs.getString(KEY_USER, null);
        String savedPass    = prefs.getString(KEY_PASS, null);
        String savedRole    = prefs.getString(KEY_ROLE, null);
        String savedSociety = prefs.getString(KEY_SOCIETY, null);
        String savedUserId  = prefs.getString(KEY_USERID, null);

        if (savedUser != null && savedRole != null) {
            // Restore session without re-querying Firestore
            restoreSession(savedUser, savedRole, savedSociety, savedUserId);
            return; // don't load the login UI
        }

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin   = findViewById(R.id.btnLogin);

        etUsername.setHintTextColor(android.graphics.Color.parseColor("#C4A8B0"));
        etPassword.setHintTextColor(android.graphics.Color.parseColor("#C4A8B0"));

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    /** Restores a saved session and navigates directly to the correct dashboard. */
    private void restoreSession(String username, String role,
                                String societyName, String userId) {
        switch (role) {
            case "admin":
                Intent adminIntent = new Intent(this, AdminDashboardActivity.class);
                adminIntent.putExtra("username", username);
                adminIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(adminIntent);
                finish();
                break;
            case "organizer":
                Intent orgIntent = new Intent(this, OrganizerDashboardActivity.class);
                orgIntent.putExtra("organizerUsername", username);
                orgIntent.putExtra("societyName", societyName != null ? societyName : username);
                orgIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(orgIntent);
                finish();
                break;
            case "attendee":
                goToAttendee(userId != null ? userId : "", username);
                break;
        }
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter username and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // #AT users: self-register if first time, otherwise log in normally
        if (username.startsWith("#AT")) {
            handleAttendeeLogin(username, password);
            return;
        }

        // #AD and #ORG: must already exist in Firestore users collection
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

                    if (username.startsWith("#AD")) {
                        // Save session
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                                .putString(KEY_USER, username)
                                .putString(KEY_ROLE, "admin")
                                .apply();
                        Intent intent = new Intent(this, AdminDashboardActivity.class);
                        intent.putExtra("username", username);
                        startActivity(intent);
                        finish();

                    } else if (username.startsWith("#ORG")) {
                        String societyName = matchedDoc.getString("societyName");
                        if (societyName == null) societyName = username;
                        // Save session
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                                .putString(KEY_USER, username)
                                .putString(KEY_ROLE, "organizer")
                                .putString(KEY_SOCIETY, societyName)
                                .apply();
                        Intent intent = new Intent(this, OrganizerDashboardActivity.class);
                        intent.putExtra("organizerUsername", username);
                        intent.putExtra("societyName", societyName);
                        startActivity(intent);
                        finish();

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

    /**
     * Handles attendee login with auto-registration.
     * If username+password not found in Firestore → creates new account.
     * If found → logs in with existing account (all their data is preserved).
     */
    private void handleAttendeeLogin(String username, String password) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .get()
                .addOnSuccessListener(query -> {
                    DocumentSnapshot matchedDoc = null;

                    // Search for existing account
                    for (DocumentSnapshot doc : query) {
                        String dbUser = doc.getString("username");
                        String dbPass = doc.getString("password");
                        if (dbUser != null && dbUser.trim().equals(username) &&
                                dbPass != null && dbPass.trim().equals(password)) {
                            matchedDoc = doc;
                            break;
                        }
                    }

                    if (matchedDoc != null) {
                        // ✅ Existing attendee — log them in
                        String userId = matchedDoc.getId();
                        goToAttendee(userId, username);

                    } else {
                        // 🆕 New attendee — create account in Firestore
                        java.util.Map<String, Object> newUser = new java.util.HashMap<>();
                        newUser.put("username", username);
                        newUser.put("password", password);
                        newUser.put("role", "attendee");
                        newUser.put("createdAt",
                                com.google.firebase.Timestamp.now());

                        db.collection("users")
                                .add(newUser)
                                .addOnSuccessListener(docRef -> {
                                    // Account created — go to dashboard
                                    Toast.makeText(this,
                                            "Account created! Welcome " + username,
                                            Toast.LENGTH_SHORT).show();
                                    goToAttendee(docRef.getId(), username);
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "Could not create account: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show()
                                );
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Connection error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void goToAttendee(String userId, String username) {
        // Save session
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(KEY_USER, username)
                .putString(KEY_ROLE, "attendee")
                .putString(KEY_USERID, userId)
                .apply();
        Intent intent = new Intent(this, AttendeeActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("username", username);
        startActivity(intent);
        finish();
    }

    /** Call this from logout buttons to clear the saved session. */
    public static void clearSession(android.content.Context ctx) {
        ctx.getSharedPreferences("VelvetLoginPrefs", MODE_PRIVATE).edit().clear().apply();
    }
}