package com.lums.eventhub;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * EventReportsActivity.java
 *
 * Organiser views all their completed/approved events and submits
 * a post-event report (JPEG image + optional notes) for each.
 *
 * Firestore:
 *   reads:  proposals/ + events/ where organizerUsername == mine AND status == Approved/Completed
 *   writes: eventReports/{eventId}
 *             organizerUsername, eventTitle, imageBase64, notes, submittedAt, status="Submitted"
 *
 * Received from OrganizerDashboardActivity:
 *   "organizerUsername", "societyName"
 */
public class EventReportsActivity extends AppCompatActivity {

    private static final int IMAGE_PICK_RC = 500;

    private FirebaseFirestore db;
    private String organizerUsername, societyName;

    private RecyclerView    recyclerView;
    private ReportAdapter   adapter;
    private final List<EventReportItem> items = new ArrayList<>();

    // Stats
    private TextView tvTotalEvents, tvSubmitted, tvPending;

    // Pending image pick
    private String pendingEventId, pendingEventTitle;
    private String pickedImageBase64 = "";
    private Button pendingPickButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_reports);

        db = FirebaseFirestore.getInstance();
        organizerUsername = getIntent().getStringExtra("organizerUsername");
        societyName       = getIntent().getStringExtra("societyName");
        if (organizerUsername == null) organizerUsername = "ORG0012";
        if (societyName == null)       societyName       = "My Society";

        tvTotalEvents = findViewById(R.id.tvReportTotalEvents);
        tvSubmitted   = findViewById(R.id.tvReportSubmitted);
        tvPending     = findViewById(R.id.tvReportPending);

        recyclerView = findViewById(R.id.recyclerViewReports);
        adapter      = new ReportAdapter(items);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnReportsBack).setOnClickListener(v -> finish());

        loadEvents();
    }

    // ── Load completed/approved events ────────────────────────────────────────

    private void loadEvents() {
        items.clear();

        // Load from proposals/ (Approved)
        db.collection("proposals")
                .whereEqualTo("organizerUsername", organizerUsername)
                .whereEqualTo("status", "Approved")
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        EventReportItem item = itemFromDoc(doc);
                        items.add(item);
                    }
                    loadCompletedEvents();
                })
                .addOnFailureListener(e -> loadCompletedEvents());
    }

    private void loadCompletedEvents() {
        // Also load Completed from events/
        db.collection("events")
                .whereEqualTo("organizerUsername", organizerUsername)
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        String status = doc.getString("status");
                        if (!"Approved".equals(status) && !"Completed".equals(status)) continue;
                        // Avoid duplicates
                        String id = doc.getId();
                        boolean exists = false;
                        for (EventReportItem i : items) if (i.eventId.equals(id)) { exists=true; break; }
                        if (!exists) items.add(itemFromDoc(doc));
                    }
                    // Now load existing report statuses
                    loadReportStatuses();
                })
                .addOnFailureListener(e -> loadReportStatuses());
    }

    private EventReportItem itemFromDoc(QueryDocumentSnapshot doc) {
        EventReportItem item = new EventReportItem();
        item.eventId    = doc.getId();
        item.eventTitle = nvl(doc.getString("title"), "Untitled");
        item.eventDate  = nvl(doc.getString("startDate"), nvl(doc.getString("date"), "—"));
        Long att = doc.getLong("expectedParticipants");
        item.attendees  = att != null ? att.intValue() : 0;
        item.reportStatus = "Pending";
        item.submittedAt  = "";
        return item;
    }

    private void loadReportStatuses() {
        if (items.isEmpty()) {
            updateStats();
            adapter.notifyDataSetChanged();
            return;
        }

        // For each item check if a report doc exists
        final int[] remaining = {items.size()};
        for (EventReportItem item : items) {
            db.collection("eventReports").document(item.eventId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            item.reportStatus = nvl(doc.getString("status"), "Submitted");
                            Long ts = doc.getLong("submittedAt");
                            if (ts != null) {
                                item.submittedAt = new SimpleDateFormat("MMM d, yyyy",
                                        Locale.getDefault()).format(new Date(ts));
                            }
                            item.imageBase64 = nvl(doc.getString("imageBase64"), "");
                            item.notes       = nvl(doc.getString("notes"), "");
                        }
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            updateStats();
                            adapter.notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> {
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            updateStats();
                            adapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    private void updateStats() {
        int total     = items.size();
        int submitted = 0;
        for (EventReportItem i : items) if ("Submitted".equals(i.reportStatus)) submitted++;
        int pending = total - submitted;

        tvTotalEvents.setText(String.valueOf(total));
        tvSubmitted.setText(String.valueOf(submitted));
        tvPending.setText(String.valueOf(pending));
    }

    // ── Add Report dialog ─────────────────────────────────────────────────────

    private void showAddReportDialog(EventReportItem item) {
        pendingEventId    = item.eventId;
        pendingEventTitle = item.eventTitle;
        pickedImageBase64 = "";
        pendingPickButton = null;

        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_add_report, null);

        TextView  tvTitle   = view.findViewById(R.id.tvAddReportTitle);
        Button    btnPick   = view.findViewById(R.id.btnPickReportImage);
        ImageView imgPreview= view.findViewById(R.id.imgReportPreview);
        EditText  etNotes   = view.findViewById(R.id.etReportNotes);

        tvTitle.setText(item.eventTitle);
        pendingPickButton = btnPick;

        btnPick.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(
                    Intent.createChooser(intent, "Select Report Image"), IMAGE_PICK_RC);
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Submit Report", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnSubmit = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnSubmit.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF1565C0));
            btnSubmit.setTextColor(0xFFFFFFFF);

            btnSubmit.setOnClickListener(v -> {
                if (pickedImageBase64.isEmpty()) {
                    Toast.makeText(this,
                            "Please attach a report image first.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String notes = etNotes.getText().toString().trim();
                dialog.dismiss();
                submitReport(item, pickedImageBase64, notes);
            });
        });

        dialog.show();

        // Store preview ref for onActivityResult
        view.setTag(imgPreview);
    }

    // ── View existing report ──────────────────────────────────────────────────

    private void showViewReportDialog(EventReportItem item) {
        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_view_report, null);

        TextView  tvTitle  = view.findViewById(R.id.tvViewReportTitle);
        ImageView imgProof = view.findViewById(R.id.imgViewReport);
        TextView  tvNotes  = view.findViewById(R.id.tvViewReportNotes);
        TextView  tvDate   = view.findViewById(R.id.tvViewReportDate);

        tvTitle.setText(item.eventTitle);
        tvDate.setText("Submitted: " + item.submittedAt);
        tvNotes.setText(item.notes.isEmpty() ? "No notes added." : item.notes);

        if (!item.imageBase64.isEmpty()) {
            Bitmap bmp = base64ToBitmap(item.imageBase64);
            if (bmp != null) {
                imgProof.setImageBitmap(bmp);
                imgProof.setVisibility(View.VISIBLE);
            }
        }

        new AlertDialog.Builder(this)
                .setView(view)
                .setNegativeButton("Close", null)
                .show();
    }

    // ── Submit to Firestore ───────────────────────────────────────────────────

    private void submitReport(EventReportItem item, String imageBase64, String notes) {
        Map<String, Object> report = new HashMap<>();
        report.put("organizerUsername", organizerUsername);
        report.put("societyName",       societyName);
        report.put("eventId",           item.eventId);
        report.put("eventTitle",        item.eventTitle);
        report.put("eventDate",         item.eventDate);
        report.put("attendees",         item.attendees);
        report.put("imageBase64",       imageBase64);
        report.put("notes",             notes);
        report.put("status",            "Submitted");
        report.put("submittedAt",       System.currentTimeMillis());

        db.collection("eventReports").document(item.eventId)
                .set(report)
                .addOnSuccessListener(v -> {
                    item.reportStatus = "Submitted";
                    item.imageBase64  = imageBase64;
                    item.notes        = notes;
                    item.submittedAt  = new SimpleDateFormat("MMM d, yyyy",
                            Locale.getDefault()).format(new Date());
                    updateStats();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Report submitted successfully!",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ── Image pick result ─────────────────────────────────────────────────────

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_RC && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            String encoded = encodeImage(data.getData());
            if (encoded != null) {
                pickedImageBase64 = encoded;
                if (pendingPickButton != null) {
                    pendingPickButton.setText("✅ Image selected");
                    pendingPickButton.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFFE8F5E9));
                    pendingPickButton.setTextColor(0xFF2E7D32);
                }
            }
        }
    }

    private String encodeImage(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return null;
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            if (bmp == null) return null;
            int maxPx = 1024, w = bmp.getWidth(), h = bmp.getHeight();
            if (w > maxPx || h > maxPx) {
                float s = Math.min((float) maxPx / w, (float) maxPx / h);
                bmp = Bitmap.createScaledBitmap(bmp, Math.round(w*s), Math.round(h*s), true);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) { return null; }
    }

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

    static class EventReportItem {
        String eventId, eventTitle, eventDate, reportStatus, submittedAt, imageBase64, notes;
        int    attendees;
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.VH> {

        private final List<EventReportItem> list;
        ReportAdapter(List<EventReportItem> list) { this.list = list; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_report_event, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int position) {
            EventReportItem item = list.get(position);
            h.tvName.setText(item.eventTitle);
            h.tvDate.setText("📅 " + item.eventDate);
            h.tvAttendees.setText("👥 " + item.attendees);
            h.tvStatus.setText(item.reportStatus);

            boolean submitted = "Submitted".equals(item.reportStatus);

            // Status colour
            h.tvStatus.setTextColor(submitted ? 0xFF2E7D32 : 0xFFE65100);
            h.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    submitted ? 0xFFE8F5E9 : 0xFFFFF3E0));

            // Submitted date
            h.tvSubmittedDate.setText(submitted ? item.submittedAt : "—");

            // Action button
            if (submitted) {
                h.btnAction.setText("View Report");
                h.btnAction.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF1565C0));
                h.btnAction.setOnClickListener(v -> showViewReportDialog(item));
            } else {
                h.btnAction.setText("Add Report");
                h.btnAction.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF1565C0));
                h.btnAction.setOnClickListener(v -> showAddReportDialog(item));
            }
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDate, tvAttendees, tvStatus, tvSubmittedDate;
            Button   btnAction;
            VH(View v) {
                super(v);
                tvName          = v.findViewById(R.id.tvReportEventName);
                tvDate          = v.findViewById(R.id.tvReportEventDate);
                tvAttendees     = v.findViewById(R.id.tvReportAttendees);
                tvStatus        = v.findViewById(R.id.tvReportStatus);
                tvSubmittedDate = v.findViewById(R.id.tvReportSubmittedDate);
                btnAction       = v.findViewById(R.id.btnReportAction);
            }
        }
    }
}