package com.lums.eventhub;

/**
 * OrganizerDashboardActivity.java
 *
 * Full organizer dashboard implementation.
 * Receives from LoginActivity:
 *   "organizerUsername" — the #ORG_xxx username (same as organizerId, canonical field name)
 *   "societyName"       — e.g. "SPADES Society"
 *
 * Queries proposals/ where organizerUsername == organizerUsername (not hardcoded).
 *
 * Status badges:
 *   "Approved"           → green,  Edit button HIDDEN
 *   "Revision Requested" → orange, Edit button VISIBLE
 *   "Submitted"          → yellow, Edit button HIDDEN
 *   "Draft"              → grey,   Edit button VISIBLE
 *   "Rejected"           → red,    Edit button HIDDEN
 */

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class OrganizerDashboardActivity extends AppCompatActivity {

    // Received from LoginActivity — NOT hardcoded
    private String organizerUsername;
    private String societyName;

    private FirebaseFirestore db;
    private RecyclerView recyclerViewEvents;
    private EventAdapter adapter;
    private List<EventItem> eventList = new ArrayList<>();
    private LinearLayout layoutNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_dashboard);

        db = FirebaseFirestore.getInstance();

        // Get from login intent — no hardcoding
        organizerUsername = getIntent().getStringExtra("organizerUsername");
        societyName       = getIntent().getStringExtra("societyName");

        // Fallbacks in case called without extras (e.g. direct launch in dev)
        if (organizerUsername == null) organizerUsername = "ORG0012";
        if (societyName == null)       societyName       = "My Society";

        // RecyclerView
        recyclerViewEvents = findViewById(R.id.recyclerViewEvents);
        adapter = new EventAdapter(eventList);
        recyclerViewEvents.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewEvents.setAdapter(adapter);

        layoutNotifications = findViewById(R.id.layoutNotifications);

        // Register New Event → ProposalFormActivity
        Button btnRegisterNewEvent = findViewById(R.id.btnRegisterNewEvent);
        if (btnRegisterNewEvent != null) {
            btnRegisterNewEvent.setOnClickListener(v ->
                    startActivity(new Intent(this, ProposalFormActivity.class)));
        }

        // Management Console navigation
        findViewById(R.id.btnNavAttendeeReg).setOnClickListener(v ->
                startActivity(new Intent(this, AttendeeRegistrationActivity.class)));

        findViewById(R.id.btnNavRegistrants).setOnClickListener(v ->
                startActivity(new Intent(this, RegistrantDashboardActivity.class)));

        findViewById(R.id.btnNavCheckIn).setOnClickListener(v ->
                startActivity(new Intent(this, CheckInActivity.class)));

        findViewById(R.id.btnNavFormSettings).setOnClickListener(v ->
                startActivity(new Intent(this, CapacitySettingActivity.class)));

        findViewById(R.id.btnNavPayments).setOnClickListener(v ->
                android.widget.Toast.makeText(this, "Coming soon!", android.widget.Toast.LENGTH_SHORT).show());

        loadStats();
        loadEvents();
        loadNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
        loadEvents();
        loadNotifications();
    }

    // -------------------------------------------------------------------------
    // Load stats
    // -------------------------------------------------------------------------

    private void loadStats() {
        db.collection("proposals")
                .whereEqualTo("organizerUsername", organizerUsername)
                .get()
                .addOnSuccessListener(q -> {
                    int total = 0;
                    for (QueryDocumentSnapshot doc : q) {
                        String status = doc.getString("status");
                        if (!"Draft".equals(status)) total++;
                    }
                    TextView tv = findViewById(R.id.tvTotalEvents);
                    if (tv != null) tv.setText(String.valueOf(total));
                });

        db.collection("attendees")
                .whereEqualTo("paymentStatus", "Pending")
                .get()
                .addOnSuccessListener(q -> {
                    TextView tv = findViewById(R.id.tvPendingPayments);
                    if (tv != null) tv.setText(String.valueOf(q.size()));
                });
    }

    // -------------------------------------------------------------------------
    // Load events
    // -------------------------------------------------------------------------

    private void loadEvents() {
        db.collection("proposals")
                .whereEqualTo("organizerUsername", organizerUsername)
                .get()
                .addOnSuccessListener(query -> {
                    eventList.clear();

                    if (query.isEmpty()) {
                        loadSampleEvents();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : query) {
                        String title  = doc.getString("title");
                        String date   = doc.getString("date");
                        if (date == null) date = doc.getString("eventDate");
                        String status = doc.getString("status");
                        if (title  == null) title  = "Untitled";
                        if (date   == null) date   = "—";
                        if (status == null) status = "Draft";
                        eventList.add(new EventItem(doc.getId(), title, date, status));
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> loadSampleEvents());
    }

    private void loadSampleEvents() {
        eventList.clear();
        eventList.add(new EventItem("1", societyName + " Event", "TBD", "Draft"));
        adapter.notifyDataSetChanged();
    }

    // -------------------------------------------------------------------------
    // Load notifications
    // -------------------------------------------------------------------------

    private void loadNotifications() {
        if (layoutNotifications == null) return;
        layoutNotifications.removeAllViews();

        db.collection("notifications")
                .whereEqualTo("organizerUsername", organizerUsername)
                .orderBy("timestamp",
                        com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) return;
                    for (QueryDocumentSnapshot doc : query) {
                        String message   = doc.getString("message");
                        Long   timestamp = doc.getLong("timestamp");
                        if (message == null) continue;

                        View notifView = LayoutInflater.from(this)
                                .inflate(R.layout.item_notification,
                                        layoutNotifications, false);
                        TextView tvMsg  = notifView.findViewById(R.id.tvNotifMessage);
                        TextView tvTime = notifView.findViewById(R.id.tvNotifTime);
                        if (tvMsg  != null) tvMsg.setText(message);
                        if (tvTime != null && timestamp != null) {
                            tvTime.setText(getRelativeTime(timestamp));
                        }
                        layoutNotifications.addView(notifView);
                    }
                });
    }

    private String getRelativeTime(long timestamp) {
        long diff    = System.currentTimeMillis() - timestamp;
        long minutes = diff / 60000;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24)   return hours + "h ago";
        return (hours / 24) + "d ago";
    }

    // -------------------------------------------------------------------------
    // EventItem model
    // -------------------------------------------------------------------------

    static class EventItem {
        String id, title, date, status;
        EventItem(String id, String title, String date, String status) {
            this.id     = id;
            this.title  = title;
            this.date   = date;
            this.status = status;
        }
    }

    // -------------------------------------------------------------------------
    // EventAdapter
    // -------------------------------------------------------------------------

    class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

        private final List<EventItem> list;
        EventAdapter(List<EventItem> list) { this.list = list; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_event, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            EventItem event = list.get(position);

            holder.tvEventName.setText(event.title);
            holder.tvEventDate.setText(event.date);
            holder.tvEventStatus.setText(event.status);

            // Status badge colour + Edit button visibility
            switch (event.status) {
                case "Approved":
                    holder.tvEventStatus.setBackgroundColor(0xFF4CAF50);
                    holder.btnEventAction.setVisibility(View.GONE);
                    break;
                case "Revision Requested":
                    holder.tvEventStatus.setBackgroundColor(0xFFFF9800);
                    holder.btnEventAction.setVisibility(View.VISIBLE);
                    holder.btnEventAction.setText("Edit");
                    holder.btnEventAction.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF0D47A1));
                    break;
                case "Submitted":
                    holder.tvEventStatus.setBackgroundColor(0xFFFF9800);
                    holder.btnEventAction.setVisibility(View.GONE);
                    break;
                case "Rejected":
                    holder.tvEventStatus.setBackgroundColor(0xFFF44336);
                    holder.btnEventAction.setVisibility(View.GONE);
                    break;
                case "Draft":
                default:
                    holder.tvEventStatus.setBackgroundColor(0xFF9E9E9E);
                    holder.btnEventAction.setVisibility(View.VISIBLE);
                    holder.btnEventAction.setText("Edit");
                    holder.btnEventAction.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF0D47A1));
                    break;
            }

            // Edit button opens ProposalFormActivity with proposalId
            holder.btnEventAction.setOnClickListener(v -> {
                Intent intent = new Intent(OrganizerDashboardActivity.this,
                        ProposalFormActivity.class);
                intent.putExtra("proposalId", event.id);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEventName, tvEventDate, tvEventStatus;
            Button   btnEventAction;

            ViewHolder(View v) {
                super(v);
                tvEventName    = v.findViewById(R.id.tvEventName);
                tvEventDate    = v.findViewById(R.id.tvEventDate);
                tvEventStatus  = v.findViewById(R.id.tvEventStatus);
                btnEventAction = v.findViewById(R.id.btnEventAction);
            }
        }
    }
}