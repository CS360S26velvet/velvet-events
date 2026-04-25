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

import com.google.firebase.firestore.DocumentSnapshot;
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
 * - Navigate back via the back button
 *
 * Back button behaviour is source-aware:
 * - source = "myRegistrations" → goes back to MyRegistrationsActivity
 * - source = null (default)   → goes back to EventBrowsingActivity
 *
 * On load, checks Firestore to see if the user is already registered
 * for this event — if so, the Register button is disabled and shows
 * "Already Registered ✓" to prevent duplicate registrations.
 *
 * Receives all event data, userId, and optional source via Intent.
 */
public class EventDetailsActivity extends AppCompatActivity {

    private Button btnRegister, btnAddToCalendar, btnBackBottom;
    private TextView tvHeroCategory, tvHeroTitle;
    private TextView tvDate, tvTime, tvVenue, tvOrganizer, tvFee;
    private TextView tvSeats, tvSeatsPercent, tvDescription, tvRegCloses;
    private ProgressBar progressSeats;
    private ImageView imgHero;

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

    private String userId;
    private String source; // "myRegistrations" or null

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
        userId              = intent.getStringExtra("userId");
        source              = intent.getStringExtra("source"); // null if coming from browsing
        eventId             = intent.getStringExtra("eventId");
        eventTitle          = intent.getStringExtra("eventTitle");
        eventOrganizer      = intent.getStringExtra("eventOrganizer");
        eventDate           = intent.getStringExtra("eventDate");
        eventVenue          = intent.getStringExtra("eventVenue");
        eventCategory       = intent.getStringExtra("eventCategory");
        eventSeatsBooked    = intent.getIntExtra("eventSeatsBooked", 0);
        eventSeatsTotal     = intent.getIntExtra("eventSeatsTotal", 0);
        eventDescription    = intent.getStringExtra("Description");
        eventRegClosingDate = intent.getStringExtra("RegClosingDate");
        eventTime           = intent.getStringExtra("Time");
        eventFee            = intent.getStringExtra("fee");

        displayEventData();

        // Check Firestore on load — disables Register button if already registered
        checkIfAlreadyRegistered();

        // Source-aware back button
        btnBackBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateBack();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int availableSeats = eventSeatsTotal - eventSeatsBooked;
                if (availableSeats <= 0) {
                    Toast.makeText(EventDetailsActivity.this,
                            "Sorry, this event is fully booked!", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    if (!isRegistrationOpen()) {
                        Toast.makeText(EventDetailsActivity.this,
                                "Sorry, the deadline has already passed!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                registerForEvent();
            }
        });

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
                        .document(userId)
                        .collection("calendarEvents")
                        .document(eventId)
                        .set(event_Cal)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(EventDetailsActivity.this,
                                    "Added to Calendar!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(EventDetailsActivity.this,
                                    "Failed to add to calendar", Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    /**
     * Navigates back to the correct activity depending on where
     * the user came from.
     */
    private void navigateBack() {
        if ("myRegistrations".equals(source)) {
            Intent i = new Intent(EventDetailsActivity.this,
                    MyRegistrationsActivity.class);
            i.putExtra("userId", userId);
            startActivity(i);
        } else {
            Intent i = new Intent(EventDetailsActivity.this,
                    com.lums.eventhub.EventBrowsingActivity.class);
            i.putExtra("userId", userId);
            startActivity(i);
        }
        finish();
    }

    /**
     * Checks whether the logged-in user already has a registration
     * document for this event. If they do, the Register button is
     * disabled immediately so they cannot register twice.
     *
     * Uses eventId as the document ID in the registrations subcollection
     * (matching the .document(eventId).set(...) write in registerForEvent()).
     */
    private void checkIfAlreadyRegistered() {
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("registrations")
                .document(eventId)
                .get()
                .addOnSuccessListener((DocumentSnapshot doc) -> {
                    if (doc.exists()) {
                        setAlreadyRegisteredState();
                    }
                })
                .addOnFailureListener(e -> {
                    // Silently fail — button stays enabled, Firestore write
                    // will simply overwrite if they somehow submit again
                });
    }

    /**
     * Puts the Register button into a disabled "Already Registered" state.
     * Called both from checkIfAlreadyRegistered() and after a successful
     * registration write.
     */
    private void setAlreadyRegisteredState() {
        btnRegister.setEnabled(false);
        btnRegister.setText("Already Registered ✓");
        btnRegister.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF4CAF50));
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
            btnRegister.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFAAAAAA));
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
        registrationData.put("registeredAt",
                com.google.firebase.firestore.FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("registrations")
                .document(eventId) // eventId as doc ID prevents duplicates at DB level too
                .set(registrationData)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
                    setAlreadyRegisteredState();

                    // TODO: replace with launch to Person C's form activity once integrated
                    // Intent confirmIntent = new Intent(this, RegistrationConfirmationActivity.class);
                    // confirmIntent.putExtra("userId", userId);
                    // ... pass event extras
                    // startActivity(confirmIntent);
                    // finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}