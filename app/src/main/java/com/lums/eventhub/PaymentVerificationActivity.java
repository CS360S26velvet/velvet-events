package com.lums.eventhub;

/**
 * PaymentVerificationActivity.java
 *
 * Role: Shows all registrants who submitted payment proof for a given event.
 * Organiser can Approve or Reject each submission.
 * Matches the design shown in the sample screenshots.
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
        String collection = eventId.isEmpty() ? "registrations" : "registrations";
        Query query = db.collection("registrations")
                .whereEqualTo("eventId", eventId);

        query.get()
                .addOnSuccessListener(snap -> {
                    allRegistrants.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        Registrant r = new Registrant();
                        r.docId           = doc.getId();
                        r.studentName     = doc.getString("studentName");
                        r.studentId       = doc.getString("studentId");
                        r.paymentProofUrl = doc.getString("paymentProofUrl");
                        r.amount          = doc.getString("amount");
                        r.paymentStatus   = doc.getString("paymentStatus");
                        r.rejectionReason = doc.getString("rejectionReason");

                        // Parse submitted timestamp
                        Object ts = doc.get("submittedAt");
                        if (ts instanceof Long) {
                            r.submittedDate = formatDate((Long) ts);
                        } else if (ts instanceof String) {
                            r.submittedDate = (String) ts;
                        } else {
                            r.submittedDate = "—";
                        }

                        if (r.studentName    == null) r.studentName    = "Unknown";
                        if (r.studentId      == null) r.studentId      = "—";
                        if (r.amount         == null) r.amount         = "PKR 500";
                        if (r.paymentStatus  == null) r.paymentStatus  = "Pending";
                        if (r.rejectionReason== null) r.rejectionReason= "";

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
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void rejectRegistrant(Registrant r, String reason) {
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
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // -------------------------------------------------------------------------
    // Show detail panel (side drawer simulation via AlertDialog)
    // -------------------------------------------------------------------------

    private void showDetailDialog(Registrant r) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_payment_detail, null);

        TextView tvName      = dialogView.findViewById(R.id.tvDetailName);
        TextView tvId        = dialogView.findViewById(R.id.tvDetailId);
        TextView tvSubmitted = dialogView.findViewById(R.id.tvDetailSubmitted);
        TextView tvAmount    = dialogView.findViewById(R.id.tvDetailAmount);
        TextView tvStatus    = dialogView.findViewById(R.id.tvDetailStatus);
        Button   btnViewProof= dialogView.findViewById(R.id.btnViewProof);
        EditText etReason    = dialogView.findViewById(R.id.etRejectionReason);
        Button   btnApprove  = dialogView.findViewById(R.id.btnConfirmApprove);
        Button   btnReject   = dialogView.findViewById(R.id.btnRejectWithReason);

        tvName.setText(r.studentName);
        tvId.setText(r.studentId);
        tvSubmitted.setText(r.submittedDate);
        tvAmount.setText(r.amount);
        tvStatus.setText(r.paymentStatus);

        // Colour-code status
        int statusColor;
        switch (r.paymentStatus) {
            case "Approved": statusColor = 0xFF4CAF50; break;
            case "Rejected": statusColor = 0xFFF44336; break;
            default:         statusColor = 0xFFFF9800; break;
        }
        tvStatus.setTextColor(statusColor);

        // Pre-fill rejection reason if editing
        if (!r.rejectionReason.isEmpty()) etReason.setText(r.rejectionReason);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnViewProof.setOnClickListener(v -> {
            if (r.paymentProofUrl != null && !r.paymentProofUrl.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(r.paymentProofUrl)));
            } else {
                Toast.makeText(this, "No proof URL available.", Toast.LENGTH_SHORT).show();
            }
        });

        btnApprove.setOnClickListener(v -> {
            dialog.dismiss();
            approveRegistrant(r);
        });

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

    // -------------------------------------------------------------------------
    // Registrant model
    // -------------------------------------------------------------------------

    static class Registrant {
        String docId, studentName, studentId, submittedDate;
        String paymentProofUrl, amount, paymentStatus, rejectionReason;
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