package com.lums.eventhub;

/**
 * PaymentVerificationActivity.java
 *
 * Role: Shows all registrants who submitted payment proof for a given event.
 * Organiser can Approve or Reject each submission.
 *
 * Firestore structure assumed:
 *   registrations/{regId}
 *     eventId          — string
 *     studentName      — string
 *     studentId        — string
 *     submittedAt      — long (timestamp ms) OR string date
 *     paymentProofUrl  — string (download URL of uploaded file)
 *     amount           — string (e.g. "PKR 500")
 *     paymentStatus    — string: "Pending" | "Approved" | "Rejected"
 *     rejectionReason  — string (set when Rejected)
 *
 * Extras received from PaymentVerificationListActivity:
 *   "eventId"   — Firestore document ID of the event
 *   "eventName" — e.g. "SPADES 2025"
 */

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.ImageView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PaymentVerificationActivity extends AppCompatActivity {

    private String            eventId;
    private String            eventName;
    private FirebaseFirestore db;

    private TextView         tvTitle, tvPending, tvApproved, tvRejected;
    private RecyclerView     recyclerView;
    private RegistrantAdapter adapter;
    private List<Registrant>  allRegistrants  = new ArrayList<>();
    private List<Registrant>  shownRegistrants = new ArrayList<>();
    private EditText          etSearch;

    // Filter: "All", "Pending", "Approved", "Rejected"
    private String currentFilter = "All";

    // Tab buttons
    private Button btnFilterAll, btnFilterPending, btnFilterApproved, btnFilterRejected;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_verification);

        db        = FirebaseFirestore.getInstance();
        eventId   = getIntent().getStringExtra("eventId");
        eventName = getIntent().getStringExtra("eventName");
        if (eventId   == null) eventId   = "";
        if (eventName == null) eventName = "Event";

        bindViews();
        setupRecyclerView();
        setupFilterButtons();
        setupSearch();
        loadRegistrants();
    }

    // -------------------------------------------------------------------------
    // Bind views
    // -------------------------------------------------------------------------

    private void bindViews() {
        tvTitle = findViewById(R.id.tvVerificationTitle);
        if (tvTitle != null) tvTitle.setText("Payment Verification — " + eventName);

        tvPending  = findViewById(R.id.tvStatPending);
        tvApproved = findViewById(R.id.tvStatApproved);
        tvRejected = findViewById(R.id.tvStatRejected);
        etSearch   = findViewById(R.id.etSearchRegistrant);

        btnFilterAll      = findViewById(R.id.btnFilterAll);
        btnFilterPending  = findViewById(R.id.btnFilterPending);
        btnFilterApproved = findViewById(R.id.btnFilterApproved);
        btnFilterRejected = findViewById(R.id.btnFilterRejected);
    }

    // -------------------------------------------------------------------------
    // RecyclerView
    // -------------------------------------------------------------------------

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewRegistrants);
        adapter      = new RegistrantAdapter(shownRegistrants);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    // -------------------------------------------------------------------------
    // Filter buttons
    // -------------------------------------------------------------------------

    private void setupFilterButtons() {
        btnFilterAll.setOnClickListener(v      -> applyFilter("All"));
        btnFilterPending.setOnClickListener(v  -> applyFilter("Pending"));
        btnFilterApproved.setOnClickListener(v -> applyFilter("Approved"));
        btnFilterRejected.setOnClickListener(v -> applyFilter("Rejected"));
        highlightFilter("All");
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        highlightFilter(filter);
        rebuildShown();
    }

    private void highlightFilter(String active) {
        int activeColor   = 0xFF1565C0;
        int inactiveColor = 0xFFEEEEEE;
        int activeText    = 0xFFFFFFFF;
        int inactiveText  = 0xFF333333;

        setFilterStyle(btnFilterAll,      "All".equals(active),      activeColor, inactiveColor, activeText, inactiveText);
        setFilterStyle(btnFilterPending,  "Pending".equals(active),  activeColor, inactiveColor, activeText, inactiveText);
        setFilterStyle(btnFilterApproved, "Approved".equals(active), activeColor, inactiveColor, activeText, inactiveText);
        setFilterStyle(btnFilterRejected, "Rejected".equals(active), activeColor, inactiveColor, activeText, inactiveText);
    }

    private void setFilterStyle(Button btn, boolean active,
                                int activeColor, int inactiveColor,
                                int activeText, int inactiveText) {
        if (btn == null) return;
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(active ? activeColor : inactiveColor));
        btn.setTextColor(active ? activeText : inactiveText);
    }

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    private void setupSearch() {
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(android.text.Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                rebuildShown();
            }
        });
    }

    // -------------------------------------------------------------------------
    // Load from Firestore
    // -------------------------------------------------------------------------

    private void loadRegistrants() {
        Query query = db.collection("registrations")
                .whereEqualTo("eventId", eventId);

        query.get()
                .addOnSuccessListener(snap -> {
                    allRegistrants.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Registrant r = new Registrant();
                        r.docId                    = doc.getId();
                        r.studentName              = doc.getString("studentName");
                        r.studentId                = doc.getString("studentId");
                        r.paymentProofUrl          = doc.getString("paymentProofUrl");
                        r.accommodationProofUrl    = doc.getString("accommodationProofUrl");
                        r.paymentProofBase64       = doc.getString("paymentProofBase64");
                        r.accommodationProofBase64 = doc.getString("accommodationProofBase64");
                        r.amount                   = doc.getString("amount");
                        r.paymentStatus            = doc.getString("paymentStatus");
                        r.rejectionReason          = doc.getString("rejectionReason");
                        r.userId                   = doc.getString("userId");
                        if (r.userId == null) r.userId = "";

                        // Parse submitted timestamp
                        Object ts = doc.get("submittedAt");
                        if (ts instanceof Long) {
                            r.submittedDate = formatDate((Long) ts);
                        } else if (ts instanceof String) {
                            r.submittedDate = (String) ts;
                        } else {
                            r.submittedDate = "—";
                        }

                        if (r.studentName             == null) r.studentName             = "Unknown";
                        if (r.studentId               == null) r.studentId               = "—";
                        if (r.amount                  == null) r.amount                  = "PKR 500";
                        if (r.paymentStatus           == null) r.paymentStatus           = "Pending";
                        if (r.rejectionReason         == null) r.rejectionReason         = "";
                        if (r.accommodationProofUrl   == null) r.accommodationProofUrl   = "";
                        if (r.paymentProofBase64      == null) r.paymentProofBase64      = "";
                        if (r.accommodationProofBase64== null) r.accommodationProofBase64= "";

                        allRegistrants.add(r);
                    }
                    updateStats();
                    rebuildShown();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load registrants: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private String formatDate(long millis) {
        return new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(new Date(millis));
    }

    // -------------------------------------------------------------------------
    // Stats
    // -------------------------------------------------------------------------

    private void updateStats() {
        int pending = 0, approved = 0, rejected = 0;
        for (Registrant r : allRegistrants) {
            switch (r.paymentStatus) {
                case "Approved": approved++; break;
                case "Rejected": rejected++; break;
                default:         pending++;  break;
            }
        }
        if (tvPending  != null) tvPending.setText(String.valueOf(pending));
        if (tvApproved != null) tvApproved.setText(String.valueOf(approved));
        if (tvRejected != null) tvRejected.setText(String.valueOf(rejected));
    }

    // -------------------------------------------------------------------------
    // Rebuild shown list (filter + search)
    // -------------------------------------------------------------------------

    private void rebuildShown() {
        String query = etSearch != null ? etSearch.getText().toString().trim().toLowerCase() : "";
        shownRegistrants.clear();
        for (Registrant r : allRegistrants) {
            // Filter by status
            if (!"All".equals(currentFilter) && !currentFilter.equals(r.paymentStatus)) continue;
            // Filter by search
            if (!query.isEmpty()) {
                boolean nameMatch = r.studentName.toLowerCase().contains(query);
                boolean idMatch   = r.studentId.toLowerCase().contains(query);
                if (!nameMatch && !idMatch) continue;
            }
            shownRegistrants.add(r);
        }
        adapter.notifyDataSetChanged();
    }

    // -------------------------------------------------------------------------
    // Approve / Reject
    // -------------------------------------------------------------------------

    private void approveRegistrant(Registrant r) {
        Map<String, Object> update = new HashMap<>();
        update.put("paymentStatus", "Approved");
        update.put("rejectionReason", "");
        db.collection("registrations").document(r.docId)
                .update(update)
                .addOnSuccessListener(unused -> {
                    r.paymentStatus   = "Approved";
                    r.rejectionReason = "";
                    updateStats();
                    rebuildShown();
                    Toast.makeText(this, r.studentName + " approved.", Toast.LENGTH_SHORT).show();
                    // Increment seatsBooked on the event/proposal document
                    incrementSeatsBooked(1);
                    // Notify attendee their payment was approved
                    sendPaymentNotification(r.userId, r.studentName, eventName,
                            "payment_approved",
                            "Payment Approved",
                            "Your payment for " + eventName + " has been approved! You are now registered.");
                    // If attendee also selected accommodation, write to accommodationData subcollection
                    writeAccommodationDataIfNeeded(r);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /** Increments (or decrements) the seatsBooked field on the event document */
    private void incrementSeatsBooked(int delta) {
        if (eventId == null || eventId.isEmpty()) return;
        // Try proposals/ first, then events/
        db.collection("proposals").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        long current = doc.getLong("seatsBooked") != null
                                ? doc.getLong("seatsBooked") : 0;
                        db.collection("proposals").document(eventId)
                                .update("seatsBooked", Math.max(0, current + delta));
                    } else {
                        db.collection("events").document(eventId).get()
                                .addOnSuccessListener(evDoc -> {
                                    if (evDoc.exists()) {
                                        long current = evDoc.getLong("seatsBooked") != null
                                                ? evDoc.getLong("seatsBooked") : 0;
                                        db.collection("events").document(eventId)
                                                .update("seatsBooked", Math.max(0, current + delta));
                                    }
                                });
                    }
                });
    }

    /**
     * Checks whether this registrant selected accommodation.
     * If yes, writes/updates a document in:
     *   accommodationData/{eventId}/attendees/{registrationDocId}
     * containing name, studentId, amount, eventId — for the admin to download.
     */
    private void writeAccommodationDataIfNeeded(Registrant r) {
        // Re-read the registration doc to get wantsAccommodation field
        db.collection("registrations").document(r.docId)
                .get()
                .addOnSuccessListener(doc -> {
                    String wants = doc.getString("wantsAccommodation");
                    if (!"Yes".equals(wants)) return; // not selected — nothing to write

                    String accomAmount = doc.getString("accommodationAmount");
                    if (accomAmount == null) accomAmount = "";

                    Map<String, Object> accomData = new HashMap<>();
                    accomData.put("studentName",         r.studentName);
                    accomData.put("studentId",           r.studentId);
                    accomData.put("accommodationAmount", accomAmount);
                    accomData.put("eventId",             eventId);
                    accomData.put("eventName",           eventName);
                    accomData.put("paymentStatus",       "Approved");
                    accomData.put("wantsAccommodation",  "Yes");
                    accomData.put("approvedAt",          System.currentTimeMillis());

                    db.collection("accommodationData")
                            .document(eventId)
                            .collection("attendees")
                            .document(r.docId)
                            .set(accomData)
                            .addOnFailureListener(e -> {
                                // Non-critical — do not show error to organizer
                                android.util.Log.w("PaymentVerif",
                                        "Could not write accommodationData: " + e.getMessage());
                            });
                });
    }

    private void rejectRegistrant(Registrant r, String reason) {
        boolean wasApproved = "Approved".equals(r.paymentStatus);
        Map<String, Object> update = new HashMap<>();
        update.put("paymentStatus",   "Rejected");
        update.put("rejectionReason", reason);
        db.collection("registrations").document(r.docId)
                .update(update)
                .addOnSuccessListener(unused -> {
                    r.paymentStatus   = "Rejected";
                    r.rejectionReason = reason;
                    updateStats();
                    rebuildShown();
                    Toast.makeText(this, r.studentName + " rejected.", Toast.LENGTH_SHORT).show();
                    // If was previously Approved, decrement seatsBooked
                    if (wasApproved) incrementSeatsBooked(-1);
                    // Send notification to attendee with rejection reason
                    sendPaymentNotification(r.userId, r.studentName, eventName,
                            "payment_rejected",
                            "Payment Rejected",
                            "Your payment for " + eventName + " was rejected. Reason: " + reason
                                    + ". Please re-submit with correct proof.");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Writes a notification document to users/{userId}/notifications
     * so the attendee sees it in NotificationsActivity.
     */
    private void sendPaymentNotification(String userId, String studentName,
                                         String eventTitle, String type,
                                         String title, String message) {
        if (userId == null || userId.isEmpty()) return;
        Map<String, Object> notif = new HashMap<>();
        notif.put("type",      type);
        notif.put("title",     title);
        notif.put("message",   message);
        notif.put("eventName", eventTitle);
        notif.put("read",      false);
        notif.put("timestamp", System.currentTimeMillis());
        db.collection("users").document(userId)
                .collection("notifications")
                .add(notif);
    }

    // -------------------------------------------------------------------------
    // Show detail dialog
    // -------------------------------------------------------------------------

    private void showDetailDialog(Registrant r) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_payment_detail, null);

        TextView  tvName           = dialogView.findViewById(R.id.tvDetailName);
        TextView  tvId             = dialogView.findViewById(R.id.tvDetailId);
        TextView  tvSubmitted      = dialogView.findViewById(R.id.tvDetailSubmitted);
        TextView  tvAmount         = dialogView.findViewById(R.id.tvDetailAmount);
        TextView  tvStatus         = dialogView.findViewById(R.id.tvDetailStatus);
        ImageView imgRegProof      = dialogView.findViewById(R.id.imgRegProof);
        ImageView imgAccomProof    = dialogView.findViewById(R.id.imgAccomProof);
        TextView  tvNoRegProof     = dialogView.findViewById(R.id.tvNoRegProof);
        TextView  tvNoAccomProof   = dialogView.findViewById(R.id.tvNoAccomProof);
        TextView  tvAccomProofLabel= dialogView.findViewById(R.id.tvAccomProofLabel);
        EditText  etReason         = dialogView.findViewById(R.id.etRejectionReason);
        Button    btnApprove       = dialogView.findViewById(R.id.btnConfirmApprove);
        Button    btnReject        = dialogView.findViewById(R.id.btnRejectWithReason);

        tvName.setText(r.studentName);
        tvId.setText(r.studentId);
        tvSubmitted.setText(r.submittedDate);
        tvAmount.setText(r.amount);
        tvStatus.setText(r.paymentStatus);

        int statusColor;
        switch (r.paymentStatus) {
            case "Approved": statusColor = 0xFF4CAF50; break;
            case "Rejected": statusColor = 0xFFF44336; break;
            default:         statusColor = 0xFFFF9800; break;
        }
        tvStatus.setTextColor(statusColor);

        if (!r.rejectionReason.isEmpty()) etReason.setText(r.rejectionReason);

        // ── Registration Proof image ───────────────────────────────────────
        if (r.paymentProofBase64 != null && !r.paymentProofBase64.isEmpty()) {
            Bitmap bmp = base64ToBitmap(r.paymentProofBase64);
            if (bmp != null) {
                imgRegProof.setImageBitmap(bmp);
                imgRegProof.setVisibility(View.VISIBLE);
                if (tvNoRegProof != null) tvNoRegProof.setVisibility(View.GONE);
            } else {
                imgRegProof.setVisibility(View.GONE);
                if (tvNoRegProof != null) tvNoRegProof.setVisibility(View.VISIBLE);
            }
        } else {
            imgRegProof.setVisibility(View.GONE);
            if (tvNoRegProof != null) tvNoRegProof.setVisibility(View.VISIBLE);
        }

        // ── Accommodation Proof image ──────────────────────────────────────
        boolean hasAccomProof = r.accommodationProofBase64 != null
                && !r.accommodationProofBase64.isEmpty();
        if (tvAccomProofLabel != null)
            tvAccomProofLabel.setVisibility(hasAccomProof ? View.VISIBLE : View.GONE);

        if (hasAccomProof) {
            Bitmap bmp = base64ToBitmap(r.accommodationProofBase64);
            if (bmp != null) {
                imgAccomProof.setImageBitmap(bmp);
                imgAccomProof.setVisibility(View.VISIBLE);
                if (tvNoAccomProof != null) tvNoAccomProof.setVisibility(View.GONE);
            } else {
                imgAccomProof.setVisibility(View.GONE);
                if (tvNoAccomProof != null) tvNoAccomProof.setVisibility(View.VISIBLE);
            }
        } else {
            if (imgAccomProof  != null) imgAccomProof.setVisibility(View.GONE);
            if (tvNoAccomProof != null) tvNoAccomProof.setVisibility(View.GONE);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnApprove.setOnClickListener(v -> { dialog.dismiss(); approveRegistrant(r); });

        btnReject.setOnClickListener(v -> {
            String reason = etReason.getText().toString().trim();
            if (reason.isEmpty()) {
                etReason.setError("Please provide a rejection reason.");
                etReason.requestFocus();
                return;
            }
            dialog.dismiss();
            rejectRegistrant(r, reason);
        });

        dialog.show();
    }

    /** Decodes a Base64 string back into a Bitmap. */
    private Bitmap base64ToBitmap(String base64) {
        try {
            byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Registrant model
    // -------------------------------------------------------------------------

    static class Registrant {
        String docId, studentName, studentId, submittedDate, userId;
        String paymentProofUrl, accommodationProofUrl, amount, paymentStatus, rejectionReason;
        String paymentProofBase64, accommodationProofBase64;
    }

    // -------------------------------------------------------------------------
    // Adapter
    // -------------------------------------------------------------------------

    class RegistrantAdapter extends RecyclerView.Adapter<RegistrantAdapter.ViewHolder> {

        private final List<Registrant> list;
        RegistrantAdapter(List<Registrant> list) { this.list = list; }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_registrant_payment, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Registrant r = list.get(position);

            holder.tvName.setText(r.studentName);
            holder.tvStudentId.setText(r.studentId);
            holder.tvDate.setText(r.submittedDate);
            holder.tvAmount.setText(r.amount);
            holder.tvStatus.setText(r.paymentStatus);

            // Status color
            int color;
            switch (r.paymentStatus) {
                case "Approved": color = 0xFF4CAF50; break;
                case "Rejected": color = 0xFFF44336; break;
                default:         color = 0xFFFF9800; break;
            }
            holder.tvStatus.setTextColor(color);

            // Show/hide action buttons
            if ("Pending".equals(r.paymentStatus)) {
                holder.btnApprove.setVisibility(View.VISIBLE);
                holder.btnReject.setVisibility(View.VISIBLE);
                holder.btnApprove.setOnClickListener(v -> showDetailDialog(r));
                holder.btnReject.setOnClickListener(v  -> showDetailDialog(r));
            } else {
                holder.btnApprove.setVisibility(View.GONE);
                holder.btnReject.setVisibility(View.GONE);
            }

            // View proof link
            holder.tvViewProof.setOnClickListener(v -> {
                if (r.paymentProofUrl != null && !r.paymentProofUrl.isEmpty()) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(r.paymentProofUrl)));
                } else {
                    Toast.makeText(PaymentVerificationActivity.this,
                            "No proof uploaded.", Toast.LENGTH_SHORT).show();
                }
            });

            // Row click → full detail dialog
            holder.itemView.setOnClickListener(v -> showDetailDialog(r));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvStudentId, tvDate, tvViewProof, tvAmount, tvStatus;
            Button   btnApprove, btnReject;

            ViewHolder(View v) {
                super(v);
                tvName      = v.findViewById(R.id.tvRegistrantName);
                tvStudentId = v.findViewById(R.id.tvRegistrantStudentId);
                tvDate      = v.findViewById(R.id.tvRegistrantDate);
                tvViewProof = v.findViewById(R.id.tvRegistrantProofLink);
                tvAmount    = v.findViewById(R.id.tvRegistrantAmount);
                tvStatus    = v.findViewById(R.id.tvRegistrantStatus);
                btnApprove  = v.findViewById(R.id.btnApproveRegistrant);
                btnReject   = v.findViewById(R.id.btnRejectRegistrant);
            }
        }
    }
}