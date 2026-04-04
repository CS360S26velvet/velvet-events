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

    private TextView tvTitle, tvDetailDate, tvDetailVenue, tvDetailOrganizer, tvDetailDesc, tvDetailInfo;
    private FirebaseFirestore db;
    private String proposalId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proposal_detail);

        tvTitle          = findViewById(R.id.tvDetailTitle);
        tvDetailDate     = findViewById(R.id.tvDetailDate);
        tvDetailVenue    = findViewById(R.id.tvDetailVenue);
        tvDetailOrganizer = findViewById(R.id.tvDetailOrganizer);
        tvDetailDesc     = findViewById(R.id.tvDetailDesc);
        tvDetailInfo     = findViewById(R.id.tvDetailInfo);

        proposalId = getIntent().getStringExtra("proposalId");
        db = FirebaseFirestore.getInstance();

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Load proposal from Firestore
        db.collection("proposals").document(proposalId).get()
                .addOnSuccessListener(doc -> {
                    Proposal p = doc.toObject(Proposal.class);
                    if (p != null) {
                        tvTitle.setText(p.getTitle());
                        tvDetailDate.setText(p.getEventDate() != null ? p.getEventDate() : "—");
                        tvDetailVenue.setText(p.getVenue() != null ? p.getVenue() : "—");
                        tvDetailOrganizer.setText(p.getOrganizerUsername() != null ? p.getOrganizerUsername() : "—");
                        tvDetailDesc.setText(p.getDescription() != null ? p.getDescription() : "No description provided.");
                        tvDetailInfo.setText("Current Status: " + (p.getStatus() != null ? p.getStatus().toUpperCase() : "UNKNOWN"));
                    }
                });

        // Decision buttons
        findViewById(R.id.btnApprove).setOnClickListener(v -> updateStatus("approved"));
        findViewById(R.id.btnRevision).setOnClickListener(v -> updateStatus("revision"));
        findViewById(R.id.btnReject).setOnClickListener(v -> updateStatus("rejected"));
    }

    private void updateStatus(String status) {
        db.collection("proposals").document(proposalId)
                .update("status", status)
                .addOnSuccessListener(a -> {
                    String msg = status.equals("approved") ? "Proposal approved ✓"
                            : status.equals("rejected") ? "Proposal rejected"
                            : "Revision requested";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}