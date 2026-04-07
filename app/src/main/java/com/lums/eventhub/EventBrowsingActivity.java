package com.lums.eventhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
/**
 * EventBrowsingActivity.java
 * Displays all approved events fetched from Firestore.
 * Allows attendees to search events by title and filter
 * by category (All, Society Events, Workshops/Seminars).
 * Each event card has a "View Details" button that navigates
 * to EventDetailsActivity.
 *
 * Receives userId via Intent and passes it forward to all
 * subsequent activities.
 */

public class EventBrowsingActivity extends AppCompatActivity {
    // Search
    EditText etSearch;
    Button searchbtn;
    // Filter buttons
    Button filterALLbtn, filterSocietybtn, filterWorkshopbtn;
    Button navBrowseEvents, navMyRegistrations, navNotifs, navHome, logout;
    TextView count_results;
    String filter = "All";

    LinearLayout eventGrid;
    List<Event> allEvents = new ArrayList<>();
    List<Event> filteredEvents = new ArrayList<>();

    private String userId; // ← no longer hardcoded

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_browsing);

        // ← Receive userId from previous activity
        userId = getIntent().getStringExtra("userId");

        etSearch           = findViewById(R.id.etSearch);
        searchbtn          = findViewById(R.id.btnSearch);
        filterALLbtn       = findViewById(R.id.btnFilterAll);
        filterSocietybtn   = findViewById(R.id.btnFilterSociety);
        filterWorkshopbtn  = findViewById(R.id.btnFilterWorkshops);
        count_results      = findViewById(R.id.tvResultsCount);
        eventGrid          = findViewById(R.id.eventGrid);
        navBrowseEvents    = findViewById(R.id.navBrowseEvents);
        navMyRegistrations = findViewById(R.id.navMyRegistrations);
        navNotifs          = findViewById(R.id.navNotifications);
        navHome            = findViewById(R.id.navDashboard);
        logout             = findViewById(R.id.btnLogout);

        loadEventsFromFirebase();

        searchbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = etSearch.getText().toString().trim();
                filter_events(text);
            }
        });

        filterALLbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter = "All";
                filter_events(etSearch.getText().toString().trim());
            }
        });

        filterWorkshopbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter = "Workshops/Seminars";
                filter_events(etSearch.getText().toString().trim());
            }
        });

        filterSocietybtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter = "Society Events";
                filter_events(etSearch.getText().toString().trim());
            }
        });

        navMyRegistrations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EventBrowsingActivity.this, MyRegistrationsActivity.class);
                intent.putExtra("userId", userId); // ← pass forward
                startActivity(intent);
            }
        });

        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EventBrowsingActivity.this, com.lums.eventhub.AttendeeActivity.class);
                intent.putExtra("userId", userId); // ← pass forward
                startActivity(intent);
            }
        });

        navNotifs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(EventBrowsingActivity.this, NotificationsActivity.class);
                intent.putExtra("userId", userId); // ← pass forward
                startActivity(intent);
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(EventBrowsingActivity.this, com.lums.eventhub.auth.LoginActivity.class));
                finish(); // ← clear from back stack on logout
            }
        });

        navBrowseEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // already here
            }
        });
    }

    private void filter_events(String text) {
        filteredEvents.clear();
        for (Event event : allEvents) {
            if (!filter.equals("All") && !event.category.equals(filter)) continue;
            if (!text.isEmpty() && !event.title.toLowerCase().contains(text.toLowerCase())) continue;
            filteredEvents.add(event);
        }
        count_results.setText(filteredEvents.size() + " events found");
        show_event_grid();
    }

    private void show_event_grid() {
        if (eventGrid == null) return;
        eventGrid.removeAllViews();

        for (int i = 0; i < filteredEvents.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, 12);
            row.setLayoutParams(rowParams);

            View card1 = createEventCard(filteredEvents.get(i));
            LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            p1.setMargins(0, 0, 6, 0);
            card1.setLayoutParams(p1);
            row.addView(card1);

            if (i + 1 < filteredEvents.size()) {
                View card2 = createEventCard(filteredEvents.get(i + 1));
                LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                p2.setMargins(6, 0, 0, 0);
                card2.setLayoutParams(p2);
                row.addView(card2);
            } else {
                View spacer = new View(this);
                spacer.setLayoutParams(new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(spacer);
            }

            eventGrid.addView(row);
        }
    }

    private View createEventCard(Event event) {
        View card = LayoutInflater.from(this).inflate(R.layout.event_card, null);

        TextView tvCategory  = card.findViewById(R.id.tvCategory);
        TextView tvTitle     = card.findViewById(R.id.tvEventTitle);
        TextView tvOrganizer = card.findViewById(R.id.tvOrganizer);
        TextView tvDate      = card.findViewById(R.id.tvDate);
        TextView tvVenue     = card.findViewById(R.id.tvVenue);
        TextView tvSeats     = card.findViewById(R.id.tvSeats);
        Button btnDetails    = card.findViewById(R.id.btnViewDetails);

        tvCategory.setText(event.category);
        tvTitle.setText(event.title);
        tvOrganizer.setText(event.organizer);
        tvDate.setText(event.date);
        tvVenue.setText(event.venue);
        tvSeats.setText((event.seatsTotal - event.seatsbooked) + " / " + event.seatsTotal + " seats available");

        if ("Society Events".equals(event.category)) {
            tvCategory.setBackgroundColor(0xFFE91E8C);
        } else {
            tvCategory.setBackgroundColor(0xFF00BCD4);
        }

        btnDetails.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventDetailsActivity.class);
            intent.putExtra("userId", userId); // ← pass forward
            intent.putExtra("eventId", event.id);
            intent.putExtra("eventTitle", event.title);
            intent.putExtra("eventOrganizer", event.organizer);
            intent.putExtra("eventDate", event.date);
            intent.putExtra("eventVenue", event.venue);
            intent.putExtra("eventCategory", event.category);
            intent.putExtra("eventSeatsBooked", event.seatsbooked);
            intent.putExtra("eventSeatsTotal", event.seatsTotal);
            intent.putExtra("Description", event.desc);
            intent.putExtra("RegClosingDate", event.deadline);
            intent.putExtra("Time", event.time);
            intent.putExtra("fee", event.fee);
            startActivity(intent);
        });

        return card;
    }

    private void loadEventsFromFirebase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events")
                .whereEqualTo("status", "Approved")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allEvents.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Event event = new Event(
                                doc.getId(),
                                doc.getString("title"),
                                doc.getString("organizer"),
                                doc.getString("date"),
                                doc.getString("venue"),
                                doc.getString("category"),
                                doc.getString("description"),
                                doc.getString("RegistrationClosingDate"),
                                doc.getString("Time"),
                                doc.getString("fee"),
                                doc.getLong("seatsBooked") != null ? doc.getLong("seatsBooked").intValue() : 0,
                                doc.getLong("seatsTotal") != null ? doc.getLong("seatsTotal").intValue() : 0
                        );
                        allEvents.add(event);
                    }
                    filter = "All";
                    filter_events("");
                })
                .addOnFailureListener(e -> count_results.setText("Failed to load events"));
    }
}