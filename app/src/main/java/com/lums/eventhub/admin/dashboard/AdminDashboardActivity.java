package com.lums.eventhub.admin.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lums.eventhub.R;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // 1. Proposals Button
        findViewById(R.id.btnViewProposals).setOnClickListener(v ->
                startActivity(new Intent(this, com.lums.eventhub.admin.proposals.ProposalListActivity.class)));

        // 2. Auditorium Button
        findViewById(R.id.btnAuditorium).setOnClickListener(v ->
                startActivity(new Intent(this, com.lums.eventhub.admin.auditorium.AuditoriumActivity.class)));

        // 3. Calendar Button
        findViewById(R.id.btnCalendar).setOnClickListener(v ->
                startActivity(new Intent(this, com.lums.eventhub.admin.calendar.CalendarActivity.class)));

        // 4. Accommodation Button
        findViewById(R.id.btnAccommodation).setOnClickListener(v ->
                startActivity(new Intent(this, com.lums.eventhub.admin.accommodation.AccommodationActivity.class)));

        // Firebase Stats Loader
        loadFirebaseStats();
    }

    private void loadFirebaseStats() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("proposals").whereEqualTo("status", "pending").get()
                .addOnSuccessListener(q -> ((TextView)findViewById(R.id.tvPendingCount)).setText("Pending: " + q.size()));
        db.collection("proposals").whereEqualTo("status", "approved").get()
                .addOnSuccessListener(q -> ((TextView)findViewById(R.id.tvApprovedCount)).setText("Approved: " + q.size()));
    }
}