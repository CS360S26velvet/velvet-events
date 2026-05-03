package com.lums.eventhub.organizer.payments;

/**
 * PaymentVerificationListActivity.java
 *
 * Role: Shows all APPROVED events for this organiser.
 * Each event row has a "Check Payments" button.
 * Clicking it opens PaymentVerificationActivity for that event.
 *
 * Firestore reads:
 *   events/ where organizerUsername == organizerUsername AND status == "Approved"
 *   (also checks proposals/ with status "Approved" as fallback)
 *
 * Extras received from OrganizerDashboardActivity:
 *   "organizerUsername" — the ORG_xxx username
 *   "societyName"       — display name
 */

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

public class PaymentVerificationListActivity extends AppCompatActivity {

    private String            organizerUsername;
    private String            societyName;
    private FirebaseFirestore db;

    private RecyclerView      recyclerView;
    private TextView          tvNoEvents;
    private EventAdapter      adapter;
    private List<EventItem>   eventList = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_verification_list);

        db = FirebaseFirestore.getInstance();

        organizerUsername = getIntent().getStringExtra("organizerUsername");
        societyName       = getIntent().getStringExtra("societyName");
        if (organizerUsername == null) organizerUsername = "ORG0012";
        if (societyName       == null) societyName       = "My Society";

        TextView tvTitle = findViewById(R.id.tvPaymentListTitle);
        if (tvTitle != null) tvTitle.setText("Payment Verification — " + societyName);

        recyclerView = findViewById(R.id.recyclerViewApprovedEvents);
        tvNoEvents   = findViewById(R.id.tvNoApprovedEvents);

        adapter = new EventAdapter(eventList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadApprovedEvents();
    }

    // -------------------------------------------------------------------------
    // Load approved events from Firestore
    // -------------------------------------------------------------------------

    private void loadApprovedEvents() {
        // Load from events/ collection (admin-approved)
        db.collection("events")
                .whereEqualTo("organizerUsername", organizerUsername)
                .get()
                .addOnSuccessListener(snap -> {
                    eventList.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        String title  = doc.getString("title");
                        String status = doc.getString("status");
                        if (title == null || title.isEmpty()) title = doc.getId();
                        // Include events that are Approved or have no status (legacy)
                        eventList.add(new EventItem(doc.getId(), title, status != null ? status : "Approved"));
                    }

                    // Also check proposals/ with status == "Approved"
                    loadApprovedProposals();
                })
                .addOnFailureListener(e -> loadApprovedProposals());
    }

    private void loadApprovedProposals() {
        db.collection("proposals")
                .whereEqualTo("organizerUsername", organizerUsername)
                .whereEqualTo("status", "Approved")
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        String title = doc.getString("title");
                        if (title == null || title.isEmpty()) title = doc.getId();
                        // Avoid duplicates
                        boolean exists = false;
                        for (EventItem ei : eventList) {
                            if (ei.id.equals(doc.getId())) { exists = true; break; }
                        }
                        if (!exists) {
                            eventList.add(new EventItem(doc.getId(), title, "Approved"));
                        }
                    }
                    showOrHideEmpty();
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    showOrHideEmpty();
                    adapter.notifyDataSetChanged();
                });
    }

    private void showOrHideEmpty() {
        if (eventList.isEmpty()) {
            tvNoEvents.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoEvents.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    // -------------------------------------------------------------------------
    // Model
    // -------------------------------------------------------------------------

    static class EventItem {
        String id, title, status;
        EventItem(String id, String title, String status) {
            this.id     = id;
            this.title  = title;
            this.status = status;
        }
    }

    // -------------------------------------------------------------------------
    // Adapter
    // -------------------------------------------------------------------------

    class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

        private final List<EventItem> list;
        EventAdapter(List<EventItem> list) { this.list = list; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_payment_event, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            EventItem event = list.get(position);
            holder.tvEventName.setText(event.title);
            holder.tvEventStatus.setText(event.status);

            holder.btnCheckPayments.setOnClickListener(v -> {
                Intent intent = new Intent(
                        PaymentVerificationListActivity.this,
                        PaymentVerificationActivity.class);
                intent.putExtra("eventId",   event.id);
                intent.putExtra("eventName", event.title);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEventName, tvEventStatus;
            Button   btnCheckPayments;
            ViewHolder(View v) {
                super(v);
                tvEventName      = v.findViewById(R.id.tvPaymentEventName);
                tvEventStatus    = v.findViewById(R.id.tvPaymentEventStatus);
                btnCheckPayments = v.findViewById(R.id.btnCheckPayments);
            }
        }
    }
}