package com.lums.eventhub.organizer.checkin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CheckInParticipantsActivity.java
 *
 * Shows ONLY participants whose paymentStatus == "Approved"
 * for the selected event. Organiser can mark them as checked in.
 *
 * Stats: Total Approved / Checked In / Remaining + progress bar.
 * Search: by name or student ID.
 *
 * Receives from CheckInActivity:
 *   "eventId"    — Firestore document ID
 *   "eventTitle" — display name
 *   "eventVenue" — venue string
 */
public class CheckInParticipantsActivity extends AppCompatActivity {

    private RecyclerView      recyclerView;
    private EditText          etSearch;
    private TextView          tvTitle, tvSubtitle;
    private TextView          tvTotalRegistered, tvCheckedIn, tvRemaining, tvProgress;
    private ProgressBar       progressBar;
    private AttendeeAdapter   adapter;

    private final List<Attendee> allList      = new ArrayList<>();
    private final List<Attendee> filteredList = new ArrayList<>();
    private int checkedInCount = 0;

    private String eventId    = "";
    private String eventTitle = "Event";
    private String eventVenue = "";

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in_participants);

        db = FirebaseFirestore.getInstance();

        eventId    = getIntent().getStringExtra("eventId");
        eventTitle = getIntent().getStringExtra("eventTitle");
        eventVenue = getIntent().getStringExtra("eventVenue");
        if (eventId    == null) eventId    = "";
        if (eventTitle == null) eventTitle = "Event";
        if (eventVenue == null) eventVenue = "";

        bindViews();

        tvTitle.setText("Check-In — " + eventTitle);
        tvSubtitle.setText(eventVenue.isEmpty() ? "Approved participants" : eventVenue);

        adapter = new AttendeeAdapter(filteredList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString().trim());
            }
        });

        findViewById(R.id.btnParticipantsBack).setOnClickListener(v -> finish());

        loadApprovedParticipants();
    }

    // ── Bind views ─────────────────────────────────────────────────────────────

    private void bindViews() {
        tvTitle           = findViewById(R.id.tvParticipantsTitle);
        tvSubtitle        = findViewById(R.id.tvParticipantsSubtitle);
        tvTotalRegistered = findViewById(R.id.tvTotalRegistered);
        tvCheckedIn       = findViewById(R.id.tvCheckedIn);
        tvRemaining       = findViewById(R.id.tvRemaining);
        tvProgress        = findViewById(R.id.tvCheckInProgress);
        progressBar       = findViewById(R.id.progressCheckIn);
        etSearch          = findViewById(R.id.etSearch);
        recyclerView      = findViewById(R.id.recyclerView);
    }

    // ── Load ONLY Approved registrations for this event ───────────────────────

    private void loadApprovedParticipants() {
        db.collection("registrations")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("paymentStatus", "Approved")
                .get()
                .addOnSuccessListener(snap -> {
                    allList.clear();
                    checkedInCount = 0;

                    for (QueryDocumentSnapshot doc : snap) {
                        Attendee a    = new Attendee();
                        a.id          = doc.getId();
                        a.name        = nvl(doc.getString("studentName"), "Unknown");
                        a.studentId   = nvl(doc.getString("studentId"),   "—");
                        Boolean ci    = doc.getBoolean("checkedIn");
                        a.checkedIn   = Boolean.TRUE.equals(ci);
                        Long ts       = doc.getLong("checkedInAt");
                        a.checkedInAt = ts != null
                                ? new SimpleDateFormat("hh:mm a", Locale.getDefault())
                                .format(new Date(ts))
                                : "";
                        if (a.checkedIn) checkedInCount++;
                        allList.add(a);
                    }

                    filteredList.clear();
                    filteredList.addAll(allList);
                    updateStats();
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load participants: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ── Search filter ──────────────────────────────────────────────────────────

    private void filterList(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(allList);
        } else {
            String q = query.toLowerCase();
            for (Attendee a : allList) {
                if (a.name.toLowerCase().contains(q)
                        || a.studentId.toLowerCase().contains(q)) {
                    filteredList.add(a);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // ── Stats ──────────────────────────────────────────────────────────────────

    private void updateStats() {
        int total     = allList.size();
        int remaining = total - checkedInCount;
        int pct       = total > 0 ? (checkedInCount * 100) / total : 0;

        tvTotalRegistered.setText(String.valueOf(total));
        tvCheckedIn.setText(String.valueOf(checkedInCount));
        tvRemaining.setText(String.valueOf(remaining));
        tvProgress.setText(pct + "% checked in");
        progressBar.setProgress(pct);
    }

    // ── Check in a single attendee ─────────────────────────────────────────────

    private void checkInAttendee(Attendee a, int position) {
        long now = System.currentTimeMillis();
        Map<String, Object> update = new HashMap<>();
        update.put("checkedIn",   true);
        update.put("checkedInAt", now);

        db.collection("registrations").document(a.id)
                .update(update)
                .addOnSuccessListener(v -> {
                    a.checkedIn   = true;
                    a.checkedInAt = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                            .format(new Date(now));
                    checkedInCount++;
                    updateStats();
                    adapter.notifyItemChanged(position);
                    Toast.makeText(this,
                            a.name + " checked in ✓", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String nvl(String s, String fallback) {
        return (s != null && !s.isEmpty()) ? s : fallback;
    }

    // ── Model ──────────────────────────────────────────────────────────────────

    static class Attendee {
        String  id, name, studentId, checkedInAt;
        boolean checkedIn;
    }

    // ── Adapter ────────────────────────────────────────────────────────────────

    class AttendeeAdapter extends RecyclerView.Adapter<AttendeeAdapter.VH> {

        private final List<Attendee> list;
        AttendeeAdapter(List<Attendee> list) { this.list = list; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_attendee, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int position) {
            Attendee a = list.get(position);

            h.tvName.setText(a.name);
            h.tvStudentId.setText(a.studentId);

            if (a.checkedIn) {
                h.tvCheckInStatus.setText("● Checked In  " + a.checkedInAt);
                h.tvCheckInStatus.setTextColor(0xFF2E7D32);
                h.btnCheckIn.setText("Checked In ✓");
                h.btnCheckIn.setEnabled(false);
                h.btnCheckIn.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF9E9E9E));
            } else {
                h.tvCheckInStatus.setText("Not yet checked in");
                h.tvCheckInStatus.setTextColor(0xFF888888);
                h.btnCheckIn.setText("Check In");
                h.btnCheckIn.setEnabled(true);
                h.btnCheckIn.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF1565C0));
                h.btnCheckIn.setOnClickListener(v -> {
                    int pos = h.getAdapterPosition();
                    if (pos != RecyclerView.NO_ID) checkInAttendee(a, pos);
                });
            }
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvStudentId, tvCheckInStatus;
            Button   btnCheckIn;
            VH(View v) {
                super(v);
                tvName          = v.findViewById(R.id.tvName);
                tvStudentId     = v.findViewById(R.id.tvStudentId);
                tvCheckInStatus = v.findViewById(R.id.tvCheckInStatus);
                btnCheckIn      = v.findViewById(R.id.btnCheckIn);
            }
        }
    }
}