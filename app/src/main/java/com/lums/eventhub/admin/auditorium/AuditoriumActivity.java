package com.lums.eventhub.admin.auditorium;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AuditoriumActivity.java
 *
 * Role: Admin screen for viewing all auditorium bookings requested
 * by organizers through their event proposals. Admin can confirm
 * venue availability for any pending proposal directly from this screen.
 *
 * Implements: Admin US-06
 */
public class AuditoriumActivity extends AppCompatActivity {

    private RecyclerView rvBookings;
    private FirebaseFirestore db;
    private List<Map<String, String>> bookingList = new ArrayList<>();
    private BookingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auditorium);

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvBookings = findViewById(R.id.rvBookings);
        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingAdapter(bookingList);
        rvBookings.setAdapter(adapter);

        loadBookings();
    }

    private void loadBookings() {
        // Fetch ALL proposals with a venue (both pending and approved)
        db.collection("proposals").get()
                .addOnSuccessListener(query -> {
                    bookingList.clear();
                    int confirmed = 0, pending = 0;

                    for (QueryDocumentSnapshot doc : query) {
                        String venue = doc.getString("venue");
                        if (venue == null || venue.isEmpty()) continue;

                        Map<String, String> booking = new HashMap<>();
                        booking.put("id", doc.getId());
                        booking.put("title", doc.getString("title"));
                        booking.put("venue", venue);
                        booking.put("date", doc.getString("eventDate"));
                        booking.put("organizer", doc.getString("organizerUsername"));
                        booking.put("status", doc.getString("status"));
                        bookingList.add(booking);

                        String status = doc.getString("status");
                        if ("approved".equals(status)) confirmed++;
                        else if ("pending".equals(status)) pending++;
                    }

                    // Update stats
                    ((TextView) findViewById(R.id.tvTotalBookings))
                            .setText(String.valueOf(bookingList.size()));
                    ((TextView) findViewById(R.id.tvConfirmedBookings))
                            .setText(String.valueOf(confirmed));
                    ((TextView) findViewById(R.id.tvPendingBookings))
                            .setText(String.valueOf(pending));

                    TextView tvEmpty = findViewById(R.id.tvEmpty);
                    if (bookingList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvBookings.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvBookings.setVisibility(View.VISIBLE);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    // ── Inner Adapter ──
    private class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.VH> {

        private List<Map<String, String>> data;

        BookingAdapter(List<Map<String, String>> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_booking, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Map<String, String> b = data.get(position);

            holder.tvTitle.setText(b.get("title") != null ? b.get("title") : "—");
            holder.tvVenue.setText(b.get("venue") != null ? b.get("venue") : "—");
            holder.tvDate.setText(b.get("date") != null ? b.get("date") : "—");

            String org = b.get("organizer") != null ? b.get("organizer") : "—";
            if (org.contains("_")) org = org.substring(org.indexOf("_") + 1).toUpperCase();
            holder.tvOrganizer.setText(org);

            // Availability badge based on status
            String status = b.get("status");
            if ("approved".equals(status)) {
                holder.tvAvailability.setText("CONFIRMED");
                holder.tvAvailability.setTextColor(0xFF27AE60);
            } else if ("pending".equals(status)) {
                holder.tvAvailability.setText("PENDING");
                holder.tvAvailability.setTextColor(0xFFF39C12);
            } else if ("rejected".equals(status)) {
                holder.tvAvailability.setText("REJECTED");
                holder.tvAvailability.setTextColor(0xFFE74C3C);
            }

            // Confirm availability → approve in Firestore
            String docId = b.get("id");
            holder.tvConfirmBtn.setOnClickListener(v -> {
                db.collection("proposals").document(docId)
                        .update("status", "approved")
                        .addOnSuccessListener(a -> {
                            Toast.makeText(AuditoriumActivity.this,
                                    "Availability confirmed ✓", Toast.LENGTH_SHORT).show();
                            loadBookings(); // Refresh
                        });
            });

            if ("approved".equals(status)) {
                holder.tvConfirmBtn.setVisibility(View.GONE);
            } else {
                holder.tvConfirmBtn.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvVenue, tvDate, tvOrganizer, tvAvailability, tvConfirmBtn;

            VH(@NonNull View itemView) {
                super(itemView);
                tvTitle        = itemView.findViewById(R.id.tvBookingTitle);
                tvVenue        = itemView.findViewById(R.id.tvBookingVenue);
                tvDate         = itemView.findViewById(R.id.tvBookingDate);
                tvOrganizer    = itemView.findViewById(R.id.tvBookingOrganizer);
                tvAvailability = itemView.findViewById(R.id.tvAvailability);
                tvConfirmBtn   = itemView.findViewById(R.id.tvConfirmBtn);
            }
        }
    }
}