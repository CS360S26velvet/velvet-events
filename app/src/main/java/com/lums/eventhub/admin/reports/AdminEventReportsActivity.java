package com.lums.eventhub.admin.reports;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.lums.eventhub.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AdminEventReportsActivity.java  (REWRITTEN)
 *
 * Admin color scheme: #8B1A4A header, white cards.
 *
 * Layout: societies accordion (same pattern as AdminAccommodationBySocietyActivity).
 *   Society header (tap to expand) → list of events with submitted reports.
 *   Each event row has a "View Report" button.
 *   View Report dialog shows image + notes + APPROVE / REJECT buttons.
 *   Approve → sets eventReports/{id}.status = "Approved"
 *             → organizer side sees "Approved" badge
 *   Reject  → prompts for reason → sets status = "Rejected", rejectionReason = reason
 *             → organizer side sees "Rejected" badge + reason + "Add" button
 *
 * Firestore reads:  eventReports/ where status == "Submitted"
 * Firestore writes: eventReports/{id}  status, rejectionReason, reviewedAt
 */
public class AdminEventReportsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout      llSocieties;
    private TextView          tvEmpty;

    // societyName -> list of reports
    private final Map<String, List<ReportDoc>> grouped = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_event_reports);

        db          = FirebaseFirestore.getInstance();
        llSocieties = findViewById(R.id.llAdminReportSocieties);
        tvEmpty     = findViewById(R.id.tvAdminReportsEmpty);

        findViewById(R.id.btnAdminReportsBack).setOnClickListener(v -> finish());

        loadReports();
    }

    // ── Load & group by society ───────────────────────────────────────────────

    private void loadReports() {
        db.collection("eventReports")
                .whereEqualTo("status", "Submitted")
                .get()
                .addOnSuccessListener(snap -> {
                    grouped.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        ReportDoc r      = new ReportDoc();
                        r.id             = doc.getId();
                        r.eventId        = nvl(doc.getString("eventId"), doc.getId());
                        r.eventTitle     = nvl(doc.getString("eventTitle"), "Untitled");
                        r.societyName    = nvl(doc.getString("societyName"), "Unknown Society");
                        r.eventDate      = nvl(doc.getString("eventDate"), "—");
                        r.imageBase64    = nvl(doc.getString("imageBase64"), "");
                        r.notes          = nvl(doc.getString("notes"), "");
                        Long att         = doc.getLong("attendees");
                        r.attendees      = att != null ? att.intValue() : 0;
                        Long ts          = doc.getLong("submittedAt");
                        r.submittedAt    = ts != null
                                ? new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                .format(new Date(ts)) : "—";

                        if (!grouped.containsKey(r.societyName)) {
                            grouped.put(r.societyName, new ArrayList<>());
                        }
                        grouped.get(r.societyName).add(r);
                    }

                    buildUI();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load reports: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ── Build accordion UI ────────────────────────────────────────────────────

    private void buildUI() {
        llSocieties.removeAllViews();

        if (grouped.isEmpty()) {
            if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);

        for (Map.Entry<String, List<ReportDoc>> entry : grouped.entrySet()) {
            String          society = entry.getKey();
            List<ReportDoc> list    = entry.getValue();

            // Society header
            TextView tvHeader = new TextView(this);
            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            hp.setMargins(0, 12, 0, 0);
            tvHeader.setLayoutParams(hp);
            tvHeader.setText(society + "  (" + list.size() + " report" +
                    (list.size() > 1 ? "s" : "") + ") ▼");
            tvHeader.setTextColor(0xFFFFFFFF);
            tvHeader.setTextSize(15f);
            tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);
            tvHeader.setBackgroundColor(0xFF8B1A4A);
            tvHeader.setPadding(32, 24, 32, 24);

            // Events container (starts expanded)
            LinearLayout llEvents = new LinearLayout(this);
            llEvents.setOrientation(LinearLayout.VERTICAL);
            llEvents.setBackgroundColor(0xFFFFFFFF);

            for (ReportDoc r : list) {
                llEvents.addView(buildEventRow(r));
            }

            // Toggle expand/collapse
            tvHeader.setOnClickListener(v -> {
                boolean visible = llEvents.getVisibility() == View.VISIBLE;
                llEvents.setVisibility(visible ? View.GONE : View.VISIBLE);
                tvHeader.setText(society + "  (" + list.size() + " report" +
                        (list.size() > 1 ? "s" : "") + (visible ? " ▶" : " ▼"));
            });

            llSocieties.addView(tvHeader);
            llSocieties.addView(llEvents);
        }
    }

    private View buildEventRow(ReportDoc r) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(24, 20, 24, 20);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Add a bottom divider feel with alternating background
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(rp);
        row.setBackgroundColor(0xFFFAFAFA);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        info.setLayoutParams(ip);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(r.eventTitle);
        tvTitle.setTextSize(14f);
        tvTitle.setTextColor(0xFF2D1B2E);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvDate = new TextView(this);
        tvDate.setText("📅 " + r.eventDate + "  •  👥 " + r.attendees);
        tvDate.setTextSize(12f);
        tvDate.setTextColor(0xFF666666);

        TextView tvSubmitted = new TextView(this);
        tvSubmitted.setText("Submitted: " + r.submittedAt);
        tvSubmitted.setTextSize(11f);
        tvSubmitted.setTextColor(0xFF999999);

        info.addView(tvTitle);
        info.addView(tvDate);
        info.addView(tvSubmitted);

        Button btnView = new Button(this);
        btnView.setText("View Report");
        btnView.setTextSize(12f);
        btnView.setTextColor(0xFFFFFFFF);
        btnView.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF8B1A4A));
        btnView.setPadding(16, 8, 16, 8);
        btnView.setOnClickListener(v -> showReportDialog(r));

        row.addView(info);
        row.addView(btnView);

        // Thin divider below
        View divider = new View(this);
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        divider.setLayoutParams(dp);
        divider.setBackgroundColor(0xFFEEEEEE);
        outer.addView(row);
        outer.addView(divider);
        return outer;
    }

    // ── Report dialog with Approve / Reject ───────────────────────────────────

    private void showReportDialog(ReportDoc r) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_admin_view_report, null);

        TextView  tvTitle    = view.findViewById(R.id.tvAdminReportTitle);
        TextView  tvSociety  = view.findViewById(R.id.tvAdminReportSociety);
        TextView  tvDate     = view.findViewById(R.id.tvAdminReportDate);
        TextView  tvAtt      = view.findViewById(R.id.tvAdminReportAttendees);
        TextView  tvSubmitted= view.findViewById(R.id.tvAdminReportSubmitted);
        TextView  tvNotes    = view.findViewById(R.id.tvAdminReportNotes);
        ImageView imgReport  = view.findViewById(R.id.imgAdminReport);
        TextView  tvNoImage  = view.findViewById(R.id.tvAdminReportNoImage);

        tvTitle.setText(r.eventTitle);
        tvSociety.setText(r.societyName);
        tvDate.setText("Event Date: " + r.eventDate);
        tvAtt.setText("Attendees: " + r.attendees);
        tvSubmitted.setText("Submitted: " + r.submittedAt);
        tvNotes.setText(r.notes.isEmpty() ? "No notes provided." : r.notes);

        if (!r.imageBase64.isEmpty()) {
            Bitmap bmp = base64ToBitmap(r.imageBase64);
            if (bmp != null) {
                imgReport.setImageBitmap(bmp);
                imgReport.setVisibility(View.VISIBLE);
                if (tvNoImage != null) tvNoImage.setVisibility(View.GONE);
            }
        } else {
            imgReport.setVisibility(View.GONE);
            if (tvNoImage != null) tvNoImage.setVisibility(View.VISIBLE);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setNeutralButton("Close", null)
                .setPositiveButton("Approve", null)
                .setNegativeButton("Reject", null)
                .create();

        dialog.setOnShowListener(d -> {
            // Approve
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                approveReport(r, dialog);
            });
            // Reject
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
                dialog.dismiss();
                showRejectDialog(r);
            });
        });

        dialog.show();
    }

    private void approveReport(ReportDoc r, AlertDialog dialog) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", "Approved");
        update.put("rejectionReason", "");
        update.put("reviewedAt", System.currentTimeMillis());

        db.collection("eventReports").document(r.id)
                .update(update)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Report approved.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    // Also write reportStatus to events/ so organizer sees "Edit Prior Event"
                    db.collection("events").document(r.eventId)
                            .update("reportStatus", "Approved")
                            .addOnFailureListener(e -> android.util.Log.w("AdminReports",
                                    "Could not update events reportStatus: " + e.getMessage()));
                    // Remove from list and refresh UI
                    for (List<ReportDoc> list : grouped.values()) {
                        list.removeIf(rep -> rep.id.equals(r.id));
                    }
                    grouped.entrySet().removeIf(e -> e.getValue().isEmpty());
                    buildUI();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void showRejectDialog(ReportDoc r) {
        EditText etReason = new EditText(this);
        etReason.setHint("Enter rejection reason...");
        etReason.setPadding(32, 16, 32, 16);

        new AlertDialog.Builder(this)
                .setTitle("Reject Report")
                .setMessage("Provide a reason for rejection. This will be shown to the organizer.")
                .setView(etReason)
                .setPositiveButton("Submit Rejection", (d, w) -> {
                    String reason = etReason.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(this, "Please enter a reason.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    rejectReport(r, reason);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void rejectReport(ReportDoc r, String reason) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", "Rejected");
        update.put("rejectionReason", reason);
        update.put("reviewedAt", System.currentTimeMillis());

        db.collection("eventReports").document(r.id)
                .update(update)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Report rejected.", Toast.LENGTH_SHORT).show();
                    for (List<ReportDoc> list : grouped.values()) {
                        list.removeIf(rep -> rep.id.equals(r.id));
                    }
                    grouped.entrySet().removeIf(e -> e.getValue().isEmpty());
                    buildUI();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Bitmap base64ToBitmap(String b64) {
        try {
            byte[] bytes = Base64.decode(b64, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) { return null; }
    }

    private String nvl(String s, String fallback) {
        return (s != null && !s.isEmpty()) ? s : fallback;
    }

    // ── Model ─────────────────────────────────────────────────────────────────

    static class ReportDoc {
        String id, eventId, eventTitle, societyName, eventDate;
        String imageBase64, notes, submittedAt;
        int    attendees;
    }
}