package com.lums.eventhub.admin.proposals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lums.eventhub.R;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProposalDetailActivity.java
 *
 * Role: Admin screen that displays the full details of a single proposal
 * submitted by an organizer. Admin can Approve, Request Revision, or Reject
 * the proposal from this screen.
 *
 * On Approve: writes status="Approved" to proposals/ AND copies key fields
 * into the events/ collection using the same document ID as the proposal.
 *
 * Canonical status values written to Firestore:
 *   "Approved"           — organizer sees green badge
 *   "Revision Requested" — organizer sees orange badge
 *   "Rejected"           — organizer sees red badge
 *
 * Proposal sections displayed:
 *   Section 1 — Basic info
 *   Section 2 — Participants
 *   Section 3 — Budget
 *   Section 4 — Sessions
 *   Section 5 — Accommodation
 *   Guests
 *
 * Implements: Admin US-02
 */
/**
 * ProposalDetailActivity.java
 *
 * Loads the full proposal submitted by the organizer and displays ALL fields.
 * Admin can Approve, Request Revision, or Reject.
 *
 * Status values written back to Firestore (canonical):
 *   "Approved"           — organizer sees green badge, Edit hidden
 *   "Revision Requested" — organizer sees orange badge, Edit visible
 *   "Rejected"           — organizer sees red badge, Edit hidden
 */
public class ProposalDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String proposalId;
    private DocumentSnapshot currentDoc;

    // Detail TextViews
    private TextView tvDetailTitle, tvDetailDate, tvDetailVenue,
            tvDetailOrganizer, tvDetailSociety, tvDetailDesc,
            tvDetailEventType, tvDetailParticipants, tvDetailBudget,
            tvDetailAccommodation, tvDetailStatus;
    private LinearLayout llSessions, llGuests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proposal_detail);

        db         = FirebaseFirestore.getInstance();
        proposalId = getIntent().getStringExtra("proposalId");

        bindViews();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadProposal();

        // Decision buttons — canonical status values
        findViewById(R.id.btnApprove).setOnClickListener(v -> approveProposal());
        findViewById(R.id.btnRevision).setOnClickListener(v ->
                updateStatus("Revision Requested"));
        findViewById(R.id.btnReject).setOnClickListener(v ->
                updateStatus("Rejected"));
    }

    private void bindViews() {
        tvDetailTitle        = findViewById(R.id.tvDetailTitle);
        tvDetailDate         = findViewById(R.id.tvDetailDate);
        tvDetailVenue        = findViewById(R.id.tvDetailVenue);
        tvDetailOrganizer    = findViewById(R.id.tvDetailOrganizer);
        tvDetailSociety      = findViewById(R.id.tvDetailSociety);
        tvDetailDesc         = findViewById(R.id.tvDetailDesc);
        tvDetailEventType    = findViewById(R.id.tvDetailEventType);
        tvDetailParticipants = findViewById(R.id.tvDetailParticipants);
        tvDetailBudget       = findViewById(R.id.tvDetailBudget);
        tvDetailAccommodation= findViewById(R.id.tvDetailAccommodation);
        tvDetailStatus       = findViewById(R.id.tvDetailInfo);
        llSessions           = findViewById(R.id.llDetailSessions);
        llGuests             = findViewById(R.id.llDetailGuests);
    }

    private void loadProposal() {
        db.collection("proposals").document(proposalId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Proposal not found.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    currentDoc = doc;
                    populateFields(doc);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading proposal: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    @SuppressWarnings("unchecked")
    private void populateFields(DocumentSnapshot doc) {
        // Section 1 — Basic info
        set(tvDetailTitle,     doc.getString("title"));
        set(tvDetailDesc,      doc.getString("description"));
        set(tvDetailEventType, doc.getString("eventType"));

        // Date — check both field names for compatibility
        String date = doc.getString("date");
        if (date == null) date = doc.getString("eventDate");
        set(tvDetailDate, date);

        set(tvDetailVenue, doc.getString("venue"));

        // Organizer info
        // organizerUsername is canonical (same as organizerId)
        set(tvDetailOrganizer, doc.getString("organizerUsername"));
        set(tvDetailSociety,   doc.getString("societyName"));

        // Section 2 — Participants
        Long participants = doc.getLong("expectedParticipants");
        set(tvDetailParticipants,
                participants != null && participants > 0
                        ? String.valueOf(participants) : "—");

        // Section 3 — Budget
        Long budget = doc.getLong("estimatedBudget");
        set(tvDetailBudget,
                budget != null && budget > 0
                        ? "PKR " + budget : "—");

        // Section 4 — Sessions
        List<Map<String, Object>> sessions =
                (List<Map<String, Object>>) doc.get("sessions");
        if (sessions != null && !sessions.isEmpty() && llSessions != null) {
            llSessions.removeAllViews();
            for (Map<String, Object> s : sessions) {
                TextView tv = new TextView(this);
                tv.setTextSize(13f);
                tv.setTextColor(0xFF2D1B2E);
                tv.setPadding(0, 4, 0, 4);
                String sessionText = "• " + nvl(s.get("name"))
                        + "  |  " + nvl(s.get("venue"))
                        + "  |  " + nvl(s.get("startTime")) + " – " + nvl(s.get("endTime"));
                tv.setText(sessionText);
                llSessions.addView(tv);
            }
        }

        // Section 5 — Accommodation
        Boolean accom = doc.getBoolean("requiresAccommodation");
        if (Boolean.TRUE.equals(accom)) {
            Long count = doc.getLong("accommodationCount");
            String checkIn  = doc.getString("checkInDate");
            String checkOut = doc.getString("checkOutDate");
            String special  = doc.getString("specialRequirements");
            String accomText = "Yes — " + (count != null ? count : "—") + " rooms"
                    + "\nCheck-in: " + nvl(checkIn)
                    + "  Check-out: " + nvl(checkOut);
            if (special != null && !special.isEmpty()) {
                accomText += "\nNotes: " + special;
            }
            set(tvDetailAccommodation, accomText);
        } else {
            set(tvDetailAccommodation, "Not required");
        }

        // Guests
        List<Map<String, Object>> guests =
                (List<Map<String, Object>>) doc.get("guests");
        if (guests != null && !guests.isEmpty() && llGuests != null) {
            llGuests.removeAllViews();
            for (Map<String, Object> g : guests) {
                TextView tv = new TextView(this);
                tv.setTextSize(13f);
                tv.setTextColor(0xFF2D1B2E);
                tv.setPadding(0, 4, 0, 4);
                tv.setText("• " + nvl(g.get("name"))
                        + " — " + nvl(g.get("title"))
                        + ", " + nvl(g.get("organization")));
                llGuests.addView(tv);
            }
        }

        // Current status
        String status = doc.getString("status");
        if (tvDetailStatus != null) {
            tvDetailStatus.setText("Current Status: " + (status != null ? status.toUpperCase() : "UNKNOWN"));
        }
    }

    private void approveProposal() {
        if (currentDoc == null) {
            Toast.makeText(this, "Proposal not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("proposals").document(proposalId)
                .update("status", "Approved")
                .addOnSuccessListener(a -> writeToEventsCollection())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Approval failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void writeToEventsCollection() {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("title",             nvlStr(currentDoc.getString("title")));
        eventData.put("date",              nvlStr(currentDoc.getString("date")));
        eventData.put("venue",             nvlStr(currentDoc.getString("venue")));
        eventData.put("societyName",       nvlStr(currentDoc.getString("societyName")));
        eventData.put("organizerUsername", nvlStr(currentDoc.getString("organizerUsername")));
        eventData.put("description",       nvlStr(currentDoc.getString("description")));
        eventData.put("eventType",         nvlStr(currentDoc.getString("eventType")));
        eventData.put("status",            "Approved");
        eventData.put("approvedAt",        System.currentTimeMillis());
        eventData.put("proposalId",        proposalId);

        // Same document ID as the proposal for easy cross-reference
        db.collection("events").document(proposalId)
                .set(eventData)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Proposal approved ✓", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Approved but event record failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void updateStatus(String status) {
        db.collection("proposals").document(proposalId)
                .update("status", status)
                .addOnSuccessListener(a -> {
                    String msg;
                    switch (status) {
                        case "Rejected":           msg = "Proposal rejected";  break;
                        case "Revision Requested": msg = "Revision requested"; break;
                        default:                   msg = "Status updated";
                    }
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private String nvlStr(String s) {
        return s != null ? s : "";
    }

    private void set(TextView tv, String value) {
        if (tv != null) tv.setText(value != null && !value.isEmpty() ? value : "—");
    }

    private String nvl(Object o) {
        return o != null ? o.toString() : "—";
    }
}