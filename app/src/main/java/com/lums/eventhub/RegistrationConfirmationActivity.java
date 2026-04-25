package com.lums.eventhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

/**
 * RegistrationConfirmationActivity.java
 *
 * AT US-11 — Displays a confirmation screen after an attendee
 * successfully registers for an event.
 *
 * Shows:
 * - Success icon and congratulations message
 * - Event summary: title, date, time, venue, fee
 * - "View My Registrations" button → MyRegistrationsActivity
 * - "Browse More Events" button → EventBrowsingActivity
 *
 * Launched by Person C's form submission activity after a
 * successful registration write to Firestore.
 *
 * Receives all event data and userId via Intent.
 */
public class RegistrationConfirmationActivity extends AppCompatActivity {

    private TextView tvEventTitle;
    private TextView tvEventDate;
    private TextView tvEventTime;
    private TextView tvEventVenue;
    private TextView tvEventFee;
    private TextView tvEventOrganizer;
    private Button btnViewRegistrations;
    private Button btnBrowseMore;

    private String userId;
    private String eventTitle;
    private String eventDate;
    private String eventTime;
    private String eventVenue;
    private String eventFee;
    private String eventOrganizer;
    private String eventCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_confirmation);

        // Receive all data from Person C's form submission activity
        Intent intent = getIntent();
        userId         = intent.getStringExtra("userId");
        eventTitle     = intent.getStringExtra("eventTitle");
        eventDate      = intent.getStringExtra("eventDate");
        eventTime      = intent.getStringExtra("eventTime");
        eventVenue     = intent.getStringExtra("eventVenue");
        eventFee       = intent.getStringExtra("eventFee");
        eventOrganizer = intent.getStringExtra("eventOrganizer");
        eventCategory  = intent.getStringExtra("eventCategory");

        tvEventTitle     = findViewById(R.id.tvConfirmEventTitle);
        tvEventDate      = findViewById(R.id.tvConfirmDate);
        tvEventTime      = findViewById(R.id.tvConfirmTime);
        tvEventVenue     = findViewById(R.id.tvConfirmVenue);
        tvEventFee       = findViewById(R.id.tvConfirmFee);
        tvEventOrganizer = findViewById(R.id.tvConfirmOrganizer);
        btnViewRegistrations = findViewById(R.id.btnViewMyRegistrations);
        btnBrowseMore    = findViewById(R.id.btnBrowseMoreEvents);

        populateEventDetails();

        btnViewRegistrations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(RegistrationConfirmationActivity.this,
                        MyRegistrationsActivity.class);
                i.putExtra("userId", userId);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
            }
        });

        btnBrowseMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(RegistrationConfirmationActivity.this,
                        EventBrowsingActivity.class);
                i.putExtra("userId", userId);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
            }
        });

        // Replaces deprecated onBackPressed() — handles back button and back gestures (Android 13+)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent i = new Intent(RegistrationConfirmationActivity.this,
                        AttendeeActivity.class);
                i.putExtra("userId", userId);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
            }
        });
    }

    private void populateEventDetails() {
        tvEventTitle.setText(eventTitle != null ? eventTitle : "—");
        tvEventDate.setText(eventDate != null ? eventDate : "—");
        tvEventTime.setText(eventTime != null ? eventTime : "—");
        tvEventVenue.setText(eventVenue != null ? eventVenue : "—");
        tvEventOrganizer.setText(eventOrganizer != null ? eventOrganizer : "—");

        // Show fee — handle free events
        if (eventFee == null || eventFee.trim().isEmpty()
                || eventFee.equalsIgnoreCase("free")
                || eventFee.equals("0")) {
            tvEventFee.setText("Free");
        } else {
            tvEventFee.setText(eventFee);
        }
    }
}