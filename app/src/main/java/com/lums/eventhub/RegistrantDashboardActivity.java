package com.lums.eventhub;

/**
 * RegistrantDashboardActivity.java
 *
 * <p>Role: Displays all registrants for an event, highlighting incomplete registrations.
 * Allows organizers to export the registrant list to a CSV file.</p>
 *
 * <p>User Stories Covered:
 * - US-28: Show incomplete registrations (started but not submitted)
 * - US-29: Export registrant list to CSV/Excel</p>
 *
 * <p>Design Pattern: RecyclerView with Adapter pattern, Firestore data loading.</p>
 *
 * <p>Outstanding Issues: CSV export saves to Downloads folder, may need permission handling.</p>
 */

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RegistrantDashboardActivity extends AppCompatActivity {

    /** RecyclerView for displaying registrant list */
    RecyclerView recyclerViewRegistrants;

    /** Adapter for registrant items */
    RegistrantAdapter adapter;

    /** Full list of registrants */
    List<Registrant> registrantList = new ArrayList<>();

    /** Filtered list based on selected tab */
    List<Registrant> filteredList = new ArrayList<>();

    /** Stats TextViews */
    TextView tvTotalCount, tvIncompleteCount;

    /** Firestore instance */
    FirebaseFirestore db;

    /** Current event ID */
    String eventId = "SPADES2025";

    /** Currently showing all or incomplete only */
    boolean showingAll = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrant_dashboard);

        db = FirebaseFirestore.getInstance();

        recyclerViewRegistrants = findViewById(R.id.recyclerViewRegistrants);
        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvIncompleteCount = findViewById(R.id.tvIncompleteCount);
        Button btnExportCSV = findViewById(R.id.btnExportCSV);
        Button btnAll = findViewById(R.id.btnAll);
        Button btnIncomplete = findViewById(R.id.btnIncomplete);


        adapter = new RegistrantAdapter(filteredList);
        recyclerViewRegistrants.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRegistrants.setAdapter(adapter);

        loadRegistrants();

        btnAll.setOnClickListener(v -> showAll());
        btnIncomplete.setOnClickListener(v -> showIncomplete());
        btnExportCSV.setOnClickListener(v -> exportToCSV());
    }

    /**
     * Loads registrants from Firestore.
     * Falls back to sample data if Firestore is empty.
     */
    void loadRegistrants() {
        db.collection("events").document(eventId)
                .collection("registrants")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    registrantList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        String studentId = doc.getString("studentId");
                        String time = doc.getString("startedAt");
                        Boolean completed = doc.getBoolean("completed");
                        if (name != null) {
                            registrantList.add(new Registrant(
                                    name, studentId, time,
                                    completed != null && completed));
                        }
                    }
                    if (registrantList.isEmpty()) addSampleData();
                    else updateUI();
                })
                .addOnFailureListener(e -> addSampleData());
    }

    /**
     * Adds realistic sample registrant data for demonstration.
     */
    void addSampleData() {
        registrantList.add(new Registrant("Hassan Raza", "AT0041", "3 hours ago", false));
        registrantList.add(new Registrant("Fatima Malik", "AT0023", "2 hours ago", true));
        registrantList.add(new Registrant("Bilal Khan", "AT0067", "1 hour ago", false));
        registrantList.add(new Registrant("Sara Ahmed", "AT0078", "45 min ago", true));
        registrantList.add(new Registrant("Usman Tariq", "AT0089", "30 min ago", false));
        registrantList.add(new Registrant("Zainab Ali", "AT0055", "15 min ago", false));
        updateUI();
    }

    /**
     * Updates stats and refreshes the list display.
     */
    void updateUI() {
        int incomplete = 0;
        for (Registrant r : registrantList) {
            if (!r.completed) incomplete++;
        }
        tvTotalCount.setText(String.valueOf(registrantList.size()));
        tvIncompleteCount.setText(String.valueOf(incomplete));
        showAll();
    }

    /**
     * Shows all registrants in the list.
     */
    void showAll() {
        showingAll = true;
        filteredList.clear();
        filteredList.addAll(registrantList);
        adapter.notifyDataSetChanged();
    }

    /**
     * Filters the list to show only incomplete registrations (US-28).
     */
    void showIncomplete() {
        showingAll = false;
        filteredList.clear();
        for (Registrant r : registrantList) {
            if (!r.completed) filteredList.add(r);
        }
        adapter.notifyDataSetChanged();
    }

    /**
     * Exports the registrant list to a CSV file in the Downloads folder (US-29).
     * Each row contains name, student ID, start time, and completion status.
     */
    void exportToCSV() {
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            File csvFile = new File(downloadsDir, "registrants_" + eventId + ".csv");
            FileWriter writer = new FileWriter(csvFile);
            writer.append("Name,Student ID,Started At,Status\n");
            for (Registrant r : registrantList) {
                writer.append(r.name).append(",")
                        .append(r.studentId).append(",")
                        .append(r.startedAt).append(",")
                        .append(r.completed ? "Completed" : "Incomplete")
                        .append("\n");
            }
            writer.flush();
            writer.close();
            Toast.makeText(this,
                    "CSV exported to Downloads folder!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this,
                    "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Registrant model class representing a single event registrant.
     */
    static class Registrant {
        /** Full name of the registrant */
        String name;

        /** Student ID */
        String studentId;

        /** Time when registration was started */
        String startedAt;

        /** Whether the registration was completed */
        boolean completed;

        /**
         * @param name       Full name
         * @param studentId  Student ID
         * @param startedAt  Time started
         * @param completed  Completion status
         */
        Registrant(String name, String studentId, String startedAt, boolean completed) {
            this.name = name;
            this.studentId = studentId;
            this.startedAt = startedAt;
            this.completed = completed;
        }
    }

    /**
     * RecyclerView Adapter for displaying registrant items.
     */
    class RegistrantAdapter extends RecyclerView.Adapter<RegistrantAdapter.ViewHolder> {

        List<Registrant> list;

        /**
         * @param list List of registrants to display
         */
        RegistrantAdapter(List<Registrant> list) {
            this.list = list;
        }

        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_registrant, parent, false);
            return new ViewHolder(v);
        }

        /**
         * Binds registrant data to the view and sets status color.
         */
        public void onBindViewHolder(ViewHolder holder, int position) {
            Registrant r = list.get(position);
            holder.tvName.setText(r.name);
            holder.tvId.setText(r.studentId);
            holder.tvTime.setText("Started: " + r.startedAt);
            holder.tvStatus.setText(r.completed ? "Completed" : "Incomplete");
            holder.tvStatus.setBackgroundColor(
                    r.completed ? 0xFF4CAF50 : 0xFFF44336);
        }

        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvId, tvTime, tvStatus;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvRegistrantName);
                tvId = v.findViewById(R.id.tvRegistrantId);
                tvTime = v.findViewById(R.id.tvRegistrantTime);
                tvStatus = v.findViewById(R.id.tvStatus);
            }
        }
    }
}