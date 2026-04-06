package com.lums.eventhub;

/**
 * OrganizerDashboardActivity.java
 *
 * Full organizer dashboard. Merges proposals/ (Draft, Submitted, Revision Requested, Rejected)
 * and events/ (Approved, Completed) so the organizer sees the full event lifecycle.
 *
 * Receives from LoginActivity:
 *   "organizerUsername" — the #ORG_xxx username (canonical field name)
 *   "societyName"       — e.g. "SPADES Society"
 *
 * Status badge colours + action buttons:
 *   "Approved"           → green,  no action button
 *   "Revision Requested" → orange, Edit button
 *   "Submitted"          → yellow/orange, no action button
 *   "Rejected"           → red,    "Edit & Resubmit" button
 *   "Draft"              → grey,   Edit button
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
    private final List<EventItem> eventList = new ArrayList<>();
    private LinearLayout layoutNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_dashboard);

        db = FirebaseFirestore.getInstance();

        // Get from login intent — no hardcoding
        organizerUsername = getIntent().getStringExtra("organizerUsername");
        societyName       = getIntent().getStringExtra("societyName");

        // Fallbacks for dev/direct launch
        if (organizerUsername == null) organizerUsername = "ORG0012";
        if (societyName == null)       societyName       = "My Society";

        // Update header with actual society name
        TextView tvSociety = findViewById(R.id.tvSocietyName);
        if (tvSociety != null) tvSociety.setText(societyName);

        // RecyclerView
        recyclerViewEvents = findViewById(R.id.recyclerViewEvents);
        adapter = new EventAdapter(eventList);
        recyclerViewEvents.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewEvents.setAdapter(adapter);

        layoutNotifications = findViewById(R.id.layoutNotifications);

        // Register New Event → ProposalFormActivity
        Button btnRegisterNewEvent = findViewById(R.id.btnRegisterNewEvent);
        if (btnRegisterNewEvent != null) {
            btnRegisterNewEvent.setOnClickListener(v -> {
                Intent i = new Intent(this, ProposalFormActivity.class);
                i.putExtra("organizerUsername", organizerUsername);
                i.putExtra("societyName", societyName);
                startActivity(i);
            });
        }

        // Management Console navigation
        findViewById(R.id.btnNavAttendeeReg).setOnClickListener(v -> {
            Intent i = new Intent(this, AttendeeRegistrationActivity.class);
            i.putExtra("organizerUsername", organizerUsername);
            startActivity(i);
        });

        findViewById(R.id.btnNavRegistrants).setOnClickListener(v ->
                startActivity(new Intent(this, RegistrantDashboardActivity.class)));

        findViewById(R.id.btnNavCheckIn).setOnClickListener(v ->
                startActivity(new Intent(this, CheckInActivity.class)));

        findViewById(R.id.btnNavFormSettings).setOnClickListener(v ->
                startActivity(new Intent(this, CapacitySettingActivity.class)));

        findViewById(R.id.btnNavPayments).setOnClickListener(v ->
                Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show());
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
        // Count proposals that are not drafts + all approved events
        db.collection("proposals")
                .whereEqualTo("organizerUsername", organizerUsername)
                .get()
                .addOnSuccessListener(q -> {
                    int total = q.size();
                    // Also count from events/ collection (approved/completed)
                    db.collection("events")
                            .whereEqualTo("organizerUsername", organizerUsername)
                            .get()
                            .addOnSuccessListener(evSnap -> {
                                TextView tv = findViewById(R.id.tvTotalEvents);
                                if (tv != null) tv.setText(String.valueOf(total + evSnap.size()));
                            })
                            .addOnFailureListener(e -> {
                                TextView tv = findViewById(R.id.tvTotalEvents);
                                if (tv != null) tv.setText(String.valueOf(total));
                            });
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
    // Load events — merge proposals/ and events/ collections
    // -------------------------------------------------------------------------

    /**
     * Step 1: load from proposals/ (Draft, Submitted, Revision Requested, Rejected).
     * Approved/Completed come from events/ collection (written by admin on approval).
     */
    private void loadEvents() {
        db.collection("proposals")
                .whereEqualTo("organizerUsername", organizerUsername)
                .get()
                .addOnSuccessListener(query -> {
                    eventList.clear();

                    for (QueryDocumentSnapshot doc : query) {
                        String status = doc.getString("status");
                        // Approved/Completed will come from events/ — skip them here
                        if ("Approved".equals(status) || "Completed".equals(status)) continue;

                        String title = doc.getString("title");
                        String date  = doc.getString("date");
                        if (date == null) date = doc.getString("eventDate");
                        if (title  == null) title  = "Untitled";
                        if (date   == null) date   = "—";
                        if (status == null) status = "Draft";

                        // isProposal=true means Edit → ProposalFormActivity with proposalId
                        eventList.add(new EventItem(doc.getId(), title, date, status, true));
                    }

                    // Step 2: load approved/completed from events/
                    loadApprovedEvents();
                })
                .addOnFailureListener(e -> loadApprovedEvents());
    }

    /** Step 2: load from events/ (Approved/Completed written by admin). */
    private void loadApprovedEvents() {
        db.collection("events")
                .whereEqualTo("organizerUsername", organizerUsername)
                .get()
                .addOnSuccessListener(evSnap -> {
                    for (QueryDocumentSnapshot doc : evSnap) {
                        String title  = doc.getString("title");
                        String date   = doc.getString("date");
                        String status = doc.getString("status");
                        if (title  == null) title  = "Untitled";
                        if (date   == null) date   = "—";
                        if (status == null) status = "Approved";
                        // isProposal=false — no edit allowed for approved events
                        eventList.add(new EventItem(doc.getId(), title, date, status, false));
                    }

                    if (eventList.isEmpty()) loadSampleEvents();
                    else adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (eventList.isEmpty()) loadSampleEvents();
                    else adapter.notifyDataSetChanged();
                });
    }

    private void loadSampleEvents() {
        eventList.clear();
        eventList.add(new EventItem("s1", societyName + " Event", "TBD", "Draft", true));
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
        String  id, title, date, status;
        boolean isProposal; // true = from proposals/, false = from events/

        EventItem(String id, String title, String date, String status, boolean isProposal) {
            this.id         = id;
            this.title      = title;
            this.date       = date;
            this.status     = status;
            this.isProposal = isProposal;
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

            // Reset button state
            holder.btnEventAction.setVisibility(View.GONE);
            holder.btnEventAction.setOnClickListener(null);

            switch (event.status) {

                case "Approved":
                    holder.tvEventStatus.setBackgroundColor(0xFF4CAF50);
                    // No action — event is approved and live
                    break;

                case "Revision Requested":
                    holder.tvEventStatus.setBackgroundColor(0xFFFF9800);
                    holder.btnEventAction.setVisibility(View.VISIBLE);
                    holder.btnEventAction.setText("Edit");
                    holder.btnEventAction.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF0D47A1));
                    holder.btnEventAction.setOnClickListener(v -> openProposalForm(event.id));
                    break;

                case "Submitted":
                    holder.tvEventStatus.setBackgroundColor(0xFFFF9800);
                    // Awaiting admin decision — no edit allowed
                    break;

                case "Rejected":
                    holder.tvEventStatus.setBackgroundColor(0xFFF44336);
                    holder.btnEventAction.setVisibility(View.VISIBLE);
                    holder.btnEventAction.setText("Edit & Resubmit");
                    holder.btnEventAction.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFFC62828));
                    holder.btnEventAction.setOnClickListener(v -> openProposalForm(event.id));
                    break;

                case "Completed":
                    holder.tvEventStatus.setBackgroundColor(0xFF607D8B);
                    break;

                case "Draft":
                default:
                    holder.tvEventStatus.setBackgroundColor(0xFF9E9E9E);
                    holder.btnEventAction.setVisibility(View.VISIBLE);
                    holder.btnEventAction.setText("Edit");
                    holder.btnEventAction.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF0D47A1));
                    holder.btnEventAction.setOnClickListener(v -> openProposalForm(event.id));
                    break;
            }
        }

        /** Opens ProposalFormActivity in edit mode for the given proposalId. */
        private void openProposalForm(String proposalId) {
            Intent intent = new Intent(OrganizerDashboardActivity.this,
                    ProposalFormActivity.class);
            intent.putExtra("proposalId", proposalId);
            intent.putExtra("organizerUsername", organizerUsername);
            intent.putExtra("societyName", societyName);
            startActivity(intent);
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
