package com.lums.eventhub;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * MyRegistrationsActivity.java
 * Displays a list of all events the logged-in attendee
 * has registered for, fetched from their Firestore
 * subcollection: users/{userId}/registrations
 *
 * Each registration is shown as a card with event title,
 * organizer, date, time, venue, and a "View Event" button
 * that navigates to EventDetailsActivity.
 *
 * Passes source="myRegistrations" to EventDetailsActivity
 * so the back button there returns here instead of going
 * to EventBrowsingActivity.
 *
 * AT US-19 / US-20 — For past events (date already passed),
 * a "Rate Event" button is shown that launches FeedbackActivity.
 *
 * Receives userId via Intent and passes it forward to all
 * subsequent activities.
 */
public class MyRegistrationsActivity extends AppCompatActivity {

    LinearLayout registrationsList;
    TextView tvTotalCount, tvEmpty;
    Button navDashboard, navBrowseEvents, navMyRegistrations, navNotifications, btnLogout;

    FirebaseFirestore db;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_registrations);

        userId = getIntent().getStringExtra("userId");

        registrationsList  = findViewById(R.id.registrationsList);
        tvTotalCount       = findViewById(R.id.tvTotalCount);
        tvEmpty            = findViewById(R.id.tvEmpty);
        navDashboard       = findViewById(R.id.navDashboard);
        navBrowseEvents    = findViewById(R.id.navBrowseEvents);
        navMyRegistrations = findViewById(R.id.navMyRegistrations);
        navNotifications   = findViewById(R.id.navNotifications);
        btnLogout          = findViewById(R.id.btnLogout);

        db = FirebaseFirestore.getInstance();

        loadRegistrations();

        navDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyRegistrationsActivity.this,
                        com.lums.eventhub.AttendeeActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            }
        });

        navBrowseEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyRegistrationsActivity.this,
                        com.lums.eventhub.EventBrowsingActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            }
        });

        navMyRegistrations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // already here
            }
        });

        navNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyRegistrationsActivity.this,
                        NotificationsActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MyRegistrationsActivity.this,
                        com.lums.eventhub.auth.LoginActivity.class));
                finish();
            }
        });
    }

    private void loadRegistrations() {
        db.collection("users").document(userId).collection("registrations").get()
                .addOnSuccessListener(snapshots -> {
                    registrationsList.removeAllViews();
                    int count = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String eventId        = doc.getString("eventId");
                        String title          = doc.getString("eventTitle");
                        String organizer      = doc.getString("organizer");
                        String date           = doc.getString("date");
                        String venue          = doc.getString("venue");
                        String time           = doc.getString("time");
                        String fee            = doc.getString("fee");
                        String description    = doc.getString("description");
                        String regClosingDate = doc.getString("RegClosingDate");
                        String category       = doc.getString("category");
                        int seatsBooked       = doc.getLong("seatsBooked") != null ? doc.getLong("seatsBooked").intValue() : 0;
                        int seatsTotal        = doc.getLong("seatsTotal") != null ? doc.getLong("seatsTotal").intValue() : 0;

                        registrationsList.addView(
                                buildCard(eventId, title, organizer, date, time, venue,
                                        fee, description, regClosingDate, category,
                                        seatsBooked, seatsTotal)
                        );
                        count++;
                    }

                    tvTotalCount.setText(String.valueOf(count));
                    tvEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);

                }).addOnFailureListener(e ->
                        tvEmpty.setText("Failed to load registrations"));
    }

    private View buildCard(String eventId, String title, String organizer, String date,
                           String time, String venue, String fee, String description,
                           String regClosingDate, String category,
                           int seatsBooked, int seatsTotal) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(0xFFFFFFFF);
        card.setElevation(4f);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardParams);
        card.setPadding(16, 16, 16, 16);

        // Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText(title != null ? title : "Event");
        tvTitle.setTextColor(0xFF1A1A2E);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextSize(16);
        card.addView(tvTitle);

        // Organizer
        TextView tvOrg = new TextView(this);
        tvOrg.setText(organizer != null ? organizer : "");
        tvOrg.setTextColor(0xFF888888);
        tvOrg.setTextSize(12);
        card.addView(tvOrg);

        // Date, time, venue
        TextView tvInfo = new TextView(this);
        tvInfo.setText("📅 " + (date != null ? date : "")
                + "   🕐 " + (time != null ? time : "")
                + "   📍 " + (venue != null ? venue : ""));
        tvInfo.setTextColor(0xFF444444);
        tvInfo.setTextSize(11);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        infoParams.setMargins(0, 8, 0, 12);
        tvInfo.setLayoutParams(infoParams);
        card.addView(tvInfo);

        // Button row
        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams btnRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        btnRowParams.setMargins(0, 4, 0, 0);
        btnRow.setLayoutParams(btnRowParams);

        // View Event button — passes source="myRegistrations" so back button
        // in EventDetailsActivity returns here instead of EventBrowsingActivity
        Button btnView = new Button(this);
        btnView.setText("View Event");
        btnView.setTextColor(0xFFFFFFFF);
        btnView.setTextSize(13);
        btnView.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF5B2D8E));
        LinearLayout.LayoutParams viewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        viewParams.setMargins(0, 0, 10, 0);
        btnView.setLayoutParams(viewParams);
        btnView.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.lums.eventhub.EventDetailsActivity.class);
            intent.putExtra("userId",           userId);
            intent.putExtra("source",           "myRegistrations"); // ← back button fix
            intent.putExtra("eventId",          eventId);
            intent.putExtra("eventTitle",       title);
            intent.putExtra("eventOrganizer",   organizer);
            intent.putExtra("eventDate",        date);
            intent.putExtra("eventVenue",       venue);
            intent.putExtra("Time",             time);
            intent.putExtra("fee",              fee);
            intent.putExtra("Description",      description);
            intent.putExtra("RegClosingDate",   regClosingDate);
            intent.putExtra("eventCategory",    category);
            intent.putExtra("eventSeatsBooked", seatsBooked);
            intent.putExtra("eventSeatsTotal",  seatsTotal);
            startActivity(intent);
        });
        btnRow.addView(btnView);

        // AT US-19 / US-20 — Rate Event button, only for past events
        if (isEventPast(date)) {
            Button btnFeedback = new Button(this);
            btnFeedback.setText("Rate Event");
            btnFeedback.setTextColor(0xFF5B2D8E);
            btnFeedback.setTextSize(13);
            btnFeedback.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFEDE7F6));
            btnFeedback.setOnClickListener(v -> {
                Intent intent = new Intent(this, FeedbackActivity.class);
                intent.putExtra("userId",         userId);
                intent.putExtra("eventId",        eventId);
                intent.putExtra("eventTitle",     title);
                intent.putExtra("eventOrganizer", organizer);
                intent.putExtra("eventDate",      date);
                startActivity(intent);
            });
            btnRow.addView(btnFeedback);
        }

        card.addView(btnRow);

        return card;
    }

    /**
     * Returns true if the event date has already passed.
     * Matches the date format stored in Firestore: "MMM d, yyyy" e.g. "Apr 20, 2025"
     */
    private boolean isEventPast(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            Date eventDate = sdf.parse(dateStr);
            return eventDate != null && eventDate.before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}