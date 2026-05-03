package com.lums.eventhub;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * EventVisibilityActivity.java
 *
 * Organizer sees a list of all their events (approved) and can toggle
 * "Remove from Attendee Dashboard" per event.
 *
 * When toggled ON (removed):  events/{id}.hiddenFromAttendees = true
 *   → event disappears from EventBrowsingActivity on attendee side
 * When toggled OFF (visible): events/{id}.hiddenFromAttendees = false
 *   → event reappears on attendee side
 *
 * Firestore:
 *   reads/writes: events/ where organizerUsername == mine
 */
public class EventVisibilityActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String organizerUsername;

    private RecyclerView recycler;
    private EventVisibilityAdapter adapter;
    private final List<EventVisibilityItem> items = new ArrayList<>();
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_visibility);

        db = FirebaseFirestore.getInstance();
        organizerUsername = getIntent().getStringExtra("organizerUsername");
        if (organizerUsername == null) organizerUsername = "";

        tvEmpty  = findViewById(R.id.tvVisibilityEmpty);
        recycler = findViewById(R.id.recyclerVisibility);
        adapter  = new EventVisibilityAdapter(items);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        findViewById(R.id.btnVisibilityBack).setOnClickListener(v -> finish());

        loadEvents();
    }

    private void loadEvents() {
        db.collection("events")
                .whereEqualTo("organizerUsername", organizerUsername)
                .get()
                .addOnSuccessListener(snap -> {
                    items.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        EventVisibilityItem item = new EventVisibilityItem();
                        item.eventId   = doc.getId();
                        item.title     = nvl(doc.getString("title"), "Untitled");
                        item.date      = nvl(doc.getString("startDate"),
                                nvl(doc.getString("date"), "—"));
                        item.status    = nvl(doc.getString("status"), "Approved");
                        Boolean hidden = doc.getBoolean("hiddenFromAttendees");
                        item.hidden    = Boolean.TRUE.equals(hidden);
                        items.add(item);
                    }
                    if (items.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load events: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void setVisibility(EventVisibilityItem item, boolean hidden) {
        db.collection("events").document(item.eventId)
                .update("hiddenFromAttendees", hidden)
                .addOnSuccessListener(unused -> {
                    item.hidden = hidden;
                    String msg = hidden
                            ? item.title + " removed from attendee dashboard"
                            : item.title + " visible to attendees";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Revert toggle on failure
                    item.hidden = !hidden;
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private String nvl(String s, String fallback) {
        return (s != null && !s.isEmpty()) ? s : fallback;
    }

    // ── Model ─────────────────────────────────────────────────────────────────

    static class EventVisibilityItem {
        String  eventId, title, date, status;
        boolean hidden;
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    class EventVisibilityAdapter
            extends RecyclerView.Adapter<EventVisibilityAdapter.VH> {

        private final List<EventVisibilityItem> list;
        EventVisibilityAdapter(List<EventVisibilityItem> list) { this.list = list; }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            // Build row programmatically — no extra layout file needed
            LinearLayout row = new LinearLayout(EventVisibilityActivity.this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setBackgroundColor(0xFFFFFFFF);
            row.setPadding(24, 20, 24, 20);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rp.setMargins(0, 0, 0, 8);
            row.setLayoutParams(rp);
            row.setElevation(2f);
            row.setTag("row");

            // Top row: title + switch
            LinearLayout topRow = new LinearLayout(EventVisibilityActivity.this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            topRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            TextView tvTitle = new TextView(EventVisibilityActivity.this);
            tvTitle.setTextSize(15f);
            tvTitle.setTextColor(0xFF1A1A2E);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tvTitle.setTag("tvTitle");

            Switch sw = new Switch(EventVisibilityActivity.this);
            sw.setTag("switch");

            topRow.addView(tvTitle);
            topRow.addView(sw);

            // Date row
            TextView tvDate = new TextView(EventVisibilityActivity.this);
            tvDate.setTextSize(12f);
            tvDate.setTextColor(0xFF666666);
            tvDate.setTag("tvDate");
            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            dp.setMargins(0, 4, 0, 0);
            tvDate.setLayoutParams(dp);

            // Status chip
            TextView tvStatus = new TextView(EventVisibilityActivity.this);
            tvStatus.setTextSize(11f);
            tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            tvStatus.setPadding(12, 4, 12, 4);
            tvStatus.setTag("tvStatus");
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            sp.setMargins(0, 6, 0, 0);
            tvStatus.setLayoutParams(sp);

            row.addView(topRow);
            row.addView(tvDate);
            row.addView(tvStatus);

            // Divider
            View divider = new View(EventVisibilityActivity.this);
            divider.setBackgroundColor(0xFFEEEEEE);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));

            LinearLayout wrapper = new LinearLayout(EventVisibilityActivity.this);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            wrapper.addView(row);
            wrapper.addView(divider);

            return new VH(wrapper, tvTitle, tvDate, tvStatus, sw);
        }

        @Override
        public void onBindViewHolder(VH h, int position) {
            EventVisibilityItem item = list.get(position);

            h.tvTitle.setText(item.title);
            h.tvDate.setText("📅 " + item.date);

            // Visibility status chip
            if (item.hidden) {
                h.tvStatus.setText("🚫 Hidden from attendees");
                h.tvStatus.setTextColor(0xFFB71C1C);
                h.tvStatus.setBackgroundColor(0xFFFFEBEE);
            } else {
                h.tvStatus.setText("✅ Visible to attendees");
                h.tvStatus.setTextColor(0xFF2E7D32);
                h.tvStatus.setBackgroundColor(0xFFE8F5E9);
            }

            // Switch — checked = hidden (removed from attendee dashboard)
            h.sw.setOnCheckedChangeListener(null); // clear before setting
            h.sw.setChecked(item.hidden);
            h.sw.setText(item.hidden ? "Remove from Dashboard" : "Remove from Dashboard");
            h.sw.setOnCheckedChangeListener((btn, checked) -> {
                setVisibility(item, checked);
                // Update chip immediately
                if (checked) {
                    h.tvStatus.setText("🚫 Hidden from attendees");
                    h.tvStatus.setTextColor(0xFFB71C1C);
                    h.tvStatus.setBackgroundColor(0xFFFFEBEE);
                } else {
                    h.tvStatus.setText("✅ Visible to attendees");
                    h.tvStatus.setTextColor(0xFF2E7D32);
                    h.tvStatus.setBackgroundColor(0xFFE8F5E9);
                }
            });
        }

        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvDate, tvStatus;
            Switch   sw;
            VH(View root, TextView tvTitle, TextView tvDate,
               TextView tvStatus, Switch sw) {
                super(root);
                this.tvTitle = tvTitle;
                this.tvDate  = tvDate;
                this.tvStatus= tvStatus;
                this.sw      = sw;
            }
        }
    }
}