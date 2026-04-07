package com.lums.eventhub.admin.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.lums.eventhub.R;
import com.lums.eventhub.admin.organizer.RegisterOrganizerActivity;
import com.lums.eventhub.admin.proposals.ProposalDetailActivity;
import com.lums.eventhub.admin.proposals.ProposalListActivity;
import com.lums.eventhub.admin.auditorium.AuditoriumActivity;
import com.lums.eventhub.admin.calendar.CalendarActivity;
import com.lums.eventhub.admin.accommodation.AccommodationActivity;

/**
 * AdminDashboardActivity.java
 *
 * Role: Main landing screen for admin users after login.
 * Displays summary stat cards (pending proposals, approved proposals,
 * accommodation requests, active events) and a live list of the 5 most
 * recent proposals awaiting CCA review. Provides navigation to all
 * admin sub-screens.
 *
 * Implements: Admin US-08
 */

public class AdminDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db = FirebaseFirestore.getInstance();

        // Show username in header
        String username = getIntent().getStringExtra("username");
        if (username != null) {
            ((TextView) findViewById(R.id.tvUsername)).setText(username);
        }

        // Navigation buttons
        findViewById(R.id.btnViewProposals).setOnClickListener(v ->
                startActivity(new Intent(this, ProposalListActivity.class)));

        findViewById(R.id.btnAuditorium).setOnClickListener(v ->
                startActivity(new Intent(this, AuditoriumActivity.class)));

        findViewById(R.id.btnCalendar).setOnClickListener(v ->
                startActivity(new Intent(this, CalendarActivity.class)));

        findViewById(R.id.btnAccommodation).setOnClickListener(v ->
                startActivity(new Intent(this, AccommodationActivity.class)));

        // Register New Organizer button
        findViewById(R.id.btnRegisterOrganizer).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterOrganizerActivity.class)));

        // "View All" proposals link
        findViewById(R.id.tvViewAll).setOnClickListener(v ->
                startActivity(new Intent(this, ProposalListActivity.class)));

        // Sign out
        findViewById(R.id.btnSignOut).setOnClickListener(v -> finish());

        loadStats();
        loadPendingProposals();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
        loadPendingProposals();
    }

    private void loadStats() {
        // Pending = status "Submitted" (organizer submitted to CCA, not yet reviewed)
        db.collection("proposals").whereEqualTo("status", "Submitted").get()
                .addOnSuccessListener(q ->
                        ((TextView) findViewById(R.id.tvPendingNumber)).setText(String.valueOf(q.size())));

        // Approved
        db.collection("proposals").whereEqualTo("status", "Approved").get()
                .addOnSuccessListener(q ->
                        ((TextView) findViewById(R.id.tvApprovedNumber)).setText(String.valueOf(q.size())));

        // Accommodation requests
        db.collection("accommodation_requests").get()
                .addOnSuccessListener(q ->
                        ((TextView) findViewById(R.id.tvAccomNumber)).setText(String.valueOf(q.size())));

        // Active events = Approved proposals
        db.collection("proposals").whereEqualTo("status", "Approved").get()
                .addOnSuccessListener(q ->
                        ((TextView) findViewById(R.id.tvActiveNumber)).setText(String.valueOf(q.size())));
    }

    private void loadPendingProposals() {
        LinearLayout container = findViewById(R.id.llPendingProposals);
        TextView tvNoPending   = findViewById(R.id.tvNoPending);
        container.removeAllViews();

        // Show "Submitted" proposals (pending CCA review) — not Draft
        db.collection("proposals").whereEqualTo("status", "Submitted")
                .limit(5)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        tvNoPending.setVisibility(View.VISIBLE);
                        return;
                    }
                    tvNoPending.setVisibility(View.GONE);

                    for (QueryDocumentSnapshot doc : query) {
                        String docId   = doc.getId();
                        String title   = doc.getString("title");
                        // societyName preferred for display; fallback to organizerUsername
                        String society = doc.getString("societyName");
                        if (society == null) {
                            society = doc.getString("organizerUsername");
                            if (society != null && society.contains("_")) {
                                society = society.substring(society.indexOf("_") + 1).toUpperCase();
                            }
                        }
                        String date = doc.getString("date");
                        if (date == null) date = doc.getString("eventDate");

                        if (title   == null) title   = "Untitled";
                        if (society == null) society = "—";
                        if (date    == null) date    = "—";

                        // Build row
                        LinearLayout row = new LinearLayout(this);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setPadding(10, 14, 10, 14);
                        int rowColor = (container.getChildCount() % 2 == 0) ? 0xFFFFFFFF : 0xFFFFF5F7;
                        row.setBackgroundColor(rowColor);

                        TextView tvSociety = makeCell(society, 1.2f, true);
                        TextView tvTitle   = makeCell(title,   1.8f, false);
                        TextView tvDate    = makeCell(date,    1.2f, false);

                        TextView tvReview = new TextView(this);
                        LinearLayout.LayoutParams btnParams =
                                new LinearLayout.LayoutParams(0,
                                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                        tvReview.setLayoutParams(btnParams);
                        tvReview.setText("Review");
                        tvReview.setTextSize(11f);
                        tvReview.setTextColor(0xFFFFFFFF);
                        tvReview.setTypeface(null, android.graphics.Typeface.BOLD);
                        tvReview.setGravity(Gravity.CENTER);
                        tvReview.setBackgroundResource(R.drawable.bg_button_login);
                        tvReview.setPadding(8, 8, 8, 8);

                        final String proposalId = docId;
                        tvReview.setOnClickListener(v -> {
                            Intent intent = new Intent(this, ProposalDetailActivity.class);
                            intent.putExtra("proposalId", proposalId);
                            startActivity(intent);
                        });

                        row.addView(tvSociety);
                        row.addView(tvTitle);
                        row.addView(tvDate);
                        row.addView(tvReview);
                        container.addView(row);

                        View divider = new View(this);
                        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 1);
                        divider.setLayoutParams(divParams);
                        divider.setBackgroundColor(0xFFF5D5DC);
                        container.addView(divider);
                    }
                });
    }

    private TextView makeCell(String text, float weight, boolean bold) {
        TextView tv = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, weight);
        tv.setLayoutParams(params);
        tv.setText(text);
        tv.setTextSize(12f);
        tv.setTextColor(0xFF2D1B2E);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setPadding(0, 0, 8, 0);
        return tv;
    }
}