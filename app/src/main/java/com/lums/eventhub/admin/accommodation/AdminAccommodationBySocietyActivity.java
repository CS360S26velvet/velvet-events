package com.lums.eventhub.admin.accommodation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.lums.eventhub.R;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AdminAccommodationBySocietyActivity.java
 *
 * Reads directly from the registrations collection so it works even for
 * attendees approved before the accommodationData write was added.
 *
 * Query: registrations where eventId == X
 *                          AND wantsAccommodation == "Yes"
 *                          AND paymentStatus == "Approved"
 */
public class AdminAccommodationBySocietyActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView rvSocieties;
    private SocietyAdapter adapter;
    private final List<SocietyItem> societyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_accommodation_by_society);

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvSocieties = findViewById(R.id.rvSocieties);
        rvSocieties.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SocietyAdapter(societyList);
        rvSocieties.setAdapter(adapter);

        loadSocietiesAndEvents();
    }

    private void loadSocietiesAndEvents() {
        db.collection("users")
                .whereEqualTo("role", "organizer")
                .get()
                .addOnSuccessListener(userSnap -> {
                    Map<String, String> orgToSociety = new HashMap<>();
                    for (QueryDocumentSnapshot doc : userSnap) {
                        String username = doc.getString("username");
                        String society  = doc.getString("societyName");
                        if (username != null) {
                            orgToSociety.put(username, society != null ? society : username);
                        }
                    }

                    db.collection("proposals")
                            .whereEqualTo("status", "Approved")
                            .get()
                            .addOnSuccessListener(propSnap -> {
                                Map<String, SocietyItem> societyMap = new HashMap<>();

                                for (QueryDocumentSnapshot doc : propSnap) {
                                    String orgUser    = doc.getString("organizerUsername");
                                    String eventTitle = doc.getString("title");
                                    String eventId    = doc.getId();
                                    if (orgUser == null) continue;

                                    String society = orgToSociety.containsKey(orgUser)
                                            ? orgToSociety.get(orgUser) : orgUser;

                                    if (!societyMap.containsKey(orgUser)) {
                                        societyMap.put(orgUser,
                                                new SocietyItem(orgUser, society, new ArrayList<>()));
                                    }
                                    societyMap.get(orgUser).events.add(
                                            new EventItem(eventId,
                                                    eventTitle != null ? eventTitle : eventId));
                                }

                                societyList.clear();
                                societyList.addAll(societyMap.values());
                                adapter.notifyDataSetChanged();

                                TextView tvEmpty = findViewById(R.id.tvEmpty);
                                if (tvEmpty != null) {
                                    tvEmpty.setVisibility(
                                            societyList.isEmpty() ? View.VISIBLE : View.GONE);
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Failed to load events: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed to load societies: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * Reads directly from registrations collection.
     * Works for ALL approved attendees regardless of when they were approved.
     * Filters: eventId matches AND wantsAccommodation == "Yes" AND paymentStatus == "Approved"
     */
    private void downloadAccommodationCSV(String societyName, String eventId, String eventTitle) {
        db.collection("registrations")
                .whereEqualTo("eventId", eventId)
                .whereEqualTo("answers.wantsAccommodation", "Yes")
                .whereEqualTo("paymentStatus", "Approved")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Toast.makeText(this,
                                "No approved accommodation attendees for this event.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        StringBuilder csv = new StringBuilder();
                        csv.append("Student Name,Student ID,Accommodation Fee,Event,Society\n");

                        for (QueryDocumentSnapshot doc : snap) {
                            csv.append(csvSafe(doc.getString("studentName"))).append(",")
                                    .append(csvSafe(doc.getString("studentId"))).append(",")
                                    .append(csvSafe(doc.getString("accommodationAmount"))).append(",")
                                    .append(csvSafe(eventTitle)).append(",")
                                    .append(csvSafe(societyName)).append("\n");
                        }

                        String safeTitle = eventTitle.replaceAll("[^a-zA-Z0-9]", "_");
                        File file = new File(getExternalFilesDir(null),
                                "accommodation_" + safeTitle + ".csv");
                        FileWriter fw = new FileWriter(file);
                        fw.write(csv.toString());
                        fw.close();

                        Toast.makeText(this,
                                "CSV saved: " + file.getAbsolutePath(),
                                Toast.LENGTH_LONG).show();

                    } catch (Exception e) {
                        Toast.makeText(this,
                                "Export failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private String csvSafe(String s) {
        if (s == null) return "";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    // ── Models ────────────────────────────────────────────────────────────────

    static class SocietyItem {
        String orgUsername, societyName;
        List<EventItem> events;
        SocietyItem(String orgUsername, String societyName, List<EventItem> events) {
            this.orgUsername = orgUsername;
            this.societyName = societyName;
            this.events      = events;
        }
    }

    static class EventItem {
        String id, title;
        EventItem(String id, String title) { this.id = id; this.title = title; }
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    class SocietyAdapter extends RecyclerView.Adapter<SocietyAdapter.VH> {

        private final List<SocietyItem> data;
        SocietyAdapter(List<SocietyItem> data) { this.data = data; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_society_accommodation, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            SocietyItem society = data.get(position);
            h.tvSocietyName.setText(society.societyName);

            h.llEvents.removeAllViews();
            for (EventItem event : society.events) {
                View row = LayoutInflater.from(AdminAccommodationBySocietyActivity.this)
                        .inflate(R.layout.item_society_event_row, h.llEvents, false);

                ((TextView) row.findViewById(R.id.tvEventTitle)).setText(event.title);
                row.findViewById(R.id.btnDownloadAccommodation)
                        .setOnClickListener(v ->
                                downloadAccommodationCSV(
                                        society.societyName, event.id, event.title));

                h.llEvents.addView(row);
            }

            h.tvSocietyName.setOnClickListener(v -> {
                boolean visible = h.llEvents.getVisibility() == View.VISIBLE;
                h.llEvents.setVisibility(visible ? View.GONE : View.VISIBLE);
            });
        }

        @Override
        public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvSocietyName;
            android.widget.LinearLayout llEvents;

            VH(@NonNull View v) {
                super(v);
                tvSocietyName = v.findViewById(R.id.tvSocietyName);
                llEvents      = v.findViewById(R.id.llEvents);
            }
        }
    }
}