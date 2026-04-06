package com.lums.eventhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OrganizerDashboardActivity.java
 *
 * <p>Main organizer dashboard. Reads from TWO Firestore collections and merges them:
 * <ul>
 *   <li>{@code proposals/} — Draft, Submitted, Rejected items (organizer-owned)</li>
 *   <li>{@code events/}    — Approved, Completed items (written by CCA admin on approval)</li>
 * </ul>
 * This ensures the organizer always sees the full lifecycle of their events.</p>
 *
 * <p>User Stories: Org US-01, US-11</p>
 */
public class OrganizerDashboardActivity extends AppCompatActivity {

    private static final String ORGANIZER_ID = "ORG0012";
    private static final String SOCIETY_NAME = "SPADES Society";

    private FirebaseFirestore db;
    private TextView          tvTotalEvents;
    private TextView          tvPendingPayments;
    private RecyclerView      recyclerViewEvents;
    private LinearLayout      layoutNotifications;
    private EventAdapter      eventAdapter;

    private final List<EventRow> eventList = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_dashboard);
        db = FirebaseFirestore.getInstance();
        bindViews();
        setupRecyclerView();
        wireButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh every time we return so newly saved drafts appear immediately
        loadStatCards();
        loadAllEvents();
        loadNotifications();
    }

    // -------------------------------------------------------------------------
    // UI setup
    // -------------------------------------------------------------------------

    /** Binds all view references. */
    private void bindViews() {
        tvTotalEvents       = findViewById(R.id.tvTotalEvents);
        tvPendingPayments   = findViewById(R.id.tvPendingPayments);
        recyclerViewEvents  = findViewById(R.id.recyclerViewEvents);
        layoutNotifications = findViewById(R.id.layoutNotifications);
    }

    /** Wires the RecyclerView with adapter. */
    private void setupRecyclerView() {
        eventAdapter = new EventAdapter(eventList);
        recyclerViewEvents.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewEvents.setAdapter(eventAdapter);
    }

    /** Wires action buttons. */
    private void wireButtons() {
        Button btnNewEvent = findViewById(R.id.btnRegisterNewEvent);
        btnNewEvent.setOnClickListener(v ->
                startActivity(new Intent(this, ProposalFormActivity.class)));

        Button btnViewAllNotifs = findViewById(R.id.btnViewAllNotifications);
        btnViewAllNotifs.setOnClickListener(v ->
                Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show());
    }

    // -------------------------------------------------------------------------
    // Stat cards
    // -------------------------------------------------------------------------

    /**
     * Card 1: total count across proposals/ + events/ for this organizer.
     * Card 2: count of attendees with paymentStatus == "Pending".
     */
    private void loadStatCards() {
        db.collection("proposals")
                .whereEqualTo("organizerId", ORGANIZER_ID)
                .get()
                .addOnSuccessListener(ps -> {
                    int pCount = ps.size();
                    db.collection("events")
                            .whereEqualTo("organizerId", ORGANIZER_ID)
                            .get()
                            .addOnSuccessListener(es ->
                                    tvTotalEvents.setText(String.valueOf(pCount + es.size())))
                            .addOnFailureListener(e ->
                                    tvTotalEvents.setText(String.valueOf(pCount)));
                })
                .addOnFailureListener(e -> tvTotalEvents.setText("0"));

        db.collection("attendees")
                .whereEqualTo("paymentStatus", "Pending")
                .get()
                .addOnSuccessListener(snap ->
                        tvPendingPayments.setText(String.valueOf(snap.size())))
                .addOnFailureListener(e -> tvPendingPayments.setText("0"));
    }

    // -------------------------------------------------------------------------
    // Events list — merge proposals/ and events/
    // -------------------------------------------------------------------------

    /**
     * Reads proposals/ (Draft/Submitted/Rejected) and events/ (Approved/Completed)
     * and merges them into eventList. Falls back to sample data only if both return empty.
     */
    private void loadAllEvents() {
        eventList.clear();

        db.collection("proposals")
                .whereEqualTo("organizerId", ORGANIZER_ID)
                .get()
                .addOnSuccessListener(proposalSnap -> {
                    for (QueryDocumentSnapshot doc : proposalSnap) {
                        String status = doc.getString("status");
                        // Approved/Completed proposals will appear via events/ collection
                        if ("Approved".equals(status) || "Completed".equals(status)) continue;
                        eventList.add(new EventRow(
                                doc.getId(),
                                nvl(doc.getString("title"), "Untitled"),
                                nvl(doc.getString("date"), ""),
                                nvl(status, "Draft"),
                                true));
                    }
                    loadApprovedEvents();
                })
                .addOnFailureListener(e -> loadApprovedEvents());
    }

    /** Second half of merge — loads from events/ then finalises the list. */
    private void loadApprovedEvents() {
        db.collection("events")
                .whereEqualTo("organizerId", ORGANIZER_ID)
                .get()
                .addOnSuccessListener(eventSnap -> {
                    for (QueryDocumentSnapshot doc : eventSnap) {
                        eventList.add(new EventRow(
                                doc.getId(),
                                nvl(doc.getString("title"), "Untitled"),
                                nvl(doc.getString("date"), ""),
                                nvl(doc.getString("status"), "Approved"),
                                false));
                    }
                    if (eventList.isEmpty()) addSampleEvents();
                    else eventAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (eventList.isEmpty()) addSampleEvents();
                    else eventAdapter.notifyDataSetChanged();
                });
    }

    /** Fallback sample data shown when both collections are empty. */
    private void addSampleEvents() {
        eventList.add(new EventRow("s1", "SPADES 2025",      "15 Apr 2026", "Approved",  false));
        eventList.add(new EventRow("s2", "Tech Workshop",    "22 Apr 2026", "Submitted", true));
        eventList.add(new EventRow("s3", "Annual Gala",      "30 Apr 2026", "Draft",     true));
        eventList.add(new EventRow("s4", "Orientation 2025", "10 Jan 2026", "Completed", false));
        eventAdapter.notifyDataSetChanged();
    }

    // -------------------------------------------------------------------------
    // Notifications
    // -------------------------------------------------------------------------

    /** Loads up to 5 notifications; falls back to 3 hardcoded rows if empty. */
    private void loadNotifications() {
        db.collection("notifications")
                .whereEqualTo("organizerId", ORGANIZER_ID)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) { addSampleNotifications(); return; }
                    layoutNotifications.removeAllViews();
                    for (QueryDocumentSnapshot doc : snap) {
                        String msg = doc.getString("message");
                        long ts    = doc.getLong("timestamp") != null
                                ? doc.getLong("timestamp") : 0L;
                        addNotificationRow(nvl(msg, ""), formatRelativeTime(ts));
                    }
                })
                .addOnFailureListener(e -> addSampleNotifications());
    }

    private void addSampleNotifications() {
        layoutNotifications.removeAllViews();
        addNotificationRow("Your proposal SPADES 2025 was approved by CCA", "2h ago");
        addNotificationRow("Payment received from Hassan Raza",              "5h ago");
        addNotificationRow("Proposal submitted successfully",                "1d ago");
    }

    private void addNotificationRow(String message, String time) {
        View row = getLayoutInflater()
                .inflate(R.layout.item_notification, layoutNotifications, false);
        ((TextView) row.findViewById(R.id.tvNotifMessage)).setText(message);
        ((TextView) row.findViewById(R.id.tvNotifTime)).setText(time);
        layoutNotifications.addView(row);
    }

    // -------------------------------------------------------------------------
    // Duplicate
    // -------------------------------------------------------------------------

    /**
     * Copies an approved event into proposals/ as a new Draft, then opens
     * ProposalFormActivity so the organizer can edit and resubmit it.
     */
    private void duplicateEvent(EventRow source) {
        Map<String, Object> copy = new HashMap<>();
        copy.put("title",       source.title + " (Copy)");
        copy.put("date",        source.date);
        copy.put("status",      "Draft");
        copy.put("organizerId", ORGANIZER_ID);
        copy.put("societyName", SOCIETY_NAME);
        copy.put("createdAt",   System.currentTimeMillis());

        db.collection("proposals").add(copy)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Duplicated as draft!", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(this, ProposalFormActivity.class);
                    i.putExtra("proposalId", ref.getId());
                    startActivity(i);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Duplicate failed.", Toast.LENGTH_SHORT).show());
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private String nvl(String s, String fallback) {
        return s != null ? s : fallback;
    }

    private String formatRelativeTime(long ms) {
        long diff = System.currentTimeMillis() - ms;
        long min  = diff / 60_000;
        if (min < 1)  return "just now";
        if (min < 60) return min + "m ago";
        long hr = min / 60;
        if (hr < 24)  return hr + "h ago";
        return (hr / 24) + "d ago";
    }

    // =========================================================================
    // Model
    // =========================================================================

    /**
     * One row in the My Events list. May originate from proposals/ or events/.
     */
    static class EventRow {
        final String  docId;
        final String  title;
        final String  date;
        final String  status;
        final boolean isProposal; // true = proposals/, false = events/

        EventRow(String docId, String title, String date, String status, boolean isProposal) {
            this.docId      = docId;
            this.title      = title;
            this.date       = date;
            this.status     = status;
            this.isProposal = isProposal;
        }
    }

    // =========================================================================
    // Adapter + ViewHolder
    // =========================================================================

    class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

        private final List<EventRow> items;
        EventAdapter(List<EventRow> items) { this.items = items; }

        @NonNull @Override
        public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_event, parent, false);
            return new EventViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull EventViewHolder h, int pos) {
            h.bind(items.get(pos));
        }

        @Override public int getItemCount() { return items.size(); }

        class EventViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvEventName, tvEventDate, tvStatus;
            private final Button   btnAction;

            EventViewHolder(@NonNull View v) {
                super(v);
                tvEventName = v.findViewById(R.id.tvEventName);
                tvEventDate = v.findViewById(R.id.tvEventDate);
                tvStatus    = v.findViewById(R.id.tvEventStatus);
                btnAction   = v.findViewById(R.id.btnEventAction);
            }

            void bind(EventRow row) {
                tvEventName.setText(row.title);
                tvEventDate.setText(row.date);
                tvStatus.setText(row.status);

                int color;
                switch (row.status) {
                    case "Approved":   color = 0xFF4CAF50; break;
                    case "Submitted":  color = 0xFFFF9800; break;
                    case "Rejected":   color = 0xFFF44336; break;
                    default:           color = 0xFF9E9E9E; break;
                }
                tvStatus.setBackgroundColor(color);

                switch (row.status) {
                    case "Draft":
                        btnAction.setVisibility(View.VISIBLE);
                        btnAction.setText("Edit");
                        btnAction.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(0xFF0D47A1));
                        btnAction.setOnClickListener(v -> {
                            Intent i = new Intent(OrganizerDashboardActivity.this,
                                    ProposalFormActivity.class);
                            i.putExtra("proposalId", row.docId);
                            startActivity(i);
                        });
                        break;

                    case "Approved":
                        btnAction.setVisibility(View.VISIBLE);
                        btnAction.setText("Duplicate");
                        btnAction.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(0xFF1565C0));
                        btnAction.setOnClickListener(v -> duplicateEvent(row));
                        break;

                    case "Rejected":
                        btnAction.setVisibility(View.VISIBLE);
                        btnAction.setText("Edit & Resubmit");
                        btnAction.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(0xFFF44336));
                        btnAction.setOnClickListener(v -> {
                            Intent i = new Intent(OrganizerDashboardActivity.this,
                                    ProposalFormActivity.class);
                            i.putExtra("proposalId", row.docId);
                            startActivity(i);
                        });
                        break;

                    default:
                        btnAction.setVisibility(View.GONE);
                        break;
                }
            }
        }
    }
}