package com.lums.eventhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
    private LinearLayout recentActivityList;

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
        recentActivityList = findViewById(R.id.recentActivityList);

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
        loadRecentActivity();
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
    /**
    * loads the recently registered events to show as recent activity
     */

    private void loadRecentActivity() {
        db.collection("users")
                .document(userId)
                .collection("registrations")
                .orderBy("registeredAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(snapshots -> {
                    recentActivityList.removeAllViews();

                    if (snapshots.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText("No recent activity yet. Browse events to get started!");
                        empty.setTextColor(0xFF554477);
                        empty.setTextSize(12);
                        recentActivityList.addView(empty);
                        return;
                    }

                    boolean first = true;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String title = doc.getString("eventTitle");
                        String date  = doc.getString("date");

                        // Divider (not before first item)
                        if (!first) {
                            View divider = new View(this);
                            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
                            divParams.setMargins(0, 0, 0, 10);
                            divider.setLayoutParams(divParams);
                            divider.setBackgroundColor(0xFFF2F2F2);
                            recentActivityList.addView(divider);
                        }
                        first = false;

                        // Row
                        LinearLayout row = new LinearLayout(this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        rowParams.setMargins(0, 0, 0, 10);
                        row.setLayoutParams(rowParams);

                        // Green dot
                        View dot = new View(this);
                        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(8, 8);
                        dotParams.setMargins(0, 0, 10, 0);
                        dot.setLayoutParams(dotParams);
                        dot.setBackgroundColor(0xFF4CAF50);
                        row.addView(dot);

                        // Text
                        TextView tvText = new TextView(this);
                        tvText.setText("Registered for " + (title != null ? title : "an event"));
                        tvText.setTextColor(0xFF1A1A2E);
                        tvText.setTextSize(12);
                        tvText.setLayoutParams(new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                        row.addView(tvText);

                        // Date
                        TextView tvDate = new TextView(this);
                        tvDate.setText(date != null ? date : "");
                        tvDate.setTextColor(0xFFBBBBBB);
                        tvDate.setTextSize(10);
                        row.addView(tvDate);

                        recentActivityList.addView(row);
                    }
                })
                .addOnFailureListener(e -> {
                    // silently fail
                });
    }
}