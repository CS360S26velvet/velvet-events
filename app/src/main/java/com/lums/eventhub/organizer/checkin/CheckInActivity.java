package com.lums.eventhub.organizer.checkin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.lums.eventhub.R;

import java.util.ArrayList;
import java.util.List;

/**
 * CheckInActivity.java  (UPDATED)
 *
 * Now shows a LIST of all Approved events for this organiser.
 * Each event has a "View Participants" button which opens
 * CheckInParticipantsActivity — where only attendees whose
 * paymentStatus == "Approved" are shown and can be checked in.
 *
 * Receives from OrganizerDashboardActivity:
 *   "organizerUsername" — used to filter proposals/events
 */
public class CheckInActivity extends AppCompatActivity {

    private RecyclerView  recyclerView;
    private EventAdapter  adapter;
    private final List<EventItem> eventList = new ArrayList<>();

    private String organizerUsername = "ORG0012";
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);

        db = FirebaseFirestore.getInstance();
        organizerUsername = getIntent().getStringExtra("organizerUsername");
        if (organizerUsername == null) organizerUsername = "ORG0012";

        recyclerView = findViewById(R.id.recyclerView);
        adapter = new EventAdapter(eventList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnCheckInBack).setOnClickListener(v -> finish());

        loadApprovedEvents();
    }

    // ── Load all Approved events for this organiser ────────────────────────────

    private void loadApprovedEvents() {
        eventList.clear();

        // Load from proposals/ with status == Approved
        db.collection("proposals")
                .whereEqualTo("organizerUsername", organizerUsername)
                .whereEqualTo("status", "Approved")
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        String title = nvl(doc.getString("title"), "Untitled");
                        String venue = nvl(doc.getString("venue"), "");
                        String date  = nvl(doc.getString("startDate"),
                                nvl(doc.getString("date"), ""));
                        eventList.add(new EventItem(doc.getId(), title, venue, date));
                    }
                    loadApprovedFromEvents();
                })
                .addOnFailureListener(e -> loadApprovedFromEvents());
    }

    private void loadApprovedFromEvents() {
        // Also load from events/ collection (admin-approved)
        db.collection("events")
                .whereEqualTo("organizerUsername", organizerUsername)
                .whereEqualTo("status", "Approved")
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        // Avoid duplicates
                        String id = doc.getId();
                        boolean exists = false;
                        for (EventItem e : eventList) if (e.id.equals(id)) { exists = true; break; }
                        if (!exists) {
                            String title = nvl(doc.getString("title"), "Untitled");
                            String venue = nvl(doc.getString("venue"), "");
                            String date  = nvl(doc.getString("startDate"),
                                    nvl(doc.getString("date"), ""));
                            eventList.add(new EventItem(id, title, venue, date));
                        }
                    }
                    adapter.notifyDataSetChanged();

                    TextView tvTitle = findViewById(R.id.tvCheckInTitle);
                    TextView tvSub   = findViewById(R.id.tvCheckInSubtitle);
                    if (tvTitle != null) tvTitle.setText("Live Check-In");
                    if (tvSub   != null) tvSub.setText(eventList.size() + " approved event(s)");
                })
                .addOnFailureListener(e -> adapter.notifyDataSetChanged());
    }

    private String nvl(String s, String fallback) {
        return (s != null && !s.isEmpty()) ? s : fallback;
    }

    // ── Model ──────────────────────────────────────────────────────────────────

    static class EventItem {
        String id, title, venue, date;
        EventItem(String id, String title, String venue, String date) {
            this.id = id; this.title = title; this.venue = venue; this.date = date;
        }
    }

    // ── Adapter ────────────────────────────────────────────────────────────────

    class EventAdapter extends RecyclerView.Adapter<EventAdapter.VH> {

        private final List<EventItem> list;
        EventAdapter(List<EventItem> list) { this.list = list; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_check_in_event, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int position) {
            EventItem ev = list.get(position);
            h.tvTitle.setText(ev.title);
            h.tvVenue.setText(ev.venue.isEmpty() ? "—" : "📍 " + ev.venue);
            h.tvDate.setText(ev.date.isEmpty()  ? "—" : "📅 " + ev.date);

            h.btnViewParticipants.setOnClickListener(v -> {
                Intent intent = new Intent(CheckInActivity.this,
                        CheckInParticipantsActivity.class);
                intent.putExtra("eventId",    ev.id);
                intent.putExtra("eventTitle", ev.title);
                intent.putExtra("eventVenue", ev.venue);
                startActivity(intent);
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvVenue, tvDate;
            Button   btnViewParticipants;
            VH(View v) {
                super(v);
                tvTitle            = v.findViewById(R.id.tvCheckInEventTitle);
                tvVenue            = v.findViewById(R.id.tvCheckInEventVenue);
                tvDate             = v.findViewById(R.id.tvCheckInEventDate);
                btnViewParticipants= v.findViewById(R.id.btnViewParticipants);
            }
        }
    }
}