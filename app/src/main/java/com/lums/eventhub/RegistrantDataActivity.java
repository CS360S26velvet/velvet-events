package com.lums.eventhub;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RegistrantDataActivity.java
 *
 * Organizer sees:
 *   Screen 1 — List of their approved events (from events/ collection)
 *   Screen 2 — Tap an event → list of all attendees who registered
 *              (reads registrations/ where eventId == selected AND paymentStatus == Approved)
 *   Screen 3 — Tap an attendee → full read-only registration form dialog
 *              (shows all answers they filled + payment proof image)
 *
 * Firestore reads:
 *   events/          where organizerUsername == mine AND status == Approved
 *   registrations/   where eventId == selectedEventId
 */
public class RegistrantDataActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String organizerUsername, societyName;

    // Views — event list screen
    private LinearLayout llEventList;
    private TextView     tvEventListEmpty;
    private TextView     tvEventListHeader;

    // Views — attendee list screen
    private LinearLayout llAttendeeScreen;
    private TextView     tvAttendeeHeader;
    private RecyclerView recyclerAttendees;
    private TextView     tvAttendeeEmpty;
    private AttendeeAdapter attendeeAdapter;
    private final List<RegistrantDoc> attendeeList = new ArrayList<>();

    private String selectedEventId, selectedEventTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrant_data);

        db = FirebaseFirestore.getInstance();
        organizerUsername = getIntent().getStringExtra("organizerUsername");
        societyName       = getIntent().getStringExtra("societyName");
        if (organizerUsername == null) organizerUsername = "";

        // Event list screen
        llEventList       = findViewById(R.id.llEventRows);
        tvEventListEmpty  = findViewById(R.id.tvEventListEmpty);
        tvEventListHeader = findViewById(R.id.tvEventListHeader);

        // Attendee screen
        llAttendeeScreen  = findViewById(R.id.llAttendeeScreen);
        tvAttendeeHeader  = findViewById(R.id.tvAttendeeEventName);
        tvAttendeeEmpty   = findViewById(R.id.tvAttendeeEmpty);
        recyclerAttendees = findViewById(R.id.recyclerAttendees);

        attendeeAdapter = new AttendeeAdapter(attendeeList);
        recyclerAttendees.setLayoutManager(new LinearLayoutManager(this));
        recyclerAttendees.setAdapter(attendeeAdapter);

        // Back buttons
        findViewById(R.id.btnRegistrantDataBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAttendeeBack).setOnClickListener(v -> showEventListScreen());

        showEventListScreen();
        loadApprovedEvents();
    }

    // ── Screen switching ──────────────────────────────────────────────────────

    private void showEventListScreen() {
        llEventList.setVisibility(View.VISIBLE);
        llAttendeeScreen.setVisibility(View.GONE);
    }

    private void showAttendeeScreen(String eventId, String eventTitle) {
        selectedEventId    = eventId;
        selectedEventTitle = eventTitle;
        llEventList.setVisibility(View.GONE);
        llAttendeeScreen.setVisibility(View.VISIBLE);
        tvAttendeeHeader.setText(eventTitle);
        loadAttendees(eventId);
    }

    // ── Load approved events ──────────────────────────────────────────────────

    private void loadApprovedEvents() {
        db.collection("events")
                .whereEqualTo("organizerUsername", organizerUsername)
                .whereEqualTo("status", "Approved")
                .get()
                .addOnSuccessListener(snap -> {
                    llEventList.removeAllViews();
                    if (snap.isEmpty()) {
                        tvEventListEmpty.setVisibility(View.VISIBLE);
                        return;
                    }
                    tvEventListEmpty.setVisibility(View.GONE);
                    for (QueryDocumentSnapshot doc : snap) {
                        String eid    = doc.getId();
                        String title  = nvl(doc.getString("title"), "Untitled");
                        String date   = nvl(doc.getString("startDate"),
                                nvl(doc.getString("date"), "—"));
                        llEventList.addView(buildEventRow(eid, title, date));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load events: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private View buildEventRow(String eventId, String title, String date) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(0xFFFFFFFF);
        row.setPadding(20, 20, 20, 20);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 0, 0, 8);
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

        Button btnView = new Button(this);
        btnView.setText("View Registrants");
        btnView.setTextSize(12f);
        btnView.setTextColor(0xFFFFFFFF);
        btnView.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF1565C0));
        btnView.setOnClickListener(v -> showAttendeeScreen(eventId, title));

        row.addView(info);
        row.addView(btnView);

        // Divider
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(0xFFEEEEEE);
        wrapper.addView(row);
        wrapper.addView(divider);
        return wrapper;
    }

    // ── Load attendees for selected event ─────────────────────────────────────

    private void loadAttendees(String eventId) {
        attendeeList.clear();
        attendeeAdapter.notifyDataSetChanged();

        db.collection("registrations")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(snap -> {
                    attendeeList.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        RegistrantDoc r  = new RegistrantDoc();
                        r.docId          = doc.getId();
                        r.studentName    = nvl(doc.getString("studentName"), "Unknown");
                        r.studentId      = nvl(doc.getString("studentId"), "—");
                        r.paymentStatus  = nvl(doc.getString("paymentStatus"), "Pending");
                        r.amount         = nvl(doc.getString("amount"), "—");
                        r.proofBase64    = nvl(doc.getString("paymentProofBase64"), "");
                        r.rejectionReason= nvl(doc.getString("rejectionReason"), "");
                        Object ts = doc.get("submittedAt");
                        if (ts instanceof Long) {
                            r.submittedAt = new java.text.SimpleDateFormat("MMM d, yyyy",
                                    java.util.Locale.getDefault()).format(new java.util.Date((Long) ts));
                        } else {
                            r.submittedAt = "—";
                        }
                        // Answers map (full form responses)
                        Object ans = doc.get("answers");
                        if (ans instanceof Map) {
                            r.answers = (Map<String, Object>) ans;
                        }
                        attendeeList.add(r);
                    }

                    if (attendeeList.isEmpty()) {
                        tvAttendeeEmpty.setVisibility(View.VISIBLE);
                    } else {
                        tvAttendeeEmpty.setVisibility(View.GONE);
                    }
                    attendeeAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load registrants: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ── Show full read-only registration form dialog ───────────────────────────

    @SuppressWarnings("unchecked")
    private void showRegistrantDetail(RegistrantDoc r) {
        // Build scrollable dialog content programmatically
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        root.setBackgroundColor(0xFFFFFFFF);
        scroll.addView(root);

        // Header
        addSection(root, r.studentName, 20f, 0xFF1A1A2E, true);
        addField(root, "Student ID", r.studentId);
        addField(root, "Submitted", r.submittedAt);
        addField(root, "Amount", r.amount);

        // Payment status badge
        TextView tvStatus = new TextView(this);
        switch (r.paymentStatus) {
            case "Approved":
                tvStatus.setText("✅ Payment Approved");
                tvStatus.setTextColor(0xFF2E7D32);
                tvStatus.setBackgroundColor(0xFFE8F5E9);
                break;
            case "Rejected":
                tvStatus.setText("❌ Payment Rejected");
                tvStatus.setTextColor(0xFFB71C1C);
                tvStatus.setBackgroundColor(0xFFFFEBEE);
                break;
            default:
                tvStatus.setText("⏳ Awaiting Verification");
                tvStatus.setTextColor(0xFFE65100);
                tvStatus.setBackgroundColor(0xFFFFF3E0);
                break;
        }
        tvStatus.setTextSize(13f);
        tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
        tvStatus.setPadding(16, 8, 16, 8);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.setMargins(0, 8, 0, 16);
        tvStatus.setLayoutParams(sp);
        root.addView(tvStatus);

        if (!r.rejectionReason.isEmpty()) {
            addField(root, "Rejection Reason", r.rejectionReason);
        }

        // Divider
        addDivider(root);

        // Registration form answers
        addSection(root, "Registration Form Responses", 15f, 0xFF1565C0, true);

        if (r.answers != null && !r.answers.isEmpty()) {
            for (Map.Entry<String, Object> entry : r.answers.entrySet()) {
                String key   = entry.getKey();
                String value = entry.getValue() != null ? entry.getValue().toString() : "—";
                addField(root, key, value);
            }
        } else {
            TextView noAnswers = new TextView(this);
            noAnswers.setText("No form responses recorded.");
            noAnswers.setTextColor(0xFF999999);
            noAnswers.setTextSize(13f);
            root.addView(noAnswers);
        }

        // Payment proof image
        if (!r.proofBase64.isEmpty()) {
            addDivider(root);
            addSection(root, "Payment Proof", 15f, 0xFF1565C0, true);
            Bitmap bmp = bitmapFromBase64(r.proofBase64);
            if (bmp != null) {
                ImageView img = new ImageView(this);
                img.setImageBitmap(bmp);
                img.setAdjustViewBounds(true);
                LinearLayout.LayoutParams imgP = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                imgP.setMargins(0, 8, 0, 8);
                img.setLayoutParams(imgP);
                root.addView(img);
            }
        }

        new AlertDialog.Builder(this)
                .setView(scroll)
                .setNegativeButton("Close", null)
                .show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void addSection(LinearLayout parent, String text, float size,
                            int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(size);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 8, 0, 8);
        tv.setLayoutParams(p);
        parent.addView(tv);
    }

    private void addField(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 4, 0, 4);
        row.setLayoutParams(rp);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(11f);
        tvLabel.setTextColor(0xFF999999);
        tvLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        tvLabel.setAllCaps(true);
        row.addView(tvLabel);

        TextView tvValue = new TextView(this);
        tvValue.setText(value != null && !value.isEmpty() ? value : "—");
        tvValue.setTextSize(14f);
        tvValue.setTextColor(0xFF1A1A2E);
        tvValue.setBackgroundColor(0xFFF5F5F5);
        tvValue.setPadding(12, 8, 12, 8);
        row.addView(tvValue);

        parent.addView(row);
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(this);
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        dp.setMargins(0, 16, 0, 16);
        divider.setLayoutParams(dp);
        divider.setBackgroundColor(0xFFEEEEEE);
        parent.addView(divider);
    }

    private Bitmap bitmapFromBase64(String b64) {
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
        String docId, studentName, studentId, paymentStatus;
        String amount, proofBase64, submittedAt, rejectionReason;
        Map<String, Object> answers;
    }

    // ── Attendee Adapter ──────────────────────────────────────────────────────

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
        public void onBindViewHolder(VH h, int position) {
            RegistrantDoc r = list.get(position);
            h.tvName.setText(r.studentName);
            h.tvStudentId.setText(r.studentId);
            h.tvStatus.setText(r.paymentStatus);
            switch (r.paymentStatus) {
                case "Approved":
                    h.tvStatus.setTextColor(0xFF2E7D32); break;
                case "Rejected":
                    h.tvStatus.setTextColor(0xFFB71C1C); break;
                default:
                    h.tvStatus.setTextColor(0xFFE65100); break;
            }
            h.btnView.setOnClickListener(v -> showRegistrantDetail(r));
            h.itemView.setOnClickListener(v -> showRegistrantDetail(r));
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvStudentId, tvStatus;
            Button   btnView;
            VH(View v) {
                super(v);
                tvName      = v.findViewById(R.id.tvAttendeeName);
                tvStudentId = v.findViewById(R.id.tvAttendeeStudentId);
                tvStatus    = v.findViewById(R.id.tvAttendeeStatus);
                btnView     = v.findViewById(R.id.btnViewAttendee);
            }
        }
    }
}