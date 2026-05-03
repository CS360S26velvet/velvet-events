package com.lums.eventhub.organizer.reports;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.lums.eventhub.R;

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
 * FULL FLOW (verified):
 *   1. Organizer sees list of their approved/completed events.
 *   2. Each event starts as "Pending" (no report submitted yet) → "Add Report" button shown.
 *   3. Organizer taps "Add Report" → picks image + optional notes → submits.
 *      Firestore: eventReports/{eventId} written with status="Submitted"
 *   4. Admin sees the report on AdminEventReportsActivity → can Approve or Reject.
 *   5a. Admin APPROVES → eventReports/{eventId}.status = "Approved"
 *         Organizer sees: green "✅ Approved" badge + "View Report" button
 *         OrganizerDashboard sees: "Edit Prior Event" button (replaces Approved badge)
 *   5b. Admin REJECTS with reason → eventReports/{eventId}.status = "Rejected"
 *         Organizer sees: red "❌ Rejected" badge + rejection reason + "Add New Report" button
 *         Organizer can tap "Add New Report" to upload a replacement → resets to Submitted
 *
 * Firestore reads:
 *   proposals/  where organizerUsername == mine AND status == "Approved"
 *   events/     where organizerUsername == mine AND status in [Approved, Completed]
 *   eventReports/ where organizerUsername == mine  (to check existing report status)
 *
 * Firestore writes:
 *   eventReports/{eventId}  set/overwrite with full report data
 */
public class EventReportsActivity extends AppCompatActivity {

    private static final int IMAGE_PICK_RC = 500;

    private FirebaseFirestore db;
    private String organizerUsername, societyName;

    private RecyclerView  recyclerView;
    private ReportAdapter adapter;
    private final List<EventReportItem> items = new ArrayList<>();

    // Stats
    private TextView tvTotalEvents, tvSubmitted, tvPending;

    // Pending image pick state
    private String pendingEventId;
    private String pickedImageBase64 = "";
    private Button pendingPickButton;
    private ImageView pendingImgPreview;

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

    // ── Step 1: Load events that need reports ─────────────────────────────────

    private void loadEvents() {
        items.clear();

        // Load from proposals/ (status == Approved)
        db.collection("proposals")
                .whereEqualTo("organizerUsername", organizerUsername)
                .whereEqualTo("status", "Approved")
                .get()
                .addOnSuccessListener(snap -> {
                    for (QueryDocumentSnapshot doc : snap) {
                        items.add(itemFromDoc(doc));
                    }
                    loadCompletedEvents();
                })
                .addOnFailureListener(e -> loadCompletedEvents());
    }

    private void loadCompletedEvents() {
        // Also load from events/ collection (Approved or Completed)
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
                        for (EventReportItem i : items) {
                            if (i.eventId.equals(id)) { exists = true; break; }
                        }
                        if (!exists) items.add(itemFromDoc(doc));
                    }
                    loadReportStatuses();
                })
                .addOnFailureListener(e -> loadReportStatuses());
    }

    private EventReportItem itemFromDoc(QueryDocumentSnapshot doc) {
        EventReportItem item  = new EventReportItem();
        item.eventId          = doc.getId();
        item.eventTitle       = nvl(doc.getString("title"), "Untitled");
        item.eventDate        = nvl(doc.getString("startDate"),
                nvl(doc.getString("date"), "—"));
        Long att = doc.getLong("expectedParticipants");
        item.attendees        = att != null ? att.intValue() : 0;
        item.reportStatus     = "Pending";   // default — overwritten by loadReportStatuses
        item.submittedAt      = "";
        item.imageBase64      = "";
        item.notes            = "";
        item.rejectionReason  = "";
        return item;
    }

    // ── Step 2: Overlay existing report statuses ──────────────────────────────

    private void loadReportStatuses() {
        if (items.isEmpty()) {
            updateStats();
            adapter.notifyDataSetChanged();
            return;
        }

        db.collection("eventReports")
                .whereEqualTo("organizerUsername", organizerUsername)
                .get()
                .addOnSuccessListener(snap -> {
                    // Index by eventId and also by report doc ID (which equals eventId)
                    Map<String, QueryDocumentSnapshot> byId    = new HashMap<>();
                    Map<String, QueryDocumentSnapshot> byTitle = new HashMap<>();

                    for (QueryDocumentSnapshot doc : snap) {
                        String eid    = doc.getString("eventId");
                        String etitle = doc.getString("eventTitle");
                        // Report doc ID is also stored as the eventId
                        byId.put(doc.getId(), doc);
                        if (eid    != null) byId.put(eid, doc);
                        if (etitle != null) byTitle.put(etitle.toLowerCase().trim(), doc);
                    }

                    for (EventReportItem item : items) {
                        QueryDocumentSnapshot matched =
                                byId.containsKey(item.eventId) ? byId.get(item.eventId)
                                        : byTitle.containsKey(item.eventTitle.toLowerCase().trim())
                                        ? byTitle.get(item.eventTitle.toLowerCase().trim()) : null;

                        if (matched != null) {
                            item.reportStatus   = nvl(matched.getString("status"), "Submitted");
                            item.rejectionReason= nvl(matched.getString("rejectionReason"), "");
                            item.imageBase64    = nvl(matched.getString("imageBase64"), "");
                            item.notes          = nvl(matched.getString("notes"), "");
                            Long ts = matched.getLong("submittedAt");
                            if (ts != null) {
                                item.submittedAt = new SimpleDateFormat("MMM d, yyyy",
                                        Locale.getDefault()).format(new Date(ts));
                            }
                        }
                        // If no match → stays "Pending"
                    }

                    updateStats();
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    updateStats();
                    adapter.notifyDataSetChanged();
                });
    }

    private void updateStats() {
        int total    = items.size();
        int approved = 0;
        int pending  = 0;
        for (EventReportItem i : items) {
            if ("Approved".equals(i.reportStatus)) approved++;
            else pending++;
        }
        if (tvTotalEvents != null) tvTotalEvents.setText(String.valueOf(total));
        if (tvSubmitted   != null) tvSubmitted.setText(String.valueOf(approved));
        if (tvPending     != null) tvPending.setText(String.valueOf(pending));
    }

    // ── Add / Re-submit Report dialog ─────────────────────────────────────────

    private void showAddReportDialog(EventReportItem item) {
        pendingEventId    = item.eventId;
        pickedImageBase64 = "";
        pendingPickButton = null;
        pendingImgPreview = null;

        View view = LayoutInflater.from(this)
                .inflate(R.layout.dialog_add_report, null);

        TextView  tvTitle    = view.findViewById(R.id.tvAddReportTitle);
        Button    btnPick    = view.findViewById(R.id.btnPickReportImage);
        ImageView imgPreview = view.findViewById(R.id.imgReportPreview);
        EditText  etNotes    = view.findViewById(R.id.etReportNotes);

        tvTitle.setText(item.eventTitle);
        pendingPickButton = btnPick;
        pendingImgPreview = imgPreview;

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

    /**
     * Writes (or overwrites) eventReports/{eventId}.
     * Always resets status to "Submitted" so admin can review again.
     */
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
        report.put("rejectionReason",   "");          // clear any previous rejection
        report.put("submittedAt",       System.currentTimeMillis());

        // Use eventId as document ID so it's easy to look up from both sides
        db.collection("eventReports").document(item.eventId)
                .set(report)
                .addOnSuccessListener(v -> {
                    item.reportStatus    = "Submitted";
                    item.imageBase64     = imageBase64;
                    item.notes           = notes;
                    item.rejectionReason = "";
                    item.submittedAt     = new SimpleDateFormat("MMM d, yyyy",
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
                // Show preview in dialog
                if (pendingImgPreview != null) {
                    Bitmap bmp = base64ToBitmap(encoded);
                    if (bmp != null) {
                        pendingImgPreview.setImageBitmap(bmp);
                        pendingImgPreview.setVisibility(View.VISIBLE);
                    }
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
                bmp = Bitmap.createScaledBitmap(bmp, Math.round(w * s), Math.round(h * s), true);
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
        String eventId, eventTitle, eventDate;
        String reportStatus;    // "Pending" | "Submitted" | "Approved" | "Rejected"
        String submittedAt, imageBase64, notes, rejectionReason;
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

            // Reset rejection reason visibility
            if (h.tvRejectionReason != null) {
                h.tvRejectionReason.setVisibility(View.GONE);
                h.tvRejectionReason.setText("");
            }

            switch (item.reportStatus) {

                case "Approved":
                    // Admin has approved the report
                    h.tvStatus.setText("✅ Approved");
                    h.tvStatus.setTextColor(0xFF2E7D32);
                    h.tvStatus.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFFE8F5E9));
                    h.tvSubmittedDate.setText(item.submittedAt);
                    h.btnAction.setText("View Report");
                    h.btnAction.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF1565C0));
                    h.btnAction.setOnClickListener(v -> showViewReportDialog(item));
                    break;

                case "Rejected":
                    // Admin rejected — show reason and allow re-upload
                    h.tvStatus.setText("❌ Rejected");
                    h.tvStatus.setTextColor(0xFFC62828);
                    h.tvStatus.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFFFFEBEE));
                    h.tvSubmittedDate.setText(item.submittedAt);
                    if (h.tvRejectionReason != null && !item.rejectionReason.isEmpty()) {
                        h.tvRejectionReason.setVisibility(View.VISIBLE);
                        h.tvRejectionReason.setText("Reason: " + item.rejectionReason);
                    }
                    h.btnAction.setText("Add New Report");
                    h.btnAction.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFFC62828));
                    h.btnAction.setOnClickListener(v -> showAddReportDialog(item));
                    break;

                case "Submitted":
                    // Report sent to admin, awaiting their review
                    h.tvStatus.setText("📤 Submitted");
                    h.tvStatus.setTextColor(0xFF1565C0);
                    h.tvStatus.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFFE8EAF6));
                    h.tvSubmittedDate.setText(item.submittedAt.isEmpty() ? "—" : item.submittedAt);
                    h.btnAction.setText("View Report");
                    h.btnAction.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF1565C0));
                    h.btnAction.setOnClickListener(v -> showViewReportDialog(item));
                    break;

                case "Pending":
                default:
                    // No report submitted yet
                    h.tvStatus.setText("⏳ Pending Report");
                    h.tvStatus.setTextColor(0xFFE65100);
                    h.tvStatus.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFFFFF3E0));
                    h.tvSubmittedDate.setText("—");
                    h.btnAction.setText("Add Report");
                    h.btnAction.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF1565C0));
                    h.btnAction.setOnClickListener(v -> showAddReportDialog(item));
                    break;
            }
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDate, tvAttendees, tvStatus, tvSubmittedDate, tvRejectionReason;
            Button   btnAction;
            VH(View v) {
                super(v);
                tvName            = v.findViewById(R.id.tvReportEventName);
                tvDate            = v.findViewById(R.id.tvReportEventDate);
                tvAttendees       = v.findViewById(R.id.tvReportAttendees);
                tvStatus          = v.findViewById(R.id.tvReportStatus);
                tvSubmittedDate   = v.findViewById(R.id.tvReportSubmittedDate);
                tvRejectionReason = v.findViewById(R.id.tvRejectionReason);
                btnAction         = v.findViewById(R.id.btnReportAction);
            }
        }
    }
}