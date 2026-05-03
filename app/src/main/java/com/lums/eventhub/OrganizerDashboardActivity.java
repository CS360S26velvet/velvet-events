package com.lums.eventhub;

/**
 * OrganizerDashboardActivity.java
 *
 * CHANGES:
 *   1. Removed "Registrants" nav button and RegistrantDashboardActivity navigation
 *   2. Removed "Form Settings" nav button and CapacitySettingActivity navigation
 *   3. Removed "View All Notifications" button and loadNotifications() entirely
 *   4. FIXED "Edit Prior Event" logic:
 *        - Shows ONLY when the event report has been APPROVED by admin
 *          (reads eventReports/{eventId}.status == "Approved")
 *        - When "Edit Prior Event" is shown, the "Approved" status badge is HIDDEN
 *        - Regular "Approved" events (no approved report yet) show green "Approved"
 *          badge with NO action button — organizer must submit a report first
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
import com.lums.eventhub.auth.LoginActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OrganizerDashboardActivity extends AppCompatActivity {

    private String organizerUsername;
    private String societyName;

    private FirebaseFirestore db;
    private RecyclerView recyclerViewEvents;
    private EventAdapter adapter;
    private final List<EventItem> eventList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organizer_dashboard);

        db = FirebaseFirestore.getInstance();

        organizerUsername = getIntent().getStringExtra("organizerUsername");
        societyName       = getIntent().getStringExtra("societyName");
        if (organizerUsername == null) organizerUsername = "ORG0012";
        if (societyName == null)       societyName       = "My Society";

        TextView tvSociety = findViewById(R.id.tvSocietyName);
        if (tvSociety != null) tvSociety.setText(societyName);

        recyclerViewEvents = findViewById(R.id.recyclerViewEvents);
        adapter = new EventAdapter(eventList);
        recyclerViewEvents.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewEvents.setAdapter(adapter);

        // Register New Event
        Button btnRegisterNewEvent = findViewById(R.id.btnRegisterNewEvent);
        if (btnRegisterNewEvent != null) {
            btnRegisterNewEvent.setOnClickListener(v -> {
                Intent i = new Intent(this, ProposalFormActivity.class);
                i.putExtra("organizerUsername", organizerUsername);
                i.putExtra("societyName", societyName);
                startActivity(i);
            });
        }

        // Management Console navigation — Registrants and Form Settings REMOVED
        findViewById(R.id.btnNavAttendeeReg).setOnClickListener(v -> {
            Intent i = new Intent(this, AttendeeRegistrationActivity.class);
            i.putExtra("organizerUsername", organizerUsername);
            startActivity(i);
        });

        findViewById(R.id.btnNavCheckIn).setOnClickListener(v -> {
            Intent ciIntent = new Intent(this, CheckInActivity.class);
            ciIntent.putExtra("organizerUsername", organizerUsername);
            startActivity(ciIntent);
        });

        findViewById(R.id.btnNavPayments).setOnClickListener(v -> {
            Intent pIntent = new Intent(this, PaymentVerificationListActivity.class);
            pIntent.putExtra("organizerUsername", organizerUsername);
            pIntent.putExtra("societyName", societyName);
            startActivity(pIntent);
        });

        findViewById(R.id.btnNavVendors).setOnClickListener(v -> {
            Intent vIntent = new Intent(this, VendorDirectoryActivity.class);
            vIntent.putExtra("organizerUsername", organizerUsername);
            vIntent.putExtra("societyName", societyName);
            startActivity(vIntent);
        });

        findViewById(R.id.btnNavReports).setOnClickListener(v -> {
            Intent rIntent = new Intent(this, EventReportsActivity.class);
            rIntent.putExtra("organizerUsername", organizerUsername);
            rIntent.putExtra("societyName", societyName);
            startActivity(rIntent);
        });

        findViewById(R.id.btnNavRegistrantData).setOnClickListener(v -> {
            Intent rdIntent = new Intent(this, RegistrantDataActivity.class);
            rdIntent.putExtra("organizerUsername", organizerUsername);
            rdIntent.putExtra("societyName", societyName);
            startActivity(rdIntent);
        });

        findViewById(R.id.btnNavEventVisibility).setOnClickListener(v -> {
            Intent evIntent = new Intent(this, EventVisibilityActivity.class);
            evIntent.putExtra("organizerUsername", organizerUsername);
            startActivity(evIntent);
        });

        // Logout
        Button btnLogout = findViewById(R.id.btnLogoutOrganizer);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                LoginActivity.clearSession(this);
                Intent intent = new Intent(this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
        loadEvents();
    }

    // ── Load stats ────────────────────────────────────────────────────────────

    private void loadStats() {
        db.collection("proposals")
                .whereEqualTo("organizerUsername", organizerUsername)
                .get()
                .addOnSuccessListener(query -> {
                    db.collection("events")
                            .whereEqualTo("organizerUsername", organizerUsername)
                            .get()
                            .addOnSuccessListener(evSnap -> {
                                int total = query.size() + evSnap.size();
                                TextView tvTotal = findViewById(R.id.tvTotalEvents);
                                if (tvTotal != null) tvTotal.setText(String.valueOf(total));
                            });
                });
    }

    // ── Load events ───────────────────────────────────────────────────────────

    /**
     * Step 1: load proposals (Draft, Submitted, Revision Requested, Rejected).
     * Approved events come from events/ collection.
     */
    private void loadEvents() {
        db.collection("proposals")
                .whereEqualTo("organizerUsername", organizerUsername)
                .get()
                .addOnSuccessListener(query -> {
                    eventList.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        String status = doc.getString("status");
                        // Approved come from events/ — skip here
                        if ("Approved".equals(status) || "Completed".equals(status)) continue;

                        String title = doc.getString("title");
                        String date  = doc.getString("date");
                        if (date == null) date = doc.getString("eventDate");
                        if (title  == null) title  = "Untitled";
                        if (date   == null) date   = "—";
                        if (status == null) status = "Draft";
                        String adminReason = doc.getString("adminReason");
                        if (adminReason == null) adminReason = "";

                        eventList.add(new EventItem(
                                doc.getId(), title, date, status, true, adminReason, ""));
                    }
                    // Step 2: load approved events, then check which have approved reports
                    loadApprovedEvents();
                })
                .addOnFailureListener(e -> loadApprovedEvents());
    }

    /**
     * Step 2: load approved events from events/.
     * Step 3: query eventReports to find which ones have been admin-approved.
     *         Only those get "Edit Prior Event" — the rest show "Approved" badge only.
     *
     * DEDUP FIX: If an event ID already appears in the proposals list (meaning the
     * organizer has resubmitted it via "Edit Prior Event" and it is now Submitted /
     * Revision Requested / Rejected), skip it from the events/ list entirely so it
     * doesn't show twice.
     */
    private void loadApprovedEvents() {
        // Collect proposal IDs already loaded (non-Approved statuses in flight)
        Set<String> inFlightIds = new HashSet<>();
        for (EventItem existing : eventList) {
            if (existing.isProposal) inFlightIds.add(existing.id);
        }

        db.collection("events")
                .whereEqualTo("organizerUsername", organizerUsername)
                .get()
                .addOnSuccessListener(evSnap -> {
                    final List<EventItem> approvedItems = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : evSnap) {
                        // DEDUP: skip if this event is already showing as a
                        // resubmitted proposal (Submitted / Rejected / Revision)
                        if (inFlightIds.contains(doc.getId())) continue;

                        String title  = doc.getString("title");
                        String date   = doc.getString("date");
                        String status = doc.getString("status");
                        if (title  == null) title  = "Untitled";
                        if (date   == null) date   = "—";
                        if (status == null) status = "Approved";
                        approvedItems.add(new EventItem(
                                doc.getId(), title, date, status, false, "", ""));
                    }

                    if (approvedItems.isEmpty()) {
                        if (eventList.isEmpty()) loadSampleEvents();
                        else adapter.notifyDataSetChanged();
                        return;
                    }

                    // Step 3: find which event reports were approved by admin
                    db.collection("eventReports")
                            .whereEqualTo("organizerUsername", organizerUsername)
                            .whereEqualTo("status", "Approved")
                            .get()
                            .addOnSuccessListener(reportSnap -> {
                                Set<String> approvedReportEventIds = new HashSet<>();
                                for (QueryDocumentSnapshot rdoc : reportSnap) {
                                    String eid = rdoc.getString("eventId");
                                    if (eid != null) approvedReportEventIds.add(eid);
                                    approvedReportEventIds.add(rdoc.getId());
                                }

                                for (EventItem item : approvedItems) {
                                    if (approvedReportEventIds.contains(item.id)) {
                                        item.reportStatus = "Approved";
                                    }
                                }

                                eventList.addAll(approvedItems);
                                if (eventList.isEmpty()) loadSampleEvents();
                                else adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                eventList.addAll(approvedItems);
                                if (eventList.isEmpty()) loadSampleEvents();
                                else adapter.notifyDataSetChanged();
                            });
                })
                .addOnFailureListener(e -> {
                    if (eventList.isEmpty()) loadSampleEvents();
                    else adapter.notifyDataSetChanged();
                });
    }

    private void loadSampleEvents() {
        eventList.clear();
        eventList.add(new EventItem("s1", societyName + " Event", "TBD", "Draft", true, "", ""));
        adapter.notifyDataSetChanged();
    }

    // ── EventItem model ───────────────────────────────────────────────────────

    static class EventItem {
        String  id, title, date, status, adminReason, reportStatus;
        boolean isProposal;

        EventItem(String id, String title, String date, String status,
                  boolean isProposal, String adminReason, String reportStatus) {
            this.id           = id;
            this.title        = title;
            this.date         = date;
            this.status       = status;
            this.isProposal   = isProposal;
            this.adminReason  = adminReason  != null ? adminReason  : "";
            this.reportStatus = reportStatus != null ? reportStatus : "";
        }
    }

    // ── EventAdapter ──────────────────────────────────────────────────────────

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

            // Reset
            holder.btnEventAction.setVisibility(View.GONE);
            holder.btnEventAction.setOnClickListener(null);
            if (holder.tvAdminReason != null) {
                holder.tvAdminReason.setVisibility(View.GONE);
                holder.tvAdminReason.setText("");
            }

            switch (event.status) {

                case "Approved":
                    if ("Approved".equals(event.reportStatus)) {
                        // Event report was approved by admin → show "Edit Prior Event"
                        // Hide the "Approved" status badge — replace with action button only
                        holder.tvEventStatus.setVisibility(View.GONE);
                        holder.btnEventAction.setVisibility(View.VISIBLE);
                        holder.btnEventAction.setText("Edit Prior Event");
                        holder.btnEventAction.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(0xFF1565C0));
                        holder.btnEventAction.setOnClickListener(v -> openProposalForm(event.id));
                    } else {
                        // Approved but report not yet approved → show green badge, no button
                        holder.tvEventStatus.setVisibility(View.VISIBLE);
                        holder.tvEventStatus.setText(event.status);
                        holder.tvEventStatus.setBackgroundColor(0xFF4CAF50);
                    }
                    break;

                case "Revision Requested":
                    holder.tvEventStatus.setVisibility(View.VISIBLE);
                    holder.tvEventStatus.setText(event.status);
                    holder.tvEventStatus.setBackgroundColor(0xFFFF9800);
                    holder.btnEventAction.setVisibility(View.VISIBLE);
                    holder.btnEventAction.setText("Edit");
                    holder.btnEventAction.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF0D47A1));
                    holder.btnEventAction.setOnClickListener(v -> openProposalForm(event.id));
                    if (!event.adminReason.isEmpty() && holder.tvAdminReason != null) {
                        holder.tvAdminReason.setVisibility(View.VISIBLE);
                        holder.tvAdminReason.setText("Reason: " + event.adminReason);
                    }
                    break;

                case "Submitted":
                    holder.tvEventStatus.setVisibility(View.VISIBLE);
                    holder.tvEventStatus.setText(event.status);
                    holder.tvEventStatus.setBackgroundColor(0xFFFF9800);
                    break;

                case "Rejected":
                    holder.tvEventStatus.setVisibility(View.VISIBLE);
                    holder.tvEventStatus.setText(event.status);
                    holder.tvEventStatus.setBackgroundColor(0xFFF44336);
                    holder.btnEventAction.setVisibility(View.VISIBLE);
                    holder.btnEventAction.setText("Edit & Resubmit");
                    holder.btnEventAction.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFFC62828));
                    holder.btnEventAction.setOnClickListener(v -> openProposalForm(event.id));
                    if (!event.adminReason.isEmpty() && holder.tvAdminReason != null) {
                        holder.tvAdminReason.setVisibility(View.VISIBLE);
                        holder.tvAdminReason.setText("Reason: " + event.adminReason);
                    }
                    break;

                case "Completed":
                    holder.tvEventStatus.setVisibility(View.VISIBLE);
                    holder.tvEventStatus.setText(event.status);
                    holder.tvEventStatus.setBackgroundColor(0xFF607D8B);
                    break;

                case "Draft":
                default:
                    holder.tvEventStatus.setVisibility(View.VISIBLE);
                    holder.tvEventStatus.setText(event.status);
                    holder.tvEventStatus.setBackgroundColor(0xFF9E9E9E);
                    holder.btnEventAction.setVisibility(View.VISIBLE);
                    holder.btnEventAction.setText("Edit");
                    holder.btnEventAction.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF0D47A1));
                    holder.btnEventAction.setOnClickListener(v -> openProposalForm(event.id));
                    break;
            }
        }

        private void openProposalForm(String proposalId) {
            Intent intent = new Intent(OrganizerDashboardActivity.this,
                    ProposalFormActivity.class);
            intent.putExtra("proposalId", proposalId);
            intent.putExtra("organizerUsername", organizerUsername);
            intent.putExtra("societyName", societyName);
            startActivity(intent);
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEventName, tvEventDate, tvEventStatus, tvAdminReason;
            Button   btnEventAction;

            ViewHolder(View v) {
                super(v);
                tvEventName    = v.findViewById(R.id.tvEventName);
                tvEventDate    = v.findViewById(R.id.tvEventDate);
                tvEventStatus  = v.findViewById(R.id.tvEventStatus);
                tvAdminReason  = v.findViewById(R.id.tvAdminReason);
                btnEventAction = v.findViewById(R.id.btnEventAction);
            }
        }
    }
}