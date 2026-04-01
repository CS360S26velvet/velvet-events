package com.lums.eventhub;
/**
 * CheckInActivity.java
 * Role: Manages live event check-in for organizers (US-30)
 * Pattern: RecyclerView with Adapter pattern
 * Outstanding issues: None
 */
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CheckInActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    EditText etSearch;
    TextView tvCheckedIn, tvRemaining, tvTotalRegistered;
    AttendeeAdapter adapter;
    List<Attendee> attendeeList = new ArrayList<>();
    List<Attendee> filteredList = new ArrayList<>();
    int checkedInCount = 0;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in);

        db = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recyclerView);
        etSearch = findViewById(R.id.etSearch);
        tvCheckedIn = findViewById(R.id.tvCheckedIn);
        tvRemaining = findViewById(R.id.tvRemaining);
        tvTotalRegistered = findViewById(R.id.tvTotalRegistered);

        adapter = new AttendeeAdapter(filteredList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadAttendeesFromFirestore();

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }
            public void afterTextChanged(Editable s) {}
        });
    }

    void loadAttendeesFromFirestore() {
        db.collection("attendees")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    attendeeList.clear();
                    checkedInCount = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        String studentId = doc.getString("studentId");
                        String paymentStatus = doc.getString("paymentStatus");
                        Boolean isCheckedIn = doc.getBoolean("isCheckedIn");
                        if (name != null) {
                            Attendee a = new Attendee(
                                    doc.getId(), name, studentId, paymentStatus,
                                    isCheckedIn != null && isCheckedIn);
                            attendeeList.add(a);
                            if (a.isCheckedIn) checkedInCount++;
                        }
                    }
                    // If no data in Firestore yet, add sample data
                    if (attendeeList.isEmpty()) {
                        addSampleData();
                    } else {
                        filteredList.addAll(attendeeList);
                        updateStats();
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    addSampleData();
                });
    }

    void addSampleData() {
        attendeeList.add(new Attendee("", "Fatima Malik", "AT0023", "Paid", true));
        attendeeList.add(new Attendee("", "Hassan Raza", "AT0041", "Paid", false));
        attendeeList.add(new Attendee("", "Zainab Ali", "AT0055", "Paid", true));
        attendeeList.add(new Attendee("", "Bilal Khan", "AT0067", "Pending", false));
        attendeeList.add(new Attendee("", "Sara Ahmed", "AT0078", "Paid", false));
        attendeeList.add(new Attendee("", "Usman Tariq", "AT0089", "Paid", false));
        for (Attendee a : attendeeList) {
            if (a.isCheckedIn) checkedInCount++;
        }
        filteredList.addAll(attendeeList);
        updateStats();
        adapter.notifyDataSetChanged();
    }

    void filterList(String query) {
        filteredList.clear();
        for (Attendee a : attendeeList) {
            if (a.name.toLowerCase().contains(query.toLowerCase()) ||
                    a.studentId.toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(a);
            }
        }
        adapter.notifyDataSetChanged();
    }

    void updateStats() {
        tvTotalRegistered.setText(String.valueOf(attendeeList.size()));
        tvCheckedIn.setText(String.valueOf(checkedInCount));
        tvRemaining.setText(String.valueOf(attendeeList.size() - checkedInCount));
    }

    class Attendee {
        String id, name, studentId, paymentStatus;
        boolean isCheckedIn;
        Attendee(String id, String name, String studentId, String paymentStatus, boolean isCheckedIn) {
            this.id = id;
            this.name = name;
            this.studentId = studentId;
            this.paymentStatus = paymentStatus;
            this.isCheckedIn = isCheckedIn;
        }
    }

    class AttendeeAdapter extends RecyclerView.Adapter<AttendeeAdapter.ViewHolder> {
        List<Attendee> list;
        AttendeeAdapter(List<Attendee> list) { this.list = list; }

        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_attendee, parent, false);
            return new ViewHolder(v);
        }

        public void onBindViewHolder(ViewHolder holder, int position) {
            Attendee a = list.get(position);
            holder.tvName.setText(a.name);
            holder.tvStudentId.setText(a.studentId);
            holder.tvPaymentStatus.setText(a.paymentStatus.equals("Paid") ? "Paid ✓" : "Pending ⚠");
            holder.tvPaymentStatus.setTextColor(a.paymentStatus.equals("Paid") ?
                    0xFF4CAF50 : 0xFFFF9800);

            if (a.isCheckedIn) {
                holder.btnCheckIn.setText("Checked In ✓");
                holder.btnCheckIn.setEnabled(false);
                holder.btnCheckIn.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF9E9E9E));
            } else {
                holder.btnCheckIn.setText("Check In");
                holder.btnCheckIn.setEnabled(true);
                holder.btnCheckIn.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF1565C0));
                holder.btnCheckIn.setOnClickListener(v -> {
                    a.isCheckedIn = true;
                    checkedInCount++;
                    updateStats();
                    notifyItemChanged(position);
                    // Save to Firestore if has ID
                    if (!a.id.isEmpty()) {
                        Map<String, Object> update = new HashMap<>();
                        update.put("isCheckedIn", true);
                        db.collection("attendees").document(a.id).update(update);
                    }
                    Toast.makeText(CheckInActivity.this,
                            a.name + " checked in!", Toast.LENGTH_SHORT).show();
                });
            }
        }

        public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvStudentId, tvPaymentStatus;
            Button btnCheckIn;
            ViewHolder(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvName);
                tvStudentId = v.findViewById(R.id.tvStudentId);
                tvPaymentStatus = v.findViewById(R.id.tvPaymentStatus);
                btnCheckIn = v.findViewById(R.id.btnCheckIn);
            }
        }
    }
}