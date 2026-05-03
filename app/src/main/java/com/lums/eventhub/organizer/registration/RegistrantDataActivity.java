package com.lums.eventhub.organizer.registration;

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
import android.widget.ScrollView;
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
import java.util.Map;

/**
 * RegistrantDataActivity
 *
 * Flow A — launched from dashboard "View Registrants" button:
 *   extras: directEventId + directEventTitle → skip event list, go straight to attendees
 *
 * Flow B — launched from sidebar "Registrant Data":
 *   extras: organizerUsername only → show event list first
 *
 * KEY FIX: Events are loaded from proposals/ (single-field query, no composite index needed).
 *          Registrations are loaded with whereEqualTo("eventId", id) — also single-field.
 *          No full-collection scans. No composite indexes required.
 *
 * Firestore paths written by attendee (EventDetailsActivity):
 *   registrations/{autoId}  fields: eventId, eventTitle, studentName, studentId,
 *                                   userId, paymentStatus, submittedAt, amount,
 *                                   answers{}, paymentProofBase64, accommodationProofBase64
 *   users/{userId}/registrations/{eventId}  (mirror copy)
 */
public class RegistrantDataActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String organizerUsername;
    private boolean directLaunch;

    // Screen 1 — event list
    private LinearLayout llEventListScreen;
    private LinearLayout llEventRows;
    private TextView     tvEventListEmpty;

    // Screen 2 — attendee list
    private LinearLayout llAttendeeScreen;
    private TextView     tvAttendeeEventName;
    private TextView     tvAttendeeEmpty;
    private RecyclerView recyclerAttendees;

    private AttendeeAdapter adapter;
    private final List<RegistrantDoc> attendeeList = new ArrayList<>();
    private String selectedEventId;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrant_data);

        db = FirebaseFirestore.getInstance();
        organizerUsername = getIntent().getStringExtra("organizerUsername");
        if (organizerUsername == null) organizerUsername = "";

        // Screen 1
        llEventListScreen = findViewById(R.id.llEventList);
        llEventRows       = findViewById(R.id.llEventRows);
        tvEventListEmpty  = findViewById(R.id.tvEventListEmpty);

        // Screen 2
        llAttendeeScreen    = findViewById(R.id.llAttendeeScreen);
        tvAttendeeEventName = findViewById(R.id.tvAttendeeEventName);
        tvAttendeeEmpty     = findViewById(R.id.tvAttendeeEmpty);
        recyclerAttendees   = findViewById(R.id.recyclerAttendees);

        adapter = new AttendeeAdapter(attendeeList);
        recyclerAttendees.setLayoutManager(new LinearLayoutManager(this));
        recyclerAttendees.setAdapter(adapter);

        findViewById(R.id.btnRegistrantDataBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAttendeeBack).setOnClickListener(v -> {
            if (directLaunch) finish();
            else showEventListScreen();
        });

        String directEventId    = getIntent().getStringExtra("directEventId");
        String directEventTitle = getIntent().getStringExtra("directEventTitle");

        if (directEventId != null && !directEventId.isEmpty()) {
            directLaunch = true;
            showAttendeeScreen(directEventId,
                    directEventTitle != null ? directEventTitle : "Event");
        } else {
            directLaunch = false;
            showEventListScreen();
            loadEvents();
        }
    }

    // ── Screen switching ──────────────────────────────────────────────────────

    private void showEventListScreen() {
        llEventListScreen.setVisibility(View.VISIBLE);
        llAttendeeScreen.setVisibility(View.GONE);
    }

    private void showAttendeeScreen(String eventId, String eventTitle) {
        selectedEventId = eventId;
        llEventListScreen.setVisibility(View.GONE);
        llAttendeeScreen.setVisibility(View.VISIBLE);
        tvAttendeeEventName.setText(eventTitle);
        attendeeList.clear();
        adapter.notifyDataSetChanged();
        tvAttendeeEmpty.setText("Loading registrants…");
        tvAttendeeEmpty.setVisibility(View.VISIBLE);
        loadRegistrants(eventId, eventTitle);
    }

    // ── Load organizer's events ───────────────────────────────────────────────
    // Uses single-field queries only — no composite index required.

    private void loadEvents() {
        tvEventListEmpty.setText("Loading events…");
        tvEventListEmpty.setVisibility(View.VISIBLE);

        final java.util.LinkedHashMap<String, String[]> found = new java.util.LinkedHashMap<>();

        // Query proposals/ by organizerUsername (single field — always works)
        db.collection("proposals")
                .whereEqualTo("organizerUsername", organizerUsername)
                .get()
                .addOnSuccessListener(pSnap -> {
                    for (QueryDocumentSnapshot doc : pSnap) {
                        String status = doc.getString("status");
                        if (!"Approved".equals(status)) continue; // only show approved
                        String title = nvl(doc.getString("title"), "Untitled");
                        String date  = nvl(doc.getString("startDate"),
                                nvl(doc.getString("date"), "—"));
                        found.put(doc.getId(), new String[]{title, date});
                    }
                    // Also check events/ collection (single-field query)
                    db.collection("events")
                            .whereEqualTo("organizerUsername", organizerUsername)
                            .get()
                            .addOnSuccessListener(eSnap -> {
                                for (QueryDocumentSnapshot doc : eSnap) {
                                    if (found.containsKey(doc.getId())) continue; // no duplicate
                                    String title = nvl(doc.getString("title"), "Untitled");
                                    String date  = nvl(doc.getString("startDate"),
                                            nvl(doc.getString("date"), "—"));
                                    found.put(doc.getId(), new String[]{title, date});
                                }
                                renderEventList(found);
                            })
                            .addOnFailureListener(e -> renderEventList(found));
                })
                .addOnFailureListener(e -> {
                    tvEventListEmpty.setText("Failed to load events: " + e.getMessage());
                    tvEventListEmpty.setVisibility(View.VISIBLE);
                });
    }

    private void renderEventList(java.util.LinkedHashMap<String, String[]> found) {
        llEventRows.removeAllViews();
        if (found.isEmpty()) {
            tvEventListEmpty.setText("No approved events found.\n\nMake sure your event proposal has been approved by admin.");
            tvEventListEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvEventListEmpty.setVisibility(View.GONE);
        for (Map.Entry<String, String[]> e : found.entrySet()) {
            llEventRows.addView(buildEventRow(e.getKey(), e.getValue()[0], e.getValue()[1]));
        }
    }

    private View buildEventRow(String eventId, String title, String date) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(0xFFFFFFFF);
        row.setPadding(20, 20, 20, 20);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 0, 0, 2);
        row.setLayoutParams(rp);
        row.setElevation(2f);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(15f);
        tvTitle.setTextColor(0xFF1A1A2E);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvDate = new TextView(this);
        tvDate.setText("📅 " + date);
        tvDate.setTextSize(12f);
        tvDate.setTextColor(0xFF666666);

        info.addView(tvTitle);
        info.addView(tvDate);

        Button btn = new Button(this);
        btn.setText("View Registrants");
        btn.setTextSize(12f);
        btn.setTextColor(0xFFFFFFFF);
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1565C0));
        btn.setOnClickListener(v -> showAttendeeScreen(eventId, title));

        row.addView(info);
        row.addView(btn);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(0xFFEEEEEE);

        wrapper.addView(row);
        wrapper.addView(divider);
        return wrapper;
    }

    // ── Load registrants ──────────────────────────────────────────────────────
    // Single-field query: whereEqualTo("eventId", id) — no index needed.

    private void loadRegistrants(String eventId, String eventTitle) {
        db.collection("registrations")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snap -> {
                    attendeeList.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        attendeeList.add(toDoc(doc));
                    }
                    if (!attendeeList.isEmpty()) {
                        tvAttendeeEmpty.setVisibility(View.GONE);
                        adapter.notifyDataSetChanged();
                    } else {
                        // Fallback: match by eventTitle (handles edge case where eventId differs)
                        loadRegistrantsByTitle(eventTitle);
                    }
                })
                .addOnFailureListener(e -> {
                    // Query failed — show the error so we can debug
                    tvAttendeeEmpty.setText("Query failed: " + e.getMessage()
                            + "\n\nEventId: " + eventId);
                    tvAttendeeEmpty.setVisibility(View.VISIBLE);
                });
    }

    private void loadRegistrantsByTitle(String eventTitle) {
        if (eventTitle == null || eventTitle.isEmpty()) {
            tvAttendeeEmpty.setText("No registrants found for this event yet.");
            tvAttendeeEmpty.setVisibility(View.VISIBLE);
            return;
        }
        db.collection("registrations")
                .whereEqualTo("eventTitle", eventTitle)
                .get()
                .addOnSuccessListener(snap -> {
                    attendeeList.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        attendeeList.add(toDoc(doc));
                    }
                    if (attendeeList.isEmpty()) {
                        tvAttendeeEmpty.setText(
                                "No registrants yet for \"" + eventTitle + "\".\n\n"
                                        + "Once attendees fill the registration form and submit payment, "
                                        + "they will appear here.");
                        tvAttendeeEmpty.setVisibility(View.VISIBLE);
                    } else {
                        tvAttendeeEmpty.setVisibility(View.GONE);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    tvAttendeeEmpty.setText("Error: " + e.getMessage());
                    tvAttendeeEmpty.setVisibility(View.VISIBLE);
                });
    }

    // ── Firestore doc → model ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private RegistrantDoc toDoc(QueryDocumentSnapshot doc) {
        RegistrantDoc r   = new RegistrantDoc();
        r.docId           = doc.getId();
        r.eventId         = nvl(doc.getString("eventId"),
                selectedEventId != null ? selectedEventId : "");
        r.studentName     = nvl(doc.getString("studentName"), "Unknown");
        r.studentId       = nvl(doc.getString("studentId"), "—");
        r.paymentStatus   = nvl(doc.getString("paymentStatus"), "Pending");
        r.amount          = nvl(doc.getString("amount"), "—");
        r.proofBase64     = nvl(doc.getString("paymentProofBase64"), "");
        r.accomProof      = nvl(doc.getString("accommodationProofBase64"), "");
        r.rejectionReason = nvl(doc.getString("rejectionReason"), "");

        Object ts = doc.get("submittedAt");
        if (ts instanceof Long) {
            r.submittedAt = new SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault())
                    .format(new Date((Long) ts));
        } else {
            r.submittedAt = "—";
        }

        Object ans = doc.get("answers");
        if (ans instanceof Map) r.answers = (Map<String, Object>) ans;
        return r;
    }

    // ── Detail dialog — shows every form field the attendee filled ────────────

    private void showDetail(RegistrantDoc r) {
        // Load form question order so we display answers in the right sequence
        db.collection("proposals").document(r.eventId)
                .collection("formQuestions").orderBy("order").get()
                .addOnSuccessListener(snap -> {
                    List<String> labels = new ArrayList<>();
                    for (QueryDocumentSnapshot q : snap) {
                        String lbl = q.getString("label");
                        if (lbl != null && !lbl.isEmpty()) labels.add(lbl);
                    }
                    buildDetailDialog(r, labels);
                })
                .addOnFailureListener(e -> buildDetailDialog(r, new ArrayList<>()));
    }

    @SuppressWarnings("unchecked")
    private void buildDetailDialog(RegistrantDoc r, List<String> orderedLabels) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        root.setBackgroundColor(0xFFFFFFFF);
        scroll.addView(root);

        // ── Header ─────────────────────────────────────────────────────────────
        addBigText(root, r.studentName);
        addLabelValue(root, "STUDENT ID",   r.studentId);
        addLabelValue(root, "SUBMITTED AT", r.submittedAt);
        addLabelValue(root, "AMOUNT",       r.amount);

        // Payment status badge
        TextView badge = new TextView(this);
        switch (r.paymentStatus) {
            case "Approved":
                badge.setText("✅  Payment Approved");
                badge.setTextColor(0xFF2E7D32);
                badge.setBackgroundColor(0xFFE8F5E9);
                break;
            case "Rejected":
                badge.setText("❌  Payment Rejected");
                badge.setTextColor(0xFFB71C1C);
                badge.setBackgroundColor(0xFFFFEBEE);
                break;
            default:
                badge.setText("⏳  Awaiting Payment Verification");
                badge.setTextColor(0xFFE65100);
                badge.setBackgroundColor(0xFFFFF3E0);
                break;
        }
        badge.setTextSize(13f);
        badge.setTypeface(null, android.graphics.Typeface.BOLD);
        badge.setPadding(16, 8, 16, 8);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.setMargins(0, 8, 0, 4);
        badge.setLayoutParams(bp);
        root.addView(badge);

        if (!r.rejectionReason.isEmpty() && "Rejected".equals(r.paymentStatus)) {
            addLabelValue(root, "REJECTION REASON", r.rejectionReason);
        }

        // ── Form Answers ───────────────────────────────────────────────────────
        addDivider(root);
        addSectionHeader(root, "📋  Registration Form Responses");

        if (r.answers != null && !r.answers.isEmpty()) {
            java.util.Set<String> rendered = new java.util.LinkedHashSet<>();
            // Render in form order
            for (String lbl : orderedLabels) {
                if (r.answers.containsKey(lbl)) {
                    Object val = r.answers.get(lbl);
                    addLabelValue(root, lbl, val != null ? val.toString() : "—");
                    rendered.add(lbl);
                }
            }
            // Any extra answers not in the schema (e.g. wantsAccommodation)
            for (Map.Entry<String, Object> entry : r.answers.entrySet()) {
                if (!rendered.contains(entry.getKey())) {
                    Object val = entry.getValue();
                    addLabelValue(root, entry.getKey(), val != null ? val.toString() : "—");
                }
            }
        } else {
            TextView none = new TextView(this);
            none.setText("No form answers recorded.");
            none.setTextColor(0xFF999999);
            none.setTextSize(13f);
            none.setPadding(0, 8, 0, 8);
            root.addView(none);
        }

        // ── Payment Proof ──────────────────────────────────────────────────────
        if (!r.proofBase64.isEmpty()) {
            addDivider(root);
            addSectionHeader(root, "🧾  Payment Proof");
            Bitmap bmp = decodeB64(r.proofBase64);
            if (bmp != null) root.addView(makeImg(bmp));
        }

        // ── Accommodation Proof ────────────────────────────────────────────────
        if (!r.accomProof.isEmpty()) {
            addDivider(root);
            addSectionHeader(root, "🏠  Accommodation Proof");
            Bitmap bmp2 = decodeB64(r.accomProof);
            if (bmp2 != null) root.addView(makeImg(bmp2));
        }

        new AlertDialog.Builder(this)
                .setView(scroll)
                .setNegativeButton("Close", null)
                .show();
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void addBigText(LinearLayout p, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(20f);
        tv.setTextColor(0xFF1A1A2E);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 12);
        tv.setLayoutParams(lp);
        p.addView(tv);
    }

    private void addSectionHeader(LinearLayout p, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15f);
        tv.setTextColor(0xFF1565C0);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 8, 0, 8);
        tv.setLayoutParams(lp);
        p.addView(tv);
    }

    private void addLabelValue(LinearLayout p, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 6, 0, 6);
        row.setLayoutParams(rp);

        TextView lbl = new TextView(this);
        lbl.setText(label.toUpperCase());
        lbl.setTextSize(11f);
        lbl.setTextColor(0xFF888888);
        lbl.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(lbl);

        TextView val = new TextView(this);
        val.setText((value != null && !value.isEmpty()) ? value : "—");
        val.setTextSize(14f);
        val.setTextColor(0xFF1A1A2E);
        val.setBackgroundColor(0xFFF5F5F5);
        val.setPadding(12, 8, 12, 8);
        row.addView(val);

        p.addView(row);
    }

    private void addDivider(LinearLayout p) {
        View div = new View(this);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        dp.setMargins(0, 16, 0, 16);
        div.setLayoutParams(dp);
        div.setBackgroundColor(0xFFE0E0E0);
        p.addView(div);
    }

    private ImageView makeImg(Bitmap bmp) {
        ImageView iv = new ImageView(this);
        iv.setImageBitmap(bmp);
        iv.setAdjustViewBounds(true);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ip.setMargins(0, 8, 0, 8);
        iv.setLayoutParams(ip);
        return iv;
    }

    private Bitmap decodeB64(String b64) {
        try {
            byte[] bytes = Base64.decode(b64, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) { return null; }
    }

    private String nvl(String s, String fallback) {
        return (s != null && !s.isEmpty()) ? s : fallback;
    }

    // ── Model ─────────────────────────────────────────────────────────────────

    static class RegistrantDoc {
        String docId, eventId;
        String studentName, studentId, paymentStatus;
        String amount, proofBase64, accomProof, submittedAt, rejectionReason;
        Map<String, Object> answers;
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    class AttendeeAdapter extends RecyclerView.Adapter<AttendeeAdapter.VH> {
        private final List<RegistrantDoc> list;
        AttendeeAdapter(List<RegistrantDoc> list) { this.list = list; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_attendee_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            RegistrantDoc r = list.get(pos);
            h.tvName.setText(r.studentName);
            h.tvId.setText(r.studentId);
            h.tvStatus.setText(r.paymentStatus);
            switch (r.paymentStatus) {
                case "Approved": h.tvStatus.setTextColor(0xFF2E7D32); break;
                case "Rejected": h.tvStatus.setTextColor(0xFFB71C1C); break;
                default:         h.tvStatus.setTextColor(0xFFE65100); break;
            }
            h.btnView.setOnClickListener(v -> showDetail(r));
            h.itemView.setOnClickListener(v -> showDetail(r));
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvId, tvStatus;
            Button   btnView;
            VH(View v) {
                super(v);
                tvName   = v.findViewById(R.id.tvAttendeeName);
                tvId     = v.findViewById(R.id.tvAttendeeStudentId);
                tvStatus = v.findViewById(R.id.tvAttendeeStatus);
                btnView  = v.findViewById(R.id.btnViewAttendee);
            }
        }
    }
}