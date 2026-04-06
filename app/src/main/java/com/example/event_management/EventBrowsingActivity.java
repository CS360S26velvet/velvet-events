package com.example.event_management;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
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

public class EventBrowsingActivity extends AppCompatActivity {
    //search
    EditText etSearch;
    Button searchbtn;
    //filter buttons
    Button filterALLbtn, filterSocietybtn, filterWorkshopbtn;
    Button navBrowseEvents, navMyRegistrations, navNotifs,navHome, logout;
    TextView count_results;
    String filter= "All";

    LinearLayout eventGrid;
    List<Event> allEvents = new ArrayList<>();
    List<Event> filteredEvents = new ArrayList<>();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_browsing);
        etSearch = findViewById(R.id.etSearch);
        searchbtn = findViewById(R.id.btnSearch);
        filterALLbtn = findViewById(R.id.btnFilterAll);
        filterSocietybtn = findViewById(R.id.btnFilterSociety);
        filterWorkshopbtn = findViewById(R.id.btnFilterWorkshops);
        count_results = findViewById(R.id.tvResultsCount);
        eventGrid = findViewById(R.id.eventGrid);
        navBrowseEvents = findViewById(R.id.navBrowseEvents);
        navMyRegistrations = findViewById(R.id.navMyRegistrations);
        navNotifs = findViewById(R.id.navNotifications);
        navHome = findViewById(R.id.navDashboard);
        logout = findViewById(R.id.btnLogout);

        loadEventsFromFirebase();

        //search logic
        //as the user types something it will be automatically displayed on the text box
        //user then clicks enter button to get results
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
                String text = etSearch.getText().toString().trim();
                filter_events(text);
            }
        });
        filterWorkshopbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter= "Workshops/Seminars";
                String text = etSearch.getText().toString().trim();
                filter_events(text);
            }
        });
        filterSocietybtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filter= "Society Events";
                String text = etSearch.getText().toString().trim();
                filter_events(text);
            }
        });

        navMyRegistrations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(EventBrowsingActivity.this,MyRegistrationsActivity.class));
            }
        });

        navHome.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                startActivity(new Intent(EventBrowsingActivity.this, AttendeeActivity.class));
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(EventBrowsingActivity.this, MainActivity.class));
            }//will go back to main activity which is the login interface
        });
        navNotifs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(EventBrowsingActivity.this,NotificationsActivity.class));
            }
        });
        navBrowseEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //do nothing already on browsing page
            }
        });
    }

    /**
     * This function sees the events in the all_events list and applies a filter based on the
     * "All", "Society Events", "Workshops/Seminars"
     * and based on any text in the search bar as well
     * @param text
     */
    private void filter_events(String text){
        filteredEvents.clear();
        for(Event event : allEvents){
            if(!filter.equals("All") && !event.category.equals(filter)){
                continue;
            }
            if(!text.isEmpty() && !event.title.toLowerCase().contains(text.toLowerCase())){
                continue;
            }
            filteredEvents.add(event);
        }
        count_results.setText(filteredEvents.size() + " events found");
        show_event_grid();
    }

    /**
     * this function displays the entire grid of events
     */
    private void show_event_grid(){
        if(eventGrid==null){return;} //_---> screen crashes if this is null otherwise
        eventGrid.removeAllViews();

        for (int i = 0; i < filteredEvents.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowParams.setMargins(0, 0, 0, 12);
            row.setLayoutParams(rowParams);

            // Left card
            View card1 = createEventCard(filteredEvents.get(i));
            LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            p1.setMargins(0, 0, 6, 0);
            card1.setLayoutParams(p1);
            row.addView(card1);

            // Right card or empty spacer
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

    /**
     * this function creates a card for one event to be displayed on the grid
     * @param event
     * @return
     */
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

        // Badge color
        if ("Society Events".equals(event.category)) {
            tvCategory.setBackgroundColor(0xFFE91E8C); // pink
        } else {
            tvCategory.setBackgroundColor(0xFF00BCD4); // cyan
        }

        // Navigate to detail screen
        btnDetails.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventDetailsActivity.class);
            intent.putExtra("eventId", event.id);
            intent.putExtra("eventTitle", event.title);
            intent.putExtra("eventOrganizer", event.organizer);
            intent.putExtra("eventDate", event.date);
            intent.putExtra("eventVenue", event.venue);
            intent.putExtra("eventCategory", event.category);
            intent.putExtra("eventSeatsBooked", event.seatsbooked);
            intent.putExtra("eventSeatsTotal", event.seatsTotal);
            intent.putExtra("Description",event.desc );
            intent.putExtra("RegClosingDate", event.deadline);
            intent.putExtra("Time", event.time);
            intent.putExtra("fee", event.fee);
            startActivity(intent);
        });

        return card;
    }

    /**
     * this function loads all the approved events from the firebase
     */
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
                    filter="All";
                    filter_events("");
                })
                .addOnFailureListener(e -> count_results.setText("Failed to load events"));
    }
    //hardcoding events for now
//    private void loadHardcodedEvents() {
//        allEvents.clear();
//
//        allEvents.add(new Event("1", "Physics Seminar: Diagrammatic Vector Algebra",
//                "SBASSE", "Mar 6, 2026 | 2:30 PM", "SBASSE 10-204",
//                "Workshops/Seminars", 40, 60));
//
//        allEvents.add(new Event("2", "Basketball Championship Finals",
//                "Sports Society", "Mar 15, 2026 | 4:00 PM", "Sports Complex",
//                "Society Events", 200, 500));
//
//        allEvents.add(new Event("3", "Startup Weekend LUMS",
//                "SPADES", "Apr 4, 2026 | 9:00 AM", "SDSB Atrium",
//                "Society Events", 120, 200));
//
//        allEvents.add(new Event("4", "LUMS Cultural Night",
//                "Cultural Society", "Apr 20, 2026 | 6:00 PM", "Amphitheater",
//                "Society Events", 300, 500));
//
//        allEvents.add(new Event("5", "AI & Machine Learning Workshop",
//                "LUMS CS Society", "Apr 10, 2026 | 3:00 PM", "SBASSE Lab 1",
//                "Workshops/Seminars", 30, 50));
//
//        allEvents.add(new Event("6", "Annual Debate Competition",
//                "Debating Society", "Apr 25, 2026 | 2:00 PM", "Auditorium",
//                "Society Events", 50, 300));
//
//        filter("All");
//    }

}
