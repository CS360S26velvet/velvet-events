package com.lums.eventhub.admin.accommodation;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import android.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.lums.eventhub.R;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class AccommodationActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView rvAccommodations;
    private AccomAdapter adapter;
    private List<Map<String, String>> requestList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accommodation);

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvAccommodations = findViewById(R.id.rvAccommodations);
        rvAccommodations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AccomAdapter(requestList);
        rvAccommodations.setAdapter(adapter);

        // Forward all button - confirmation dialog
        findViewById(R.id.btnForward).setOnClickListener(v -> showForwardDialog());

        // Download CSV button
        findViewById(R.id.btnDownload).setOnClickListener(v -> exportCSV());

        loadRequests();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRequests();
    }

    private void loadRequests() {
        db.collection("accommodation_requests").get()
                .addOnSuccessListener(query -> {
                    requestList.clear();
                    int pending = 0, approved = 0, rejected = 0;

                    for (QueryDocumentSnapshot doc : query) {
                        Map<String, String> req = new HashMap<>();
                        req.put("id",       doc.getId());
                        req.put("event",    doc.getString("eventTitle"));
                        req.put("society",  doc.getString("requesterUsername"));
                        req.put("guest",    doc.getString("guestName"));
                        req.put("role",     doc.getString("role"));
                        req.put("checkIn",  doc.get("checkIn") != null ? String.valueOf(doc.get("checkIn")) : "—");
                        req.put("checkOut", doc.get("checkOut") != null ? String.valueOf(doc.get("checkOut")) : "—");
                        req.put("rooms",    doc.get("rooms") != null ? String.valueOf(doc.get("rooms")) : "1");
                        req.put("status",   doc.getString("status"));
                        requestList.add(req);

                        String status = doc.getString("status");
                        if ("pending".equals(status))  pending++;
                        else if ("approved".equals(status)) approved++;
                        else if ("rejected".equals(status)) rejected++;
                    }

                    ((TextView) findViewById(R.id.tvStatPending)).setText(String.valueOf(pending));
                    ((TextView) findViewById(R.id.tvStatApproved)).setText(String.valueOf(approved));
                    ((TextView) findViewById(R.id.tvStatRejected)).setText(String.valueOf(rejected));

                    TextView tvEmpty = findViewById(R.id.tvEmpty);
                    if (requestList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvAccommodations.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvAccommodations.setVisibility(View.VISIBLE);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    private void updateStatus(String docId, String status) {
        db.collection("accommodation_requests").document(docId)
                .update("status", status)
                .addOnSuccessListener(a -> {
                    String msg = "approved".equals(status) ? "Request approved ✓" : "Request rejected";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    loadRequests();
                });
    }

    private void showForwardDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Forward to Residence Office")
                .setMessage("This will mark all approved requests as forwarded. Download CSV first?")
                .setPositiveButton("Forward All", (d, w) -> {
                    Toast.makeText(this, "Forwarded to Residence Office ✓", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Download CSV", (d, w) -> exportCSV())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void exportCSV() {
        try {
            StringBuilder csv = new StringBuilder();
            csv.append("Society,Event,Guest,Role,Check-In,Check-Out,Rooms,Status\n");

            for (Map<String, String> r : requestList) {
                String society = r.get("society") != null ? r.get("society") : "";
                if (society.contains("_")) society = society.substring(society.indexOf("_") + 1);
                csv.append(society).append(",")
                        .append(safe(r.get("event"))).append(",")
                        .append(safe(r.get("guest"))).append(",")
                        .append(safe(r.get("role"))).append(",")
                        .append(safe(r.get("checkIn"))).append(",")
                        .append(safe(r.get("checkOut"))).append(",")
                        .append(safe(r.get("rooms"))).append(",")
                        .append(safe(r.get("status"))).append("\n");
            }

            File file = new File(getExternalFilesDir(null), "accommodation_requests.csv");
            FileWriter writer = new FileWriter(file);
            writer.write(csv.toString());
            writer.close();

            Toast.makeText(this, "CSV saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String safe(String s) {
        return s != null ? s : "—";
    }

    // ── Inner Adapter ──
    private class AccomAdapter extends RecyclerView.Adapter<AccomAdapter.VH> {

        private List<Map<String, String>> data;

        AccomAdapter(List<Map<String, String>> data) { this.data = data; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_accommodation, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Map<String, String> r = data.get(position);

            h.tvEvent.setText(safe(r.get("event")));

            String society = safe(r.get("society"));
            if (society.contains("_")) society = society.substring(society.indexOf("_") + 1).toUpperCase();
            h.tvSociety.setText(society);

            h.tvGuest.setText(safe(r.get("guest")));
            h.tvRole.setText(safe(r.get("role")));
            h.tvRooms.setText(safe(r.get("rooms")) + " room(s)");
            h.tvCheckIn.setText(safe(r.get("checkIn")));
            h.tvCheckOut.setText(safe(r.get("checkOut")));

            String status = r.get("status") != null ? r.get("status") : "pending";
            h.tvStatus.setText(status.toUpperCase());
            switch (status) {
                case "pending":
                    h.tvStatus.setTextColor(Color.parseColor("#F39C12"));
                    break;
                case "approved":
                    h.tvStatus.setTextColor(Color.parseColor("#27AE60"));
                    // Hide buttons if already decided
                    h.btnApprove.setVisibility(View.GONE);
                    h.btnReject.setVisibility(View.GONE);
                    break;
                case "rejected":
                    h.tvStatus.setTextColor(Color.parseColor("#E74C3C"));
                    h.btnApprove.setVisibility(View.GONE);
                    h.btnReject.setVisibility(View.GONE);
                    break;
                default:
                    h.tvStatus.setTextColor(Color.parseColor("#9B7B86"));
            }

            String docId = r.get("id");
            h.btnApprove.setOnClickListener(v -> updateStatus(docId, "approved"));
            h.btnReject.setOnClickListener(v -> updateStatus(docId, "rejected"));
        }

        @Override
        public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvEvent, tvSociety, tvGuest, tvRole, tvRooms,
                    tvCheckIn, tvCheckOut, tvStatus;
            Button btnApprove, btnReject;

            VH(@NonNull View v) {
                super(v);
                tvEvent    = v.findViewById(R.id.tvAccomEvent);
                tvSociety  = v.findViewById(R.id.tvAccomSociety);
                tvGuest    = v.findViewById(R.id.tvAccomGuest);
                tvRole     = v.findViewById(R.id.tvAccomRole);
                tvRooms    = v.findViewById(R.id.tvAccomRooms);
                tvCheckIn  = v.findViewById(R.id.tvAccomCheckIn);
                tvCheckOut = v.findViewById(R.id.tvAccomCheckOut);
                tvStatus   = v.findViewById(R.id.tvAccomStatus);
                btnApprove = v.findViewById(R.id.btnAccomApprove);
                btnReject  = v.findViewById(R.id.btnAccomReject);
            }
        }
    }
}