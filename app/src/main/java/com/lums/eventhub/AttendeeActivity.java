package com.lums.eventhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
/**
 * AttendeeActivity.java
 * Main dashboard for attendees after login.
 * Displays registration count and unread notification count.
 * Provides navigation to: Browse Events, My Registrations,
 * Notifications, and Calendar.
 *
 * Receives userId from LoginActivity via Intent and passes
 * it forward to all subsequent activities.
 */

public class AttendeeActivity extends AppCompatActivity {
    private Button btnBrowseEvents;
    private Button navBrowseEvents;
    private Button btnMyRegistrations;
    private Button navMyRegistrations;
    private Button btnNotifications;
    private Button navNotifs;
    private Button btnCalendar;
    private Button navHome;
    private Button logout;
    private TextView tvRegisteredCount;
    private TextView tvNotifCount;
    private FirebaseFirestore db;
    private String userId; // ← no longer hardcoded

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.attendee_activity);

        // ← Receive userId from LoginActivity
        userId = getIntent().getStringExtra("userId");

        btnBrowseEvents    = (Button) findViewById(R.id.btnBrowseEvents);
        navBrowseEvents    = (Button) findViewById(R.id.navBrowseEvents);
        btnCalendar        = (Button) findViewById(R.id.btnCalendar);
        btnNotifications   = (Button) findViewById(R.id.btnNotifications);
        navNotifs          = (Button) findViewById(R.id.navNotifications);
        btnMyRegistrations = (Button) findViewById(R.id.btnMyRegistrations);
        navMyRegistrations = (Button) findViewById(R.id.navMyRegistrations);
        navHome            = (Button) findViewById(R.id.navDashboard);
        logout             = (Button) findViewById(R.id.btnLogout);
        tvRegisteredCount  = findViewById(R.id.tvRegisteredCount);
        tvNotifCount       = findViewById(R.id.tvNotifCount);
        db                 = FirebaseFirestore.getInstance();

        btnBrowseEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AttendeeActivity.this, com.lums.eventhub.EventBrowsingActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            }
        });

        navBrowseEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AttendeeActivity.this, com.lums.eventhub.EventBrowsingActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            }
        });

        btnCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AttendeeActivity.this, com.lums.eventhub.AttendeeCalendarActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            }
        });

        btnNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AttendeeActivity.this, com.lums.eventhub.NotificationsActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            }
        });

        navNotifs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AttendeeActivity.this, com.lums.eventhub.NotificationsActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            }
        });

        btnMyRegistrations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AttendeeActivity.this, com.lums.eventhub.MyRegistrationsActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            }
        });

        navMyRegistrations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AttendeeActivity.this, com.lums.eventhub.MyRegistrationsActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            }
        });

        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // already here
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(AttendeeActivity.this, com.lums.eventhub.auth.LoginActivity.class));
                finish();
            }
        });

        loadCounts();
    }

    /**
     * Loads registration and notification counts for the logged-in user
     */
    private void loadCounts() {
        db.collection("users")
                .document(userId)
                .collection("registrations")
                .get()
                .addOnSuccessListener(snapshots -> {
                    tvRegisteredCount.setText(String.valueOf(snapshots.size()));
                });

        db.collection("users")
                .document(userId)
                .collection("notifications")
                .whereEqualTo("isRead", false)
                .get()
                .addOnSuccessListener(snapshots -> {
                    tvNotifCount.setText(String.valueOf(snapshots.size()));
                });
    }
}