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
 * EventBrowsingActivity.java  (UPDATED)
 *
 * CHANGES:
 *   1. Receives and forwards "username" (e.g. "#AT_Bilal") so EventDetailsActivity
 *      can extract the attendee name for the payment verification list
 *   2. Loads from BOTH events/ (status==Approved) AND proposals/ (status==Approved)
 *      so org-submitted events show up on attendee side
 *   3. Passes eventId only to EventDetailsActivity — all other data loaded live from
 *      Firestore inside EventDetailsActivity (startDate, endDate, fees, schedule etc.)
 *   Everything else identical to original.
 */
public class EventBrowsingActivity extends AppCompatActivity {

    EditText etSearch;
    Button searchbtn;
    Button filterALLbtn, filterSocietybtn, filterWorkshopbtn;
    Button navBrowseEvents, navMyRegistrations, navNotifs, navHome, logout;
    TextView count_results;
    String filter = "All";

    LinearLayout eventGrid;
    List<Event> allEvents      = new ArrayList<>();
    List<Event> filteredEvents = new ArrayList<>();

    private String userId;
    private String username;   // NEW — forwarded to EventDetailsActivity

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_browsing);

        userId   = getIntent().getStringExtra("userId");
        username = getIntent().getStringExtra("username");   // NEW
        if (username == null) username = "";

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

        searchbtn.setOnClickListener(v ->
                filter_events(etSearch.getText().toString().trim()));

        filterALLbtn.setOnClickListener(v -> {
            filter = "All";
            filter_events(etSearch.getText().toString().trim());
        });
        filterWorkshopbtn.setOnClickListener(v -> {
            filter = "Workshops/Seminars";
            filter_events(etSearch.getText().toString().trim());
        });
        filterSocietybtn.setOnClickListener(v -> {
            filter = "Society Events";
            filter_events(etSearch.getText().toString().trim());
        });

        navMyRegistrations.setOnClickListener(v -> {
            Intent i = new Intent(this, MyRegistrationsActivity.class);
            i.putExtra("userId",   userId);
            i.putExtra("username", username);
            startActivity(i);
        });

        navHome.setOnClickListener(v -> {
            Intent i = new Intent(this, AttendeeActivity.class);
            i.putExtra("userId",   userId);
            i.putExtra("username", username);
            startActivity(i);
        });

        navNotifs.setOnClickListener(v -> {
            Intent i = new Intent(this, NotificationsActivity.class);
            i.putExtra("userId",   userId);
            i.putExtra("username", username);
            startActivity(i);
        });

        logout.setOnClickListener(v -> {
            startActivity(new Intent(this, com.lums.eventhub.auth.LoginActivity.class));
            finish();
        });

        navBrowseEvents.setOnClickListener(v -> { /* already here */ });
    }

    // ── Filter & display ──────────────────────────────────────────────────────

    private void filter_events(String text) {
        filteredEvents.clear();
        for (Event event : allEvents) {
            if (!"All".equals(filter) && !filter.equals(event.category)) continue;
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
        Button   btnDetails  = card.findViewById(R.id.btnViewDetails);

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
            intent.putExtra("userId",        userId);
            intent.putExtra("username",      username);       // NEW — pass forward
            intent.putExtra("eventId",       event.id);
            intent.putExtra("eventTitle",    event.title);
            intent.putExtra("eventOrganizer",event.organizer);
            intent.putExtra("eventCategory", event.category);
            // All other data (dates, fees, schedule) loaded live inside EventDetailsActivity
            startActivity(intent);
        });

        return card;
    }

    // ── Load from Firestore ───────────────────────────────────────────────────

    private void loadEventsFromFirebase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Load from events/ (admin-approved)
        db.collection("events")
                .whereEqualTo("status", "Approved")
                .get()
                .addOnSuccessListener(snap -> {
                    allEvents.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        allEvents.add(eventFromDoc(doc));
                    }
                    // Also load from proposals/ with status Approved
                    loadApprovedProposals(db);
                })
                .addOnFailureListener(e -> loadApprovedProposals(db));
    }

    private void loadApprovedProposals(FirebaseFirestore db) {
        db.collection("proposals")
                .whereEqualTo("status", "Approved")
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        // Avoid duplicates
                        String id = doc.getId();
                        boolean exists = false;
                        for (Event e : allEvents) if (e.id.equals(id)) { exists = true; break; }
                        if (!exists) allEvents.add(eventFromDoc(doc));
                    }
                    filter = "All";
                    filter_events("");
                })
                .addOnFailureListener(e -> {
                    filter = "All";
                    filter_events("");
                    count_results.setText("Failed to load events");
                });
    }

    private Event eventFromDoc(QueryDocumentSnapshot doc) {
        // Use startDate if available, fall back to date
        String date = doc.getString("startDate");
        if (date == null || date.isEmpty()) date = doc.getString("date");

        String category = doc.getString("category");
        if (category == null || category.isEmpty()) {
            String type = doc.getString("eventType");
            category = "School-Led Workshop".equals(type) ? "Workshops/Seminars" : "Society Events";
        }

        return new Event(
                doc.getId(),
                nvl(doc.getString("title"), "Untitled"),
                nvl(doc.getString("organizer"), nvl(doc.getString("societyName"), "")),
                nvl(date, ""),
                nvl(doc.getString("venue"), ""),
                category,
                nvl(doc.getString("description"), ""),
                nvl(doc.getString("endDate"), ""),
                "",   // time field removed
                nvl(doc.getString("regFee"), ""),
                doc.getLong("seatsBooked") != null ? doc.getLong("seatsBooked").intValue() : 0,
                doc.getLong("expectedParticipants") != null
                        ? doc.getLong("expectedParticipants").intValue()
                        : (doc.getLong("seatsTotal") != null ? doc.getLong("seatsTotal").intValue() : 0)
        );
    }

    private String nvl(String s, String fallback) {
        return (s != null && !s.isEmpty()) ? s : fallback;
    }
}