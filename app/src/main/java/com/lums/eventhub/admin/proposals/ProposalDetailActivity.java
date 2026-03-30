package com.lums.eventhub.admin.proposals;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lums.eventhub.R;
import com.lums.eventhub.model.Proposal;

public class ProposalDetailActivity extends AppCompatActivity {
    private TextView tvTitle, tvInfo, tvDesc;
    private FirebaseFirestore db;
    private String proposalId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proposal_detail);

        tvTitle = findViewById(R.id.tvDetailTitle);
        tvInfo = findViewById(R.id.tvDetailInfo); // New field for Date/Venue
        tvDesc = findViewById(R.id.tvDetailDesc);

        proposalId = getIntent().getStringExtra("id");
        db = FirebaseFirestore.getInstance();

        // Load the specific proposal details from Firebase
        db.collection("proposals").document(proposalId).get().addOnSuccessListener(doc -> {
            Proposal p = doc.toObject(Proposal.class);
            if (p != null) {
                tvTitle.setText(p.getTitle());
                tvInfo.setText("Date: " + p.getEventDate() + "\nVenue: " + p.getVenue());
                tvDesc.setText(p.getDescription());
            }
        });

        findViewById(R.id.btnApprove).setOnClickListener(v -> updateStatus("approved"));
        findViewById(R.id.btnReject).setOnClickListener(v -> updateStatus("rejected"));
    }

    private void updateStatus(String status) {
        db.collection("proposals").document(proposalId).update("status", status)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Proposal " + status, Toast.LENGTH_SHORT).show();
                    finish(); // Go back to the list
                });
    }
}