package com.lums.eventhub.admin.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lums.eventhub.R;
import com.lums.eventhub.admin.proposals.ProposalListActivity;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvPendingCount, tvApprovedCount, tvWelcome;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvApprovedCount = findViewById(R.id.tvApprovedCount);

        db = FirebaseFirestore.getInstance();

        // Load counts from Firebase
        db.collection("proposals").whereEqualTo("status", "pending").get()
                .addOnSuccessListener(q -> tvPendingCount.setText(String.valueOf(q.size())));

        db.collection("proposals").whereEqualTo("status", "approved").get()
                .addOnSuccessListener(q -> tvApprovedCount.setText(String.valueOf(q.size())));

        findViewById(R.id.btnViewProposals).setOnClickListener(v ->
                startActivity(new Intent(this, ProposalListActivity.class))
        );
    }
}