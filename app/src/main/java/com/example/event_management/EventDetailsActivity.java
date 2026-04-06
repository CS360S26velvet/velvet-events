package com.example.event_management;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.icu.text.CaseMap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Firebase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EventDetailsActivity extends AppCompatActivity {

    // UI Components
    private Button btnBack, btnRegister, btnAddToCalendar, btnBackBottom;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_detail);

        btnBackBottom = findViewById(R.id.btnBackBottom);

        btnRegister = findViewById(R.id.btnRegister);
        btnAddToCalendar = findViewById(R.id.btnAddToCalendar);

        tvHeroCategory = findViewById(R.id.tvHeroCategory);
        tvHeroTitle = findViewById(R.id.tvHeroTitle);
        imgHero = findViewById(R.id.imgHero);

        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        tvVenue = findViewById(R.id.tvVenue);
        tvOrganizer = findViewById(R.id.tvOrganizer);
        tvFee = findViewById(R.id.tvFee);

        tvSeats = findViewById(R.id.tvSeats);
        tvSeatsPercent = findViewById(R.id.tvSeatsPercent);
        progressSeats = findViewById(R.id.progressSeats);

        tvDescription = findViewById(R.id.tvDescription);
        tvRegCloses = findViewById(R.id.tvRegCloses);

        Intent intent = getIntent();

        eventId = intent.getStringExtra("eventId");
        eventTitle = intent.getStringExtra("eventTitle");
        eventOrganizer = intent.getStringExtra("eventOrganizer");
        eventDate = intent.getStringExtra("eventDate");
        eventVenue = intent.getStringExtra("eventVenue");
        eventCategory = intent.getStringExtra("eventCategory");
        eventSeatsBooked = intent.getIntExtra("eventSeatsBooked", 0);
        eventSeatsTotal = intent.getIntExtra("eventSeatsTotal", 0);
        eventDescription = intent.getStringExtra("Description");
        eventRegClosingDate = intent.getStringExtra("RegClosingDate");
        eventTime = intent.getStringExtra("Time");
        eventFee = intent.getStringExtra("fee");

        displayEventData();

        // Back button (bottom)
        btnBackBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(EventDetailsActivity.this, EventBrowsingActivity.class));
            }
        });

        // Register button
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int availableSeats = eventSeatsTotal-eventSeatsBooked;
                if (availableSeats <= 0) {
                    Toast.makeText(EventDetailsActivity.this, "Sorry, this event is fully booked!", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    boolean reg_open_flag=isRegistrationOpen();
                    if(!reg_open_flag){
                        Toast.makeText(EventDetailsActivity.this, "Sorry, the deadline to register for this event has already passed!", Toast.LENGTH_SHORT).show();
                    }
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                //setActivity(new Intent(EventDetailsActivity.this, RegistrationActivity.class));
            }
        });

        // Add to Calendar button
        btnAddToCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                String userId = "3khY0RCTezX40llTbDbz";
                Map<String, Object> event_Cal = new HashMap<>();

//                //pass information to calendar screen
                event_Cal.put("title", eventTitle);
                event_Cal.put("venue", eventVenue);
                event_Cal.put("date", eventDate);
                event_Cal.put("time", eventTime);
                event_Cal.put("category",eventCategory);
                //add this to calendar firebase

                FirebaseFirestore.getInstance().collection("users").document(userId).collection("calendarEvents").document(eventId).set(event_Cal).addOnSuccessListener(unused -> {
                    Toast.makeText(EventDetailsActivity.this, "Added to Calendar!", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e->{
                    Toast.makeText(EventDetailsActivity.this, "failed to add to calendar", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * this function displays the details of event
     */
    @SuppressLint({"UseCompatLoadingForColorStateLists", "ResourceType"})
    private void displayEventData() {
        tvHeroCategory.setText(eventCategory);
        tvHeroTitle.setText(eventTitle);

        if ("Society Events".equals(eventCategory)) {
            tvHeroCategory.setBackgroundColor(0xFFE91E8C);
        } else {
            tvHeroCategory.setBackgroundColor(0xFF7B2FBE);
        }

        // Event details
        tvDate.setText(eventDate);
        tvTime.setText(eventTime);
        tvVenue.setText(eventVenue);
        tvOrganizer.setText(eventOrganizer);
        tvFee.setText(eventFee);
        // Capacity
        int availableSeats = eventSeatsTotal - eventSeatsBooked;
        tvSeats.setText(availableSeats + " / " + eventSeatsTotal + " seats available");
        //percentage
        int percentFull = 0;
        if (eventSeatsTotal > 0) {
            percentFull = (eventSeatsBooked * 100) / eventSeatsTotal;
        }
        tvSeatsPercent.setText(percentFull + "% full");
        progressSeats.setProgress(percentFull);

        // Change color based on availability
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

        // Description
        tvDescription.setText(eventDescription);

        // Registration closing date
        tvRegCloses.setText("Registration closes " + eventRegClosingDate);
    }

    //check if registration is still open

    /**
     * this function checks if the current date has passed the events registration deadline
     * @return
     * @throws ParseException
     */
    private boolean isRegistrationOpen() throws ParseException {
        Date currentDate = new Date();
        SimpleDateFormat formatted_date = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
        Date formatted_deadline = formatted_date.parse(eventDate);
        return currentDate.before(formatted_deadline);
    }
}