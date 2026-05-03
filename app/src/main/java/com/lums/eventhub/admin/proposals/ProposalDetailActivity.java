package com.lums.eventhub.admin.proposals;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lums.eventhub.R;

import android.app.AlertDialog;
import android.widget.EditText;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProposalDetailActivity.java
 *
 * CHANGES:
 *   - Budget section now shows the budget document IMAGE (budgetImageBase64)
 *     submitted by the organizer — visible to admin below the PKR amount.
 *
 * On Approve: writes status="Approved" to proposals/ AND copies key fields
 * into the events/ collection using the same document ID as the proposal.
 *
 * Status values written to Firestore:
 *   "Approved"           — organizer sees green badge
 *   "Revision Requested" — organizer sees orange badge + Edit button
 *   "Rejected"           — organizer sees red badge + reason
 */
public class ProposalDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String            proposalId;
    private DocumentSnapshot  currentDoc;

    private TextView    tvDetailTitle, tvDetailDate, tvDetailVenue,
            tvDetailOrganizer, tvDetailSociety, tvDetailDesc,
            tvDetailEventType, tvDetailParticipants, tvDetailBudget,
            tvDetailAccommodation, tvDetailStatus;
    private ImageView   imgDetailBudgetDoc;
    private LinearLayout llSessions, llGuests, llDecisionButtons;
    private TextView    tvApprovedBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proposal_detail);

        db         = FirebaseFirestore.getInstance();
        proposalId = getIntent().getStringExtra("proposalId");

        bindViews();
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        loadProposal();

        findViewById(R.id.btnApprove).setOnClickListener(v -> approveProposal());
        findViewById(R.id.btnRevision).setOnClickListener(v ->
                showReasonDialog("Revision Requested"));
        findViewById(R.id.btnReject).setOnClickListener(v ->
                showReasonDialog("Rejected"));
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
        imgDetailBudgetDoc   = findViewById(R.id.imgDetailBudgetDoc);
        llSessions           = findViewById(R.id.llDetailSessions);
        llGuests             = findViewById(R.id.llDetailGuests);
        llDecisionButtons    = findViewById(R.id.llDecisionButtons);
        tvApprovedBadge      = findViewById(R.id.tvApprovedBadge);
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

        String date = doc.getString("startDate");
        if (date == null) date = doc.getString("date");
        if (date == null) date = doc.getString("eventDate");
        set(tvDetailDate, date);

        set(tvDetailVenue,     doc.getString("venue"));
        set(tvDetailOrganizer, doc.getString("organizerUsername"));
        set(tvDetailSociety,   doc.getString("societyName"));

        // Section 2 — Participants
        Long participants = doc.getLong("expectedParticipants");
        set(tvDetailParticipants,
                participants != null && participants > 0
                        ? String.valueOf(participants) : "—");

        // Section 3 — Budget amount
        Long budget = doc.getLong("estimatedBudget");
        set(tvDetailBudget,
                budget != null && budget > 0 ? "PKR " + budget : "—");

        // Section 3 — Budget document image (if organizer attached one)
        String budgetImg = doc.getString("budgetImageBase64");
        if (budgetImg != null && !budgetImg.isEmpty() && imgDetailBudgetDoc != null) {
            Bitmap bmp = bitmapFromBase64(budgetImg);
            if (bmp != null) {
                imgDetailBudgetDoc.setImageBitmap(bmp);
                imgDetailBudgetDoc.setVisibility(View.VISIBLE);
            }
        }

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
                tv.setText("• " + nvl(s.get("name"))
                        + "  |  " + nvl(s.get("venue"))
                        + "  |  " + nvl(s.get("startTime")) + " – " + nvl(s.get("endTime")));
                llSessions.addView(tv);
            }
        }

        // Section 5 — Accommodation
        Boolean accom = doc.getBoolean("requiresAccommodation");
        if (Boolean.TRUE.equals(accom)) {
            Long count   = doc.getLong("accommodationCount");
            String checkIn  = doc.getString("checkInDate");
            String checkOut = doc.getString("checkOutDate");
            String special  = doc.getString("specialRequirements");
            String accomText = "Yes — " + (count != null ? count : "—") + " rooms"
                    + "\nCheck-in: " + nvlStr(checkIn)
                    + "  Check-out: " + nvlStr(checkOut);
            if (special != null && !special.isEmpty()) accomText += "\nNotes: " + special;
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
            tvDetailStatus.setText("Current Status: " +
                    (status != null ? status.toUpperCase() : "UNKNOWN"));
        }

        if ("Approved".equals(status)) {
            if (llDecisionButtons != null) llDecisionButtons.setVisibility(View.GONE);
            if (tvApprovedBadge   != null) tvApprovedBadge.setVisibility(View.VISIBLE);
        } else {
            if (llDecisionButtons != null) llDecisionButtons.setVisibility(View.VISIBLE);
            if (tvApprovedBadge   != null) tvApprovedBadge.setVisibility(View.GONE);
        }
    }

    private void approveProposal() {
        if (currentDoc == null) {
            Toast.makeText(this, "Proposal not loaded yet.", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("proposals").document(proposalId)
                .update("status", "Approved")
                .addOnSuccessListener(a -> {
                    if (llDecisionButtons != null) llDecisionButtons.setVisibility(View.GONE);
                    if (tvApprovedBadge   != null) tvApprovedBadge.setVisibility(View.VISIBLE);
                    writeToEventsCollection();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Approval failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void writeToEventsCollection() {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("title",             nvlStr(currentDoc.getString("title")));
        eventData.put("date",              nvlStr(currentDoc.getString("date")));
        eventData.put("startDate",         nvlStr(currentDoc.getString("startDate")));
        eventData.put("endDate",           nvlStr(currentDoc.getString("endDate")));
        eventData.put("venue",             nvlStr(currentDoc.getString("venue")));
        eventData.put("societyName",       nvlStr(currentDoc.getString("societyName")));
        eventData.put("organizerUsername", nvlStr(currentDoc.getString("organizerUsername")));
        eventData.put("description",       nvlStr(currentDoc.getString("description")));
        eventData.put("eventType",         nvlStr(currentDoc.getString("eventType")));
        eventData.put("status",            "Approved");
        eventData.put("approvedAt",        System.currentTimeMillis());
        eventData.put("proposalId",        proposalId);
        eventData.put("reportStatus",      "");

        String deadline = currentDoc.getString("registrationDeadline");
        if (deadline != null && !deadline.isEmpty()) {
            eventData.put("registrationDeadline", deadline);
        }
        String imageBase64 = currentDoc.getString("eventImageBase64");
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            eventData.put("eventImageBase64", imageBase64);
        }

        db.collection("events").document(proposalId)
                .set(eventData)
                .addOnSuccessListener(a -> {
                    resetPreviousEventCycle(proposalId);
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

    /**
     * Wipes all data from the previous event cycle so the new year starts fresh.
     * Deletes: eventReports, registrationForms, accommodationData, formQuestions,
     * all registrations (main + mirror copies on attendee profiles + calendar entries).
     * Feedback is kept for historical reference.
     */
    private void resetPreviousEventCycle(String eventId) {
        // Event report
        db.collection("eventReports").document(eventId).delete();
        // Registration form
        db.collection("registrationForms").document(eventId).delete();
        // Accommodation data
        db.collection("accommodationData").document(eventId).delete();
        // Form questions
        db.collection("formQuestions").document(eventId).delete();

        // All registrations for this event
        db.collection("registrations")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snap -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot regDoc : snap) {
                        String uid = regDoc.getString("userId");
                        // Delete main registration doc
                        regDoc.getReference().delete();
                        // Delete mirror copy + calendar entry on attendee profile
                        if (uid != null && !uid.isEmpty()) {
                            db.collection("users").document(uid)
                                    .collection("registrations").document(eventId).delete();
                            db.collection("users").document(uid)
                                    .collection("calendarEvents").document(eventId).delete();
                        }
                    }
                })
                .addOnFailureListener(e -> android.util.Log.w("ProposalDetail",
                        "Could not clear old registrations: " + e.getMessage()));
    }

    private void showReasonDialog(String status) {
        EditText etReason = new EditText(this);
        etReason.setHint("Enter reason (shown to organizer)...");
        etReason.setPadding(32, 16, 32, 16);

        String title = "Revision Requested".equals(status)
                ? "Request Revision" : "Reject Proposal";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("This reason will be shown to the organizer.")
                .setView(etReason)
                .setPositiveButton("Submit", (d, w) -> {
                    String reason = etReason.getText().toString().trim();
                    updateStatusWithReason(status, reason);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateStatusWithReason(String status, String reason) {
        Map<String, Object> update = new HashMap<>();
        update.put("status",      status);
        update.put("adminReason", reason);
        update.put("reviewedAt",  System.currentTimeMillis());

        db.collection("proposals").document(proposalId)
                .update(update)
                .addOnSuccessListener(a -> {
                    String msg = "Revision Requested".equals(status)
                            ? "Revision requested" : "Proposal rejected";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Bitmap bitmapFromBase64(String b64) {
        try {
            byte[] bytes = Base64.decode(b64, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) { return null; }
    }

    private void set(TextView tv, String value) {
        if (tv != null) tv.setText(value != null && !value.isEmpty() ? value : "—");
    }

    private String nvlStr(String s) { return s != null ? s : ""; }

    private String nvl(Object o) { return o != null ? o.toString() : "—"; }
}