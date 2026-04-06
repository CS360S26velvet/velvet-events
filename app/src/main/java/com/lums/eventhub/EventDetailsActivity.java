package com.lums.eventhub;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
/**
 * EventDetailsActivity.java
 * Displays full details of a selected event including title, date,
 * time, venue, organizer, fee, seat availability, description,
 * and registration deadline.
 *
 * Allows the attendee to:
 * - Register for the event (writes to users/{userId}/registrations)
 * - Add the event to their calendar (writes to users/{userId}/calendarEvents)
 * - Navigate back to EventBrowsingActivity
 *
 * Receives all event data and userId via Intent from
 * EventBrowsingActivity or MyRegistrationsActivity.
 */

public class EventDetailsActivity extends AppCompatActivity {

    // UI Components
    private Button btnRegister, btnAddToCalendar, btnBackBottom;
    private TextView tvHeroCategory, tvHeroTitle;
    private TextView tvDate, tvTime, tvVenue, tvOrganizer, tvFee;
    private TextView tvSeats, tvSeatsPercent, tvDescription, tvRegCloses;
    private ProgressBar progressSeats;
    private ImageView imgHero;

    // Event data
    private String eventId;
    private String eventTitle;
    private String eventOrganizer;
    private String eventDate;
    private String eventVenue;
    private String eventCategory;
    private int eventSeatsBooked;
    private int eventSeatsTotal;
    private String eventDescription;
    private String eventRegClosingDate;
    private String eventTime;
    private String eventFee;

    private String userId; // ← no longer hardcoded

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_detail);

        btnBackBottom    = findViewById(R.id.btnBackBottom);
        btnRegister      = findViewById(R.id.btnRegister);
        btnAddToCalendar = findViewById(R.id.btnAddToCalendar);
        tvHeroCategory   = findViewById(R.id.tvHeroCategory);
        tvHeroTitle      = findViewById(R.id.tvHeroTitle);
        imgHero          = findViewById(R.id.imgHero);
        tvDate           = findViewById(R.id.tvDate);
        tvTime           = findViewById(R.id.tvTime);
        tvVenue          = findViewById(R.id.tvVenue);
        tvOrganizer      = findViewById(R.id.tvOrganizer);
        tvFee            = findViewById(R.id.tvFee);
        tvSeats          = findViewById(R.id.tvSeats);
        tvSeatsPercent   = findViewById(R.id.tvSeatsPercent);
        progressSeats    = findViewById(R.id.progressSeats);
        tvDescription    = findViewById(R.id.tvDescription);
        tvRegCloses      = findViewById(R.id.tvRegCloses);

        Intent intent = getIntent();

        // ← Receive userId passed from EventBrowsingActivity
        userId           = intent.getStringExtra("userId");

        eventId          = intent.getStringExtra("eventId");
        eventTitle       = intent.getStringExtra("eventTitle");
        eventOrganizer   = intent.getStringExtra("eventOrganizer");
        eventDate        = intent.getStringExtra("eventDate");
        eventVenue       = intent.getStringExtra("eventVenue");
        eventCategory    = intent.getStringExtra("eventCategory");
        eventSeatsBooked = intent.getIntExtra("eventSeatsBooked", 0);
        eventSeatsTotal  = intent.getIntExtra("eventSeatsTotal", 0);
        eventDescription = intent.getStringExtra("Description");
        eventRegClosingDate = intent.getStringExtra("RegClosingDate");
        eventTime        = intent.getStringExtra("Time");
        eventFee         = intent.getStringExtra("fee");

        displayEventData();

        // Back button — pass userId back to EventBrowsingActivity
        btnBackBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(EventDetailsActivity.this, com.lums.eventhub.EventBrowsingActivity.class);
                i.putExtra("userId", userId); // ← pass forward
                startActivity(i);
            }
        });

        // Register button
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int availableSeats = eventSeatsTotal - eventSeatsBooked;
                if (availableSeats <= 0) {
                    Toast.makeText(EventDetailsActivity.this, "Sorry, this event is fully booked!", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    if (!isRegistrationOpen()) {
                        Toast.makeText(EventDetailsActivity.this, "Sorry, the deadline has already passed!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                registerForEvent();
            }
        });

        // Add to Calendar button
        btnAddToCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<String, Object> event_Cal = new HashMap<>();
                event_Cal.put("title",    eventTitle);
                event_Cal.put("venue",    eventVenue);
                event_Cal.put("date",     eventDate);
                event_Cal.put("time",     eventTime);
                event_Cal.put("category", eventCategory);

                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId) // ← uses real userId
                        .collection("calendarEvents")
                        .document(eventId)
                        .set(event_Cal)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(EventDetailsActivity.this, "Added to Calendar!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(EventDetailsActivity.this, "Failed to add to calendar", Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    @SuppressLint({"UseCompatLoadingForColorStateLists", "ResourceType"})
    private void displayEventData() {
        tvHeroCategory.setText(eventCategory);
        tvHeroTitle.setText(eventTitle);

        if ("Society Events".equals(eventCategory)) {
            tvHeroCategory.setBackgroundColor(0xFFE91E8C);
        } else {
            tvHeroCategory.setBackgroundColor(0xFF7B2FBE);
        }

        tvDate.setText(eventDate);
        tvTime.setText(eventTime);
        tvVenue.setText(eventVenue);
        tvOrganizer.setText(eventOrganizer);
        tvFee.setText(eventFee);

        int availableSeats = eventSeatsTotal - eventSeatsBooked;
        tvSeats.setText(availableSeats + " / " + eventSeatsTotal + " seats available");

        int percentFull = 0;
        if (eventSeatsTotal > 0) {
            percentFull = (eventSeatsBooked * 100) / eventSeatsTotal;
        }
        tvSeatsPercent.setText(percentFull + "% full");
        progressSeats.setProgress(percentFull);

        if (availableSeats <= 0) {
            tvSeats.setTextColor(0xFFE53935);
            tvSeatsPercent.setTextColor(0xFFE53935);
            btnRegister.setEnabled(false);
            btnRegister.setText("Sold Out");
            btnRegister.setBackgroundTintList(getResources().getColorStateList(0xFFAAAAAA));
        } else if (availableSeats < eventSeatsTotal * 0.2) {
            tvSeats.setTextColor(0xFFF5A623);
            tvSeatsPercent.setTextColor(0xFFF5A623);
        } else {
            tvSeats.setTextColor(0xFF1A1A2E);
            tvSeatsPercent.setTextColor(0xFF4CAF50);
        }

        tvDescription.setText(eventDescription);
        tvRegCloses.setText("Registration closes " + eventRegClosingDate);
    }

    private boolean isRegistrationOpen() throws ParseException {
        Date currentDate = new Date();
        SimpleDateFormat formatted_date = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        Date formatted_deadline = formatted_date.parse(eventDate);
        return currentDate.before(formatted_deadline);
    }

    private void registerForEvent() {
        Map<String, Object> registrationData = new HashMap<>();
        registrationData.put("eventId",        eventId);
        registrationData.put("eventTitle",     eventTitle);
        registrationData.put("organizer",      eventOrganizer);
        registrationData.put("date",           eventDate);
        registrationData.put("venue",          eventVenue);
        registrationData.put("time",           eventTime);
        registrationData.put("fee",            eventFee);
        registrationData.put("description",    eventDescription);
        registrationData.put("RegClosingDate", eventRegClosingDate);
        registrationData.put("category",       eventCategory);
        registrationData.put("seatsBooked",    eventSeatsBooked);
        registrationData.put("seatsTotal",     eventSeatsTotal);

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId) // ← uses real userId
                .collection("registrations")
                .document(eventId)
                .set(registrationData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
                    btnRegister.setEnabled(false);
                    btnRegister.setText("Registered ✓");
                    btnRegister.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF4CAF50));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}