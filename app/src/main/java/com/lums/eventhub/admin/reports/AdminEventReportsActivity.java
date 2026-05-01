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
import java.util.List;
import java.util.Locale;

/**
 * AdminEventReportsActivity.java
 *
 * Admin views ALL submitted event reports from all societies.
 * Each report shows: event name, society, date, attendees, status.
 * Tapping "View Report" opens a dialog with the JPEG image + notes inline.
 *
 * Firestore reads: eventReports/ (all documents)
 */
public class AdminEventReportsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView      recyclerView;
    private AdminReportAdapter adapter;
    private final List<ReportDoc> reports = new ArrayList<>();

    private TextView tvTotalReports, tvSocietiesCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_event_reports);

        db = FirebaseFirestore.getInstance();

        tvTotalReports   = findViewById(R.id.tvAdminTotalReports);
        tvSocietiesCount = findViewById(R.id.tvAdminSocietiesCount);

        recyclerView = findViewById(R.id.recyclerAdminReports);
        adapter      = new AdminReportAdapter(reports);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnAdminReportsBack).setOnClickListener(v -> finish());

        loadReports();
    }

    // ── Load all reports ──────────────────────────────────────────────────────

    private void loadReports() {
        db.collection("eventReports")
                .whereEqualTo("status", "Submitted")
                .get()
                .addOnSuccessListener(snap -> {
                    reports.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        ReportDoc r = new ReportDoc();
                        r.id             = doc.getId();
                        r.eventTitle     = nvl(doc.getString("eventTitle"), "Untitled");
                        r.societyName    = nvl(doc.getString("societyName"), "—");
                        r.eventDate      = nvl(doc.getString("eventDate"), "—");
                        r.imageBase64    = nvl(doc.getString("imageBase64"), "");
                        r.notes          = nvl(doc.getString("notes"), "");
                        Long att = doc.getLong("attendees");
                        r.attendees      = att != null ? att.intValue() : 0;
                        Long ts = doc.getLong("submittedAt");
                        r.submittedAt    = ts != null
                                ? new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                .format(new Date(ts)) : "—";
                        reports.add(r);
                    }

                    updateStats();
                    adapter.notifyDataSetChanged();

                    if (reports.isEmpty()) {
                        TextView tvEmpty = findViewById(R.id.tvAdminReportsEmpty);
                        if (tvEmpty != null) tvEmpty.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load reports", Toast.LENGTH_SHORT).show());
    }

    private void updateStats() {
        tvTotalReports.setText(String.valueOf(reports.size()));
        // Count unique societies
        List<String> societies = new ArrayList<>();
        for (ReportDoc r : reports) {
            if (!societies.contains(r.societyName)) societies.add(r.societyName);
        }
        tvSocietiesCount.setText(String.valueOf(societies.size()));
    }

    // ── View report dialog ────────────────────────────────────────────────────

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

        new AlertDialog.Builder(this)
                .setView(view)
                .setNegativeButton("Close", null)
                .show();
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
        String id, eventTitle, societyName, eventDate;
        String imageBase64, notes, submittedAt;
        int    attendees;
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    class AdminReportAdapter extends RecyclerView.Adapter<AdminReportAdapter.VH> {

        private final List<ReportDoc> list;
        AdminReportAdapter(List<ReportDoc> list) { this.list = list; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_report, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int position) {
            ReportDoc r = list.get(position);
            h.tvEventName.setText(r.eventTitle);
            h.tvSociety.setText(r.societyName);
            h.tvDate.setText("📅 " + r.eventDate);
            h.tvAttendees.setText("👥 " + r.attendees);
            h.tvSubmitted.setText("Submitted " + r.submittedAt);
            h.btnView.setOnClickListener(v -> showReportDialog(r));
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvEventName, tvSociety, tvDate, tvAttendees, tvSubmitted;
            Button   btnView;
            VH(View v) {
                super(v);
                tvEventName = v.findViewById(R.id.tvAdminReportEventName);
                tvSociety   = v.findViewById(R.id.tvAdminReportSocietyName);
                tvDate      = v.findViewById(R.id.tvAdminReportEventDate);
                tvAttendees = v.findViewById(R.id.tvAdminReportAttendeesCount);
                tvSubmitted = v.findViewById(R.id.tvAdminReportSubmittedDate);
                btnView     = v.findViewById(R.id.btnAdminViewReport);
            }
        }
    }
}